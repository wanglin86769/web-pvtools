<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					<span>Server Summary</span>
					<i v-if="loading" style="margin-left: 0.5em" class="fa fa-spinner fa-spin fa-fw fa-2x"></i>
				</div>

				<DataTable :value="serverSummary.sockets" showGridlines>
					<template #header>
						<div style="text-align: center;">Active web sockets</div>
					</template>
					<template #empty>
						<span style="color: orange">No records found.</span>
					</template>
					<Column field="id" header="ID"></Column>
					<Column header="Created">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.created)}}
						</template>
					</Column>
					<Column header="Last client message">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.last_client_message)}}
						</template>
					</Column>
					<Column header="Last message sent">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.last_message_sent)}}
						</template>
					</Column>
					<Column field="queued" header="Queued"></Column>
					<Column field="pvs" header="PVs"></Column>
					<Column field="arrays" header="Arrays"></Column>
					<Column field="max_size" header="Max size"></Column>
				</DataTable>

				<DataTable :value="serverSocket.sockets" showGridlines style="margin-top: 2em">
					<template #header>
						<div style="text-align: center;">Active web sockets and PVs</div>
					</template>
					<template #empty>
						<span style="color: orange">No records found.</span>
					</template>
					<Column field="id" header="ID"></Column>
					<Column header="Created">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.created)}}
						</template>
					</Column>
					<Column header="Last client message">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.last_client_message)}}
						</template>
					</Column>
					<Column header="Last message sent">
						<template #body="slotProps">
							{{showDateTime(slotProps.data.last_message_sent)}}
						</template>
					</Column>
					<Column field="queued" header="Queued"></Column>
					<Column header="PVs">
						<template #body="slotProps">
							<div v-for="(item, index) of slotProps.data.pvs" :key="index">
								{{ item.name }}
							</div>
						</template>
					</Column>
				</DataTable>

				<DataTable :value="serverPool" showGridlines style="margin-top: 2em">
					<template #header>
						<div style="text-align: center;">PV connection pool</div>
					</template>
					<template #empty>
						<span style="color: orange">No records found.</span>
					</template>
					<Column field="refs" header="References"></Column>
					<Column field="pv" header="PV"></Column>
				</DataTable>
			</div>
		</div>
	</div>

</template>

<script>
import moment from 'moment';
import config from '@/config/configuration.js';
import WebService from '@/service/WebService';

export default {
	data() {
		return {
			serverSummary: {},
            serverSocket: {},
            serverPool: [],
			loading: false,
		}
	},
	webService: null,
	created() {
		this.webService = new WebService();
		this.getServerSummary();
	},
	mounted() {
		
	},
	methods: {
		async getServerSummary() {
			this.loading = true;
            try {
                this.serverSummary = await this.webService.getServerSummary();
				this.serverSocket = await this.webService.getServerSocket();
				this.serverPool = await this.webService.getServerPool();
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
		showDateTime(value) {
			return value ? moment(value).format("YYYY-MM-DD HH:mm:ss") : null;
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
