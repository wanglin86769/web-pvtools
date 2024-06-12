<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					Probe
				</div>

				<div style="margin-bottom: 1em;">
					<span>
						<label style="font-size: 1.2em; font-weight: bold; margin-right: .5em;">Web Socket Status</label>
						<i v-if="serviceAvailable===true" class="fa fa-check-circle" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
						<i v-else class="fa fa-times-circle" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
					</span>
					<span style="float: right">
						<label style="font-size: 1.2em; font-weight: bold; margin-right: .5em;">Web Service Status</label>
						<i v-if="serviceAvailable===true" class="fa fa-check-circle" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
						<i v-else class="fa fa-times-circle" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
					</span>
				</div>

				<Toolbar class="mb-0">
					<template v-slot:start>
						<div v-if="writable" style="color: RGB(104,159,56); cursor: pointer;" @click="toggleWrite()">
							<span style="font-size: 1.2em; font-weight: bold; margin-right: 0.5em;">Write</span>
							<i :class="showWrite ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o'" v-tooltip.top="showWrite ? 'Hide write' : 'Show write'" style="font-size: 1.5rem;" ></i>
						</div>
					</template>

					<template v-slot:end>
						<Dropdown style="margin-right: 2em" v-model="prefix" :options="pvTypeOptions" optionLabel="name" optionValue="value"></Dropdown>
						<div class="p-inputgroup">
							<Button label="Clear" icon="pi pi-refresh" severity="warning" @click="clearData()" />
							<InputText placeholder="PV name" style="width: 20em" v-model="inputPVName" v-on:keyup.enter="getPV(inputPVName)" autofocus />
							<Button label="Start" icon="pi pi-play-circle" severity="success" style="width: 80px" @click="startPV(inputPVName)" />
							<Button label="Stop" icon="pi pi-stop-circle" severity="danger" style="width: 80px" @click="stopPV(inputPVName)" />
						</div>
					</template>
				</Toolbar>

				<Toolbar v-if="showWrite" class="mb-0">
					<template v-slot:end>
						<div class="p-inputgroup">
							<InputText placeholder="PV value" style="width: 30em" v-model="inputPVValue" v-on:keyup.enter="putPV(inputPVName, inputPVValue)" />
							<Button label="Write" icon="pi pi-play-circle" severity="success" style="width: 80px" @click="putPV(inputPVName, inputPVValue)" />
						</div>
					</template>
				</Toolbar>

				<table width="100%" style="border-collapse: collapse; margin-bottom: 4em; margin-top: 4em;">
					<tr height="40em">
						<th colspan="4" align="left" style="text-align: center;">
							<span style="margin-right: 1em">
								<span v-if="!pvData.pv_name" style="font-weight: bold; font-size: 1.5em;">PV name</span>
								<span v-else style="font-weight: bold; font-size: 1.5em;">{{ unprefixPV(pvData.pv_name) }}</span>
							</span>
							<span>
								<i v-if="pvData.connected===true" class="fa fa-check" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
								<i v-if="pvData.connected===false" class="fa fa-times" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
							</span>
							<i v-if="loading" style="margin-left: 0.5em; color: RGB(29,149,243);" class="fa fa-spinner fa-spin fa-fw fa-2x"></i>
							<Button label="Info" icon="pi pi-info-circle" severity="info" style="float: left" @click="getPVInfo(pvData.pv_name)" />
						</th>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Value</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.value }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Alarm</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.alarm }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Timestamp</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.timestamp }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">PV type</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.pv_type }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Value type</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.vtype }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Size</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.size }}</span></td>
					</tr>
				</table>
			</div>
		</div>

		<Dialog v-model:visible="pvInfoDialogDisplay" header="PV Info" :modal="true" style="min-width: 60%">
			<div>
				<table width="100%" style="border-collapse: collapse; margin-bottom: 4em;">
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">PV name</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.pv_name }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">PV type</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.pv_type }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Value type</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.vtype }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Size</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.size }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Units</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.units }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Description</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.description }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Precision</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.precision }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Min</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.min }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Max</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.max }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Warning Low</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.warn_low }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Warning high</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.warn_high }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Alarm low</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.alarm_low }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Alarm high</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvInfo.alarm_high }}</span></td>
					</tr>
				</table>
			</div>
			<template #footer>
				<Button label="Close" icon="pi pi-times" class="p-button-text" @click="pvInfoDialogDisplay = false"/>
			</template>
		</Dialog>
	</div>

</template>

<script>
import moment from 'moment';
import config from '@/config/configuration.js';
import WebService from '@/service/WebService';
import Utility from '@/service/Utility';

