<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					CA Get
				</div>

				<Toolbar class="mb-0">
					<template v-slot:start>
						<label style="font-size: 1.2em; font-weight: bold; margin-right: .5em;">Web Service Status</label>
						<i v-if="serviceAvailable===true" class="fa fa-check-circle" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
						<i v-else class="fa fa-times-circle" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
					</template>

					<template v-slot:end>
						<Dropdown style="margin-right: 2em" v-model="prefix" :options="pvTypeOptions" optionLabel="name" optionValue="value"></Dropdown>
						<div class="p-inputgroup">
							<Button label="Clear" icon="pi pi-refresh" severity="warning" @click="clearData()" />
							<InputText placeholder="PV name" style="width: 20em" v-model="inputPVName" v-on:keyup.enter="getPV()" />
							<Button label="Get" icon="pi pi-play-circle" severity="success" style="width: 80px" @click="getPV()" />
						</div>
					</template>
				</Toolbar>

				<table width="100%" style="border-collapse: collapse; margin-bottom: 4em; margin-top: 4em;">
					<tr height="40em">
						<th colspan="4" align="left" style="text-align: center;">
							<span style="font-weight: bold; font-size: 1.5em;">Data</span>
							<i v-if="loading" style="margin-left: 0.5em; color: RGB(29,149,243);" class="fa fa-spinner fa-spin fa-fw fa-2x"></i>
						</th>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">PV name</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ unprefixPV(pvData.pv_name) }}</span></td>
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
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Timestamp</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.timestamp }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Alarm</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.alarm }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Value</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.value }}</span></td>
					</tr>
				</table>
			</div>
		</div>
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
			pvData: {},
			serviceAvailable: false,
			inputPVName: '',
			prefix: Utility.defaultPrefix,
			pvTypeOptions: Utility.pvTypeOptions,
			loading: false,
			intervalId: null,
		}
	},
	webService: null,
	created() {
		this.webService = new WebService();
		this.checkServiceStatus();
		this.intervalId = setInterval(this.checkServiceStatus, 30000);  // Every 30 seconds
	},
	beforeUnmount(){
		if(this.intervalId) {
			clearInterval(this.intervalId);
			this.intervalId = null;
		}
	},
	methods: {
		async checkServiceStatus() {
            this.serviceAvailable = await this.webService.isServiceAvailable();
        },
		prefixPV(pvname, prefix) {
			return Utility.prefixPV(pvname, prefix);
		},
		unprefixPV(pvname) {
			return Utility.unprefixPV(pvname);
		},
		async getPV() {
			if(!this.inputPVName) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name is empty', life: 5000 });
				return;
			}
			this.loading = true;
            try {
				this.pvData = {};
				let pv = await this.webService.pvGet(this.prefixPV(this.inputPVName, this.prefix));
				if(pv && pv.success === false) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: pv.message });
					return;
				}
				this.pvData.pv_name = pv.name;
				this.pvData.pv_type = pv.pv_type;
				this.pvData.vtype = pv.vtype;
				this.pvData.size = pv.size;
				this.pvData.timestamp = this.showDateTimeMS(pv.seconds * 1000 + pv.nanos / 1000000);
				this.pvData.alarm = `${pv.alarm_name}    ${pv.alarm_severity}`;
				this.pvData.value = pv.pv_type === 'VString' ? pv.text : pv.value;
			} catch(error) {
				if(error.response) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: error.response.data });
				} else {
					this.$toast.add({ severity: 'error', summary: 'Failed to get PV value', detail: error.message });
				}
			} finally {
				this.loading = false;
			}
		},
		clearData() {
			this.pvData = {};
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

:deep(.p-datatable) thead th {
    color: RGB(29,149,243);
}

table, th, td {
  border: 2px solid RGB(233,235,238);
}
	
</style>
