<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					StripTool
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

				<div id="chart" style="margin-top: 2em;">
					<apexchart :key="componentKey" type="line" height="400" style="border: 2px solid RGB(233,235,238)" :options="chartOptions" :series="series"></apexchart>
				</div>
			</div>
		</div>
	</div>

</template>

<script>
import Utility from '@/service/Utility';
import moment from 'moment';
import config from '@/config/configuration.js';
const XAXIS_STEP_SIZE = 2 * 60 * 1000; // 2 minutes

export default {
	data() {
		return {
			intervalId: null,
			ws: null,
			connected: false,
			inputPVName: '',
			prefix: Utility.defaultPrefix,
			pvTypeOptions: Utility.pvTypeOptions,
			items: [
				{
                    label: 'Start',
                    command: () => {
						this.startPV(this.inputPVName);
                    }
                },
				{
                    label: 'Stop',
                    command: () => {
						this.stopPV(this.inputPVName);
                    }
                },
				{
                    label: 'Remove',
                    command: () => {
						this.removePV(this.inputPVName);
                    }
                },
                {
                    label: 'Remove All',
                    command: () => {
                        this.removeAllPVs();
                    }
                },
			],

			componentKey: 0,
			series: [],

			// series: [
			// 	{
			// 		name: "Desktops1",
			// 		data: [10, 41, 35, 51, 49, 62, 69, 91, 148]
			// 	},
			// 	{
			// 		name: "Desktops2",
			// 		data: [20, 51, 35, 51, 59, 62, 79, 91, 158, 90]
			// 	},
			// ],

			chartOptions: {
				chart: {
					type: 'line',
					zoom: {
						enabled: true
					}
				},
				dataLabels: {
					enabled: false
				},
				stroke: {
					curve: 'stepline',
					width: 2
				},
				title: {
					text: 'PV Time Curve',
					align: 'left',
					style: {
						color: '#1D95F3'
					},
				},
				xaxis: {
					type: 'datetime',
					// min: new Date().getTime() - 10 * 1000, // 10 seconds
					max: new Date().getTime() + XAXIS_STEP_SIZE,
					labels: {
						datetimeUTC: false
					}
				},
				yaxis: {
					decimalsInFloat: 3
				},
			}
		}
	},
	created() {
		this.connect();
		this.intervalId = setInterval(this.replicateLastValue, 1000);  // Every 1 seconds
	},
	beforeUnmount(){
		this.ws.close();
		if(this.intervalId) {
			clearInterval(this.intervalId);
			this.intervalId = null;
		}
	},
	methods: {
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
					let serie = this.findSerie(message.pv);
					if(serie) {
						if(message.connected) {
							let timestamp;
							if(!serie.data.length) {
								timestamp = Date.now(); // First point
							} else {
								timestamp = message.seconds * 1000 + message.nanos / 1000000; // Appended point
							}
							let value = message.value;
							serie.data.push([ timestamp, value ]);
							this.updateXAxisMax(timestamp);
						}
					}
				}
			};
		},
		// Replicate the last value with the current timestamp
		replicateLastValue() {
			if(!this.series || !this.series.length) return;
			for(let serie of this.series) {
				if(serie.stopped === true) {
					continue;
				}
				let points = serie.data;
				if(points && points.length) {
					let lastPoint = points[points.length - 1];
					let lastTimestamp = lastPoint[0];
					let lastValue = lastPoint[1];
					let currentTimestamp = Date.now();
					if(currentTimestamp - lastTimestamp >= 1000) { // Value has not been updated for 1 second
						points.push([currentTimestamp, lastValue]);
						this.updateXAxisMax(currentTimestamp);
					}
				}
			}
		},
		forceRerender() {
			this.componentKey += 1
		},
		updateXAxisMax(timestamp) {
			if(timestamp >= this.chartOptions.xaxis.max) {
				this.chartOptions.xaxis.max += XAXIS_STEP_SIZE;
				this.forceRerender(); // Re-render the apexchart component to refresh the updated max length
			}
		},
		findSerie(pvname) {
			return this.series.find(element => element.name === pvname);
		},
		removeSerie(pvname) {
			for(let i = this.series.length - 1; i >= 0; i--) {
				let serie = this.series[i];
				if(serie && serie.name === pvname) {
					this.series.splice(i, 1);
				}
			}
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
			if(this.findSerie(pvname)) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: `The specified PV already exists\n${pvname}`, life: 5000 });
			} else {
				this.series.push({ name: pvname, data: [] });
				this.sendMessage({ "type": "subscribe", "pvs": [ pvname ] });
			}
		},
		startSerie(pvname) {
			for(let serie of this.series) {
				if(serie.name === pvname) {
					if(serie.stopped === true) {
						serie.stopped = false;
					}
				}
			}
		},
		stopSerie(pvname) {
			for(let serie of this.series) {
				if(serie.name === pvname) {
					serie.stopped = true;
				}
			}
		},
		startPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.sendMessage({ "type": "subscribe", "pvs": [ pvname ] });
			this.startSerie(pvname);
		},
		stopPV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.sendMessage({ "type": "clear", "pvs": [ pvname ] });
			this.stopSerie(pvname);
		},
		removePV(pvname) {
			if(!pvname) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			pvname = this.prefixPV(pvname, this.prefix);
			this.removeSerie(pvname);
			this.sendMessage({ "type": "clear", "pvs": [ pvname ] });
		},
		removeAllPVs() {
			this.sendMessage({ "type": "clear", "pvs": this.series.map(element => element.name) });
			this.series = [];
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

</style>
