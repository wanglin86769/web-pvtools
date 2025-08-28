<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					PV Monitor
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
							<Button label="Clear" icon="pi pi-refresh" severity="warning" @click="clearData()" />
							<InputText placeholder="PV name" style="width: 20em" v-model="inputPVName" v-on:keyup.enter="startPV(inputPVName)" />
							<Button label="Start" icon="pi pi-play-circle" severity="success" @click="startPV(inputPVName)" />
							<Button label="Stop" icon="pi pi-stop-circle" severity="danger" @click="stopPV(inputPVName)" />
						</div>
					</template>
				</Toolbar>

				<div style="border: 2px solid RGB(233,235,238); border-top: none; height: 400px; overflow-y: auto; white-space: pre-wrap;" ref="container">
					<div v-for="(item, index) of monitorItems" :key="index" style="margin-left: 20px; margin-bottom: 5px; margin-top: 5px;">
						{{ item }}
					</div>
				</div>
			</div>
		</div>
	</div>

</template>

<script>
import moment from 'moment';
import config from '@/config/configuration.js';
import Utility from '@/service/Utility';

export default {
	data() {
		return {
			monitorItems: [],
			ws: null,
			connected: false,
			inputPVName: '',
			pv: {},
			prefix: Utility.defaultPrefix,
			pvTypeOptions: Utility.pvTypeOptions,
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
					if(this.pv.pv === message.pv) {
						for(const [key, value] of Object.entries(message)) {
							this.pv[key] = value;
						}
						let str = this.extractData(this.pv);
						this.monitorItems.push(str);
						this.scrollToBottom();
					}
				}
			};
		},
		scrollToBottom() {
			this.$nextTick(() => {
				let container = this.$refs.container;
				container.scrollTop = container.scrollHeight + 120;
			});
		},
		extractData(pv) {
			let name = this.unprefixPV(pv.pv);
			let connected = pv.connected;
			let vtype = pv.vtype;
			let value = vtype === 'VString' ? pv.text : pv.value;
			let timestamp = this.showDateTimeMS(pv.seconds * 1000 + pv.nanos / 1000000);
			let alarm_name = pv.alarm_name;
			let alarm_severity = pv.severity;
			let str = connected ? `${name}                    ${timestamp}    ${value}    ${alarm_name}    ${alarm_severity}` : `${name}                    *** disconnected`;
			return str;
		},
		prefixPV(pvname, prefix) {
			return Utility.prefixPV(pvname, prefix);
		},
		unprefixPV(pvname) {
			return Utility.unprefixPV(pvname);
		},
		startPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			if(this.pv.pv !== pvname) {
				this.pv = {};
				this.clearData();
			}
			this.pv.pv = pvname;
			this.sendMessage({ "type": "subscribe", "pvs": [ pvname ] });
		},
		stopPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.sendMessage({ "type": "clear", "pvs": [pvname] });
		},
		clearData() {
			this.monitorItems = [];
		},
		sendMessage(message) {
			if(!this.ws) return;
			this.ws.send(JSON.stringify(message));
		},
		showDateTimeMS(value) {
			return moment(value).format("YYYY-MM-DD HH:mm:ss.SSS");
		},
	},
	computed: {
		
	}

}
</script>

<style scoped>
	
</style>
