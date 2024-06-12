<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					PV Status Monitor
				</div>

				<Toolbar class="mb-0">
					<template v-slot:start>
						<label style="font-size: 1.2em; font-weight: bold; margin-right: .5em;">WebSocket Status</label>
						<i v-if="connected===true" class="fa fa-check-circle" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
						<i v-else class="fa fa-times-circle" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
					</template>

					<template v-slot:end>
						<Dropdown style="margin-right: 2em" v-model="prefix" :options="pvTypeOptions" optionLabel="name" optionValue="value"></Dropdown>
						<div class="p-inputgroup">
							<InputText placeholder="PV name" style="width: 20em" v-model="inputPVName" v-on:keyup.enter="addPV(inputPVName)" />
							<SplitButton label="Add" icon="pi pi-plus" @click="addPV(inputPVName)" :model="items" />
						</div>
					</template>
				</Toolbar>
				
				<DataTable :value="pvStatusList" :paginator="true" :rows="50" :rowHover="true" showGridlines 
							paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown" :rowsPerPageOptions="[10,25,50,100,200]"
							currentPageReportTemplate="Showing {first} to {last} of {totalRecords} pvs" 
							paginatorPosition="both" responsiveLayout="stack" v-model:first="currentPageFirstIndex">
					
					<Column field="index" header="Index">
						<template #body="slotProps">
							{{currentPageFirstIndex + slotProps.index + 1}}
						</template>
					</Column>
					<Column>
						<template #header>
							<span style="margin-right: 1em">PV Name</span>
							<InputSwitch v-tooltip.top="'Show PV prefix'" v-model="showPrefix" />
						</template>
						<template #body="slotProps">
							<div v-if="showPrefix" style="font-weight: bold">{{slotProps.data.pv}}</div>
							<div v-else style="font-weight: bold">{{unprefixPV(slotProps.data.pv)}}</div>
						</template>
					</Column>
					<Column header="Connected">
						<template #body="slotProps">
							<div>
								<span v-if="slotProps.data.connected===true" style="color: green">true</span>
								<span v-else style="color: red">false</span>
							</div>
						</template>
					</Column>
					<Column header="PV Type">
						<template #body="slotProps">
							<div>{{slotProps.data.pv_type}}</div>
						</template>
					</Column>
					<Column header="Value Type">
						<template #body="slotProps">
							<div v-if="slotProps.data.connected">{{slotProps.data.vtype}}</div>
						</template>
					</Column>
					<Column header="Size">
						<template #body="slotProps">
							<div v-if="slotProps.data.connected">{{slotProps.data.size}}</div>
						</template>
					</Column>
					<Column header="Value">
						<template #body="slotProps">
							<div v-if="slotProps.data.connected" style="color: green; font-weight: bold;">{{slotProps.data.value}}</div>
						</template>
					</Column>
					<Column header="Updated">
						<template #body="slotProps">
							<div v-if="slotProps.data.connected">{{showDateTimeMS(slotProps.data.seconds * 1000 + slotProps.data.nanos / 1000000)}}</div>
						</template>
					</Column>
					<Column headerStyle="width: 8em">
						<template #header>
							<i class="pi pi-cog" style="font-size: 1.5rem" v-tooltip.top="'Operations'"></i>
						</template>
						<template #body="slotProps">
							<i class="fa fa-times" v-tooltip.top="'Remove'" style="font-size: 1.5em; cursor: pointer; color: red;" @click="removePV(slotProps.data.pv)"></i>
						</template>
					</Column>
					<template #empty>
						<div style="color: orange">No records found.</div>
					</template>
				</DataTable>
			</div>
		</div>

		<Dialog v-model:visible="addMultiplePVsDialogDisplay" modal header="Add Multiple PVs" class="p-fluid" style="width: 50%">
			<Textarea v-model="inputPVNames" style="width: 100%" rows="15" spellcheck="false" placeholder="Please enter PV names line by line." autofocus />

			<template #footer>
				<Button type="button" icon="pi pi-times" label="Cancel" class="p-button-text" @click="addMultiplePVsDialogDisplay = false"></Button>
				<Button type="button" icon="pi pi-check" label="OK" severity="primary" @click="addMultiplePVs(inputPVNames)"></Button>
			</template>
		</Dialog>

		<Dialog v-model:visible="removeAllPVsDialogDisplay" header="Message" :modal="true" style="min-width: 40%">
			<div>
				<i class="pi pi-exclamation-triangle mr-3" style="font-size: 2em; color: RGB(211,47,47); vertical-align: middle;" />
				<span style="color: RGB(211,47,47);">Are you sure you want to remove all the PVs?</span>
				<div v-for="(item, index) of pvStatusList" :key="index" style="margin-left: 20px; margin-bottom: 5px; margin-top: 10px;">
					<span style="margin-right: 4em; color: RGB(29,149,243);">{{ index + 1 }}</span><span>{{ item.pv }}</span>
				</div>
			</div>
			<template #footer>
				<Button label="Cancel" icon="pi pi-times" class="p-button-text" @click="removeAllPVsDialogDisplay = false"/>
				<Button label="OK" icon="pi pi-check" class="p-button-primary" @click="removeAllPVs" />
			</template>
		</Dialog>

	</div>

