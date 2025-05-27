<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					CA Put
				</div>

				<div style="margin-bottom: 1em;">
					<label style="font-size: 1.2em; font-weight: bold; margin-right: .5em;">Web Service Status</label>
					<i v-if="serviceAvailable===true" class="fa fa-check-circle" v-tooltip.top="'Connected'" style="font-size: 1.8rem; color: green;"></i>
					<i v-else class="fa fa-times-circle" v-tooltip.top="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
				</div>

				<Toolbar class="mb-0">
					<template v-slot:start>
						<Dropdown style="margin-right: 2em" v-model="prefix" :options="pvTypeOptions" optionLabel="name" optionValue="value"></Dropdown>
					</template>

					<template v-slot:end>
						<div class="p-inputgroup">
							<Button label="Clear" icon="pi pi-refresh" severity="warning" @click="clearData()" />
							<InputText placeholder="PV name" style="width: 20em" v-model="inputPVName" v-on:keyup.enter="putPV(inputPVName, inputPVValue)" autofocus />
							<InputText placeholder="PV value" style="width: 20em" v-model="inputPVValue" v-on:keyup.enter="putPV(inputPVName, inputPVValue)" />
							<Button label="Put" icon="pi pi-play-circle" severity="success" style="width: 80px" @click="putPV(inputPVName, inputPVValue)" />
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
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">Old value</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.old_value }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 30%"><span style="margin-left: 0.5em;">New value</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ pvData.new_value }}</span></td>
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
			inputPVValue: '',
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
		async putPV(pvname, pvvalue) {
			if(!pvname || !pvvalue) {
				this.$toast.add({ severity: 'warn', summary: 'Warn Message', detail: 'PV name or PV value is empty', life: 5000 });
				return;
			}
			this.loading = true;
            try {
				this.pvData = {};
				let pv = await this.webService.pvPut(this.prefixPV(pvname, this.prefix), this.processValueInput(pvvalue));
				if(pv && pv.success === false) {
					this.$toast.add({ severity: 'error', summary: 'Failed to put PV value', detail: pv.message });
					return;
				}
				this.pvData.pv_name = pv.name;
				this.pvData.pv_type = pv.pv_type;
				this.pvData.vtype = pv.vtype;
				this.pvData.size = pv.size;
				this.pvData.old_value = pv.old_value;
				this.pvData.new_value = pv.new_value;
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
