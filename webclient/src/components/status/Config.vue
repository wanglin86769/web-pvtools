<template>
	<div class="grid">
		<div class="col-12">
			<div class="card">
				<div style="text-align: center; color: RGB(29,149,243); font-size: 1.8em; font-weight: bold; margin-bottom: .5em;">
					<span>Server Config and Status</span>
					<i v-if="loading" style="margin-left: 0.5em" class="fa fa-spinner fa-spin fa-fw fa-2x"></i>
				</div>

				<table border="1" width="100%" style="border-collapse: collapse;">
					<tr height="40em">
						<th colspan="1" align="center"><span style="font-weight: bold;">Server</span></th>
						<th colspan="1" align="center"><span style="font-weight: bold;">Config</span></th>
						<th colspan="1" align="center" style="text-align: center"><span style="font-weight: bold;">Status</span></th>
					</tr>
					<tr height="40em">
						<td colspan="1" align="center" style="text-align: center"><span style="margin-left: 0.5em;">Web Service URL</span></td>
						<td colspan="1" align="center" style="text-align: center"><span style="margin-left: 0.5em;">{{ webServiceURL }}</span></td>
						<td colspan="1" align="center" style="text-align: center">
							<span>
								<i v-if="webServiceConnected===true" class="fa fa-check-circle" v-tooltip.left="'Connected'" style="font-size: 1.8rem; color: green;"></i>
								<i v-else class="fa fa-times-circle" v-tooltip.left="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
							</span>
						</td>
					</tr>
					<tr height="40em">
						<td colspan="1" align="center" style="text-align: center"><span style="margin-left: 0.5em;">WebSocket URL</span></td>
						<td colspan="1" align="center" style="text-align: center"><span style="margin-left: 0.5em;">{{ webSocketURL }}</span></td>
						<td colspan="1" align="center" style="text-align: center">
							<span>
								<i v-if="webSocketConnected===true" class="fa fa-check-circle" v-tooltip.left="'Connected'" style="font-size: 1.8rem; color: green;"></i>
								<i v-else class="fa fa-times-circle" v-tooltip.left="'Disconnected'" style="font-size: 1.8rem; color: red;"></i>
							</span>
						</td>
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
			webServiceURL: config.webServicePath,
            webSocketURL: config.webSocketPath,

            webServiceConnected: false,
            webSocketConnected: false,

			loading: false,
			intervalId: null,
		}
	},
	webService: null,
	created() {
		this.webService = new WebService();
		this.checkServerStatus();
		this.intervalId = setInterval(this.checkServiceStatus, 30000);  // Every 30 seconds
	},
	mounted() {
		
	},
	beforeUnmount(){
		if(this.intervalId) {
			clearInterval(this.intervalId);
			this.intervalId = null;
		}
	},
	methods: {
		async checkServiceStatus() {
            try {
                let info = await this.webService.getServerInfo();
                if(info && info.start_time && info.jre) {
                    this.webServiceConnected = true;
                } else {
                    this.webServiceConnected = false;
                }
			} catch(error) {
                this.webServiceConnected = false;
				if(error.response) {
					console.log(error.response.data);
				} else {
					console.log(error.message);
				}
			}
        },
        checkSocketStatus() {
            this.webSocketConnected = false;
            const test = 'test';
            let webSocket = new WebSocket(config.webSocketPath);
            webSocket.onopen = (event) => {
                webSocket.send(JSON.stringify({ type: 'echo', message: test }));
			};
			webSocket.onmessage = (event) => {
				let message = JSON.parse(event.data);
				if(message.type === 'echo' && message.message === test) {
					this.webSocketConnected = true;
                    webSocket.close();
				}                
			};
        },
        async checkServerStatus() {
            this.loading = true;
			try {
				await this.checkServiceStatus();
				this.checkSocketStatus();
				this.serverStatusDialogDisplay = true;
			} catch(error) {
				if(error.response) {
					console.log(error.response.data);
				} else {
					console.log(error.message);
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
