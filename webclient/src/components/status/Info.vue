<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: 1em;">
					<span>Server Info</span>
					<i v-if="loading" style="margin-left: 0.5em" class="fa fa-spinner fa-spin fa-fw fa-2x"></i>
				</div>

				<table v-if="serverInfo" border="1" width="100%" style="border-collapse: collapse; margin-bottom: 4em;">
					<tr height="40em">
						<th colspan="4" align="left" style="text-align: center;"><span style="font-weight: bold">Status</span></th>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 40%"><span style="margin-left: 0.5em;">Start Time</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ serverInfo.start_time }}</span></td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="left" style="width: 40%"><span style="margin-left: 0.5em;">JRE</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ serverInfo.jre }}</span></td>
					</tr>
				</table>

				<table v-if="serverInfo" border="1" width="100%" style="border-collapse: collapse;">
					<tr height="40em">
						<th colspan="4" align="left" style="text-align: center;"><span style="font-weight: bold">Environment Variables</span></th>
					</tr>
					<tr height="40em" v-for="(item, index) in serverInfo.env" :key="index">
						<td colspan="1" align="left" style="width: 40%"><span style="margin-left: 0.5em;">{{ item.key }}</span></td>
						<td colspan="3" align="left"><span style="margin-left: 0.5em;">{{ item.value }}</span></td>
					</tr>
					<tr height="40em" v-if="!serverInfo || !serverInfo.env || !serverInfo.env.length">
						<td colspan="4" align="left" style="width: 40%"><span style="margin-left: 0.5em; color: orange;">No records found.</span></td>
					</tr>
				</table>
			</div>
		</div>
	</div>

</template>

<script>
import config from '@/config/configuration.js';
import WebService from '@/service/WebService';

export default {
	data() {
		return {
			serverInfo: {},
			loading: false,
		}
	},
	webService: null,
	created() {
		this.webService = new WebService();
		this.getServerInfo();
	},
	mounted() {
		
	},
	methods: {
		async getServerInfo() {
            this.loading = true;
            try {
                this.serverInfo = await this.webService.getServerInfoWithEnv();
			} catch(error) {
				if(error.response) {
					this.$toast.add({ severity: 'error', summary: 'Failed to get server info', detail: error.response.data });
				} else {
					this.$toast.add({ severity: 'error', summary: 'Failed to get server info', detail: error.message });
				}
			} finally {
				this.loading = false;
			}
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