</template>

<script>
import Utility from '@/service/Utility';
import moment from 'moment';
import config from '@/config/configuration.js';

export default {
	data() {
		return {
			ws: null,
			connected: false,
			pvStatusList: [],
			inputPVName: '',
			inputPVNames: '',
			prefix: Utility.defaultPrefix,
			pvTypeOptions: Utility.pvTypeOptions,
			showPrefix: false,
			items: [
                {
                    label: 'Add Multiple',
                    command: () => {
						this.inputPVNames = '';
                        this.addMultiplePVsDialogDisplay = true;
                    }
                },
                {
                    label: 'Remove All',
                    command: () => {
                        this.onRemoveAllPVsClick();
                    }
                },
			],

			addMultiplePVsDialogDisplay: false,
			removeAllPVsDialogDisplay: false,
			currentPageFirstIndex: 0,
		}
	},
	created() {
		this.connect();
	},
	beforeUnmount(){
		this.ws.close();
	},
	methods: {
		connect() {
			this.connected = false;
			this.ws = new WebSocket(config.webSocketPath);

			this.ws.onopen = (event) => {
				console.log("WebSocket connection opened:", event);
				this.connected = true;
				this.startAllPVs();
			};

			this.ws.onclose = (event) => {
				console.log('WebSocket connection is closed. Reconnect will be attempted in 5 second.', event.reason);
				this.connected = false;
				this.resetAllPVs();
				setTimeout(() => {
					this.connect();
				}, 5000);
			};

			this.ws.onerror = (error) => {
				console.error('WebSocket encountered error: ', error.message, 'Closing socket');
    			this.ws.close();
			};

			this.ws.onmessage = (event) => {
				// console.log("WebSocket message received:", event.data);
				let message = JSON.parse(event.data);
				if(message.type === 'update') {
					let pv = this.findPV(message.pv);
					if(pv) {
						for(const [key, value] of Object.entries(message)) {
							pv[key] = value;
						}
					}
				}
			};
		},
		prefixPV(pvname, prefix) {
			return Utility.prefixPV(pvname, prefix);
		},
		unprefixPV(pvname) {
			return Utility.unprefixPV(pvname);
		},
		findPV(pvname) {
			return this.pvStatusList.find(element => element.pv === pvname);
		},
		addPV(pvname) {
			if(!this.connected) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'WebSocket is disconnected', life: 5000 });
				return;
			}
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			if(!this.findPV(pvname)) {
				this.pvStatusList.push({ pv: pvname, connected: false });
				this.sendMessage({ "type": "subscribe", "pvs": [ pvname ] });
			} else {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: `The specified PV already exists\n${pvname}`, life: 5000 });
			}
			this.inputPVName = '';
			localStorage.setItem(config.localStoragePVList, JSON.stringify(this.pvStatusList.map(element => element.pv)));
		},
		addMultiplePVs(pvInput) {
			if(!this.connected) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'WebSocket is disconnected', life: 5000 });
				return;
			}
			if(!pvInput) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV input is empty', life: 5000 });
				return;
			}
			// Split PV rename by line breaks
			let pvList = pvInput.split(/\r?\n/);
			// Remove whitespace from both sides of the string
			pvList = pvList.map(element => element.trim());
			// Remove empty strings
			pvList = pvList.filter((element) => element);
			// Prefix PV name with ca:// or pva://
			pvList = pvList.map(element => this.prefixPV(element, this.prefix));

			for(let i = pvList.length - 1; i >= 0; i--) {
				let pvname = pvList[i];
				if(this.findPV(pvname)) {
					this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: `The specified PV already exists\n${pvname}`, life: 5000 });
					pvList.splice(i, 1);
				} else {
					this.pvStatusList.push({ pv: pvname, connected: false });
				}
			}
			this.sendMessage({ "type": "subscribe", "pvs": pvList });
			this.addMultiplePVsDialogDisplay = false;
			localStorage.setItem(config.localStoragePVList, JSON.stringify(this.pvStatusList.map(element => element.pv)));
		},
		startAllPVs() {
			let data = JSON.parse(localStorage.getItem(config.localStoragePVList));
			if(data && data.length) {
				if(!this.pvStatusList || !this.pvStatusList.length) {
					this.pvStatusList = [];
					for(let item of data) {
						this.pvStatusList.push({ pv: item, connected: false });
					}
				}
			}
			if(this.pvStatusList && this.pvStatusList.length) {
				this.sendMessage({ "type": "subscribe", "pvs": this.pvStatusList.map(element => element.pv) });
			}
		},
		resetAllPVs() {
			if(this.pvStatusList && this.pvStatusList.length) {
				for(let pv of this.pvStatusList) {
					pv.connected = false;
				}
			}
		},
		onRemoveAllPVsClick() {
			if(!this.pvStatusList || !this.pvStatusList.length) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'No PVs found, no need to remove.', life: 5000 });
				return;
			}
			this.removeAllPVsDialogDisplay = true;
		},
		removePV(pvname) {
			for(let i = this.pvStatusList.length - 1; i >= 0; i--) {
				if(this.pvStatusList[i].pv == pvname) {
					this.pvStatusList.splice(i, 1);
					break;
				}
			}
			if(!this.pvStatusList || !this.pvStatusList.length) {
				localStorage.removeItem(config.localStoragePVList);
			} else {
				localStorage.setItem(config.localStoragePVList, JSON.stringify(this.pvStatusList.map(element => element.pv)));
			}
			this.sendMessage({ "type": "clear", "pvs": [pvname] });
			this.$toast.add({severity: 'success', summary: 'Operation succeeds', detail: `The PV has been removed\n${pvname}`, life: 5000});
		},
		removeAllPVs() {
			if(this.pvStatusList && this.pvStatusList.length) {
				this.sendMessage({ "type": "clear", "pvs": this.pvStatusList.map(element => element.pv) });
				this.pvStatusList = [];
			}
			this.removeAllPVsDialogDisplay = false;
			localStorage.removeItem(config.localStoragePVList);
			this.$toast.add({severity: 'success', summary: 'Operation succeeds', detail: 'All PVs have been removed', life: 5000});
		},
		sendMessage(message) {
			if(!this.ws) return;
			this.ws.send(JSON.stringify(message));
		},
		showDateTimeMS(value) {
			return moment(value).format("YYYY-MM-DD HH:mm:ss.SSS");
		},
	},
}
</script>

<style scoped>

:deep(.p-datatable) thead th {
    color: RGB(29,149,243);
}

table, th, td {
  border: 2px solid RGBA(29,149,243,0.4);
}
	
</style>