export default {
	data() {
		return {
			ws: null,
			connected: false,
			pvData: {},
			pvInfo: {},
			pvWrite: {},
			serviceAvailable: false,
			inputPVName: '',
			inputPVValue: '',
			prefix: Utility.defaultPrefix,
			pvTypeOptions: Utility.pvTypeOptions,
			loading: false,
			showWrite: false,
			pvInfoDialogDisplay: false,
			writable: config.writable,
			intervalId: null,
		}
	},
	webService: null,
	created() {
		this.connect();
		this.webService = new WebService();
		this.checkServiceStatus();
		this.intervalId = setInterval(this.checkServiceStatus, 30000);  // Every 30 seconds
	},
	beforeUnmount(){
		this.ws.close();
		if(this.intervalId) {
			clearInterval(this.intervalId);
			this.intervalId = null;
		}
	},
	methods: {
		toggleWrite() {
			this.showWrite = !this.showWrite;
		},
		prefixPV(pvname, prefix) {
			return Utility.prefixPV(pvname, prefix);
		},
		unprefixPV(pvname) {
			return Utility.unprefixPV(pvname);
		},
		connect() {
			this.connected = false;
			this.ws = new WebSocket(config.webSocketPath);

			this.ws.onopen = (event) => {
				console.log("WebSocket connection opened:", event);
				this.connected = true;
			};

			this.ws.onclose = (event) => {
				console.log('WebSocket connection is closed. Reconnect will be attempted in 5 second.', event.reason);
				this.connected = false;
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
					if(this.pvData && this.pvData.pv_name === message.pv) {
						this.pvData.pv_name = message.pv;
						this.pvData.connected = message.connected;
						this.pvData.pv_type = message.pv_type;
						if(message.vtype) {
							this.pvData.vtype = message.vtype;
						}
						this.pvData.size = message.size;
						this.pvData.timestamp = this.showDateTimeMS(message.seconds * 1000 + message.nanos / 1000000);
						if(message.alarm_name || message.alarm_severity) {
							this.pvData.alarm = `${message.alarm_name}    ${message.severity}`;
						}
						this.pvData.value = message.pv_type === 'VString' ? message.text : message.value;
					}
				}
			};
		},
		async checkServiceStatus() {
            this.serviceAvailable = await this.webService.isServiceAvailable();
        },
		processValueInput(input) {
			if(!input) return null;
			// Scalar
			if(!input.includes(',') && !input.includes(' ')) {
				return input;
			}
			// Array
			if(input.includes(',')) {
				input = input.replace(/,/g, ' ');
			}
			let array = input.trim().split(/\s+/);
			return array;
		},
		async getPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			this.loading = true;
            try {
				this.pvData = {};
				let pv = await this.webService.pvGet(this.prefixPV(pvname, this.prefix));
				if(pv && pv.success === false) {
					this.pvData.pv_name = pvname;
					this.pvData.connected = false;
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: pv.message });
					return;
				}
				this.pvData.pv_name = pv.name;
				this.pvData.connected = true;
				this.pvData.pv_type = pv.pv_type;
				this.pvData.vtype = pv.vtype;
				this.pvData.size = pv.size;
				this.pvData.timestamp = this.showDateTimeMS(pv.seconds * 1000 + pv.nanos / 1000000);
				this.pvData.alarm = `${pv.alarm_name}    ${pv.alarm_severity}`;
				this.pvData.value = pv.pv_type === 'VString' ? pv.text : pv.value;
			} catch(error) {
				this.pvData.pv_name = pvname;
				this.pvData.connected = false;
				if(error.response) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: error.response.data });
				} else {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: error.message });
				}
			} finally {
				this.loading = false;
			}
		},
		async putPV(pvname, pvvalue) {
			if(!pvname || !pvvalue) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name or PV value is empty', life: 5000 });
				return;
			}
			this.loading = true;
            try {
				this.pvWrite = {};
				let pv = await this.webService.pvPut(this.prefixPV(pvname, this.prefix), this.processValueInput(pvvalue));
				if(pv && pv.success === false) {
					this.$toast.add({ severity: 'error', summary: 'Failed to put PV value', detail: pv.message });
					return;
				}
				this.pvWrite.pv_name = pv.pv;
				this.pvWrite.pv_type = pv.pv_type;
				this.pvWrite.vtype = pv.vtype;
				this.pvWrite.size = pv.size;
				this.pvWrite.old_value = pv.old_value;
				this.pvWrite.new_value = pv.new_value;
				this.$toast.add({severity: 'success', summary: 'Operation succeeds', detail: `PV name: ${this.pvWrite.pv_name}\nOld value: ${this.pvWrite.old_value}\nNew value: ${this.pvWrite.new_value}`, life: 5000});
			} catch(error) {
				if(error.response) {
					this.$toast.add({ severity: 'error', summary: 'Failed to put PV value', detail: error.response.data });
				} else {
					this.$toast.add({ severity: 'error', summary: 'Failed to put PV value', detail: error.message });
				}
			} finally {
				this.loading = false;
			}
		},
		async getPVInfo(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			this.loading = true;
            try {
				this.pvInfo = {};
				let pv = await this.webService.pvInfo(this.prefixPV(pvname, this.prefix));
				if(pv && pv.success === false) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV info', detail: pv.message });
					return;
				}
				this.pvInfo.pv_name = pv.name;
				this.pvInfo.pv_type = pv.pv_type;
				this.pvInfo.vtype = pv.vtype;
				this.pvInfo.size = pv.size;
				this.pvInfo.units = pv.units;
				this.pvInfo.description = pv.description;
				this.pvInfo.precision = pv.precision;
				this.pvInfo.min = pv.min;
				this.pvInfo.max = pv.max;
				this.pvInfo.warn_low = pv.warn_low;
				this.pvInfo.warn_high = pv.warn_high;
				this.pvInfo.alarm_low = pv.alarm_low;
				this.pvInfo.alarm_high = pv.alarm_high;
			} catch(error) {
				if(error.response) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV info', detail: error.response.data });
				} else {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV info', detail: error.message });
				}
			} finally {
				this.loading = false;
			}

			this.pvInfoDialogDisplay = true;
		},
		startPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.sendMessage({ "type": "subscribe", "pvs": [ pvname ] });
		},
		stopPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.sendMessage({ "type": "clear", "pvs": [ pvname ] });
		},
		clearData() {
			this.pvData = {};
		},
		showDateTimeMS(value) {
			return moment(value).format("YYYY-MM-DD HH:mm:ss.SSS");
		},
		sendMessage(message) {
			if(!this.ws) return;
			this.ws.send(JSON.stringify(message));
		},
	},
	computed: {
		
	}

}
</script>

<style scoped>

:deep(.p-datatable) thead th {
    color: RGB(29,149,243);
}

table, th, td {
  border: 2px solid RGB(233,235,238);
}
	
</style>
