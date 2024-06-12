import axios from 'axios';
import config from '@/config/configuration.js';

export default class WebService {

	async isServiceAvailable() {
		let available;
		try {
			let url = `${config.webServicePath}/info`;
			let res = await axios.get(url);
			let info = res.data;
			available = (info && info.start_time && info.jre) ? true : false;
		} catch(error) {
			available = false;
		}
		return available;
	}

	async getServerInfo() {
		let url = `${config.webServicePath}/info`;
		let res = await axios.get(url);
		return res.data;
	}

	async getServerInfoWithEnv() {
		let url = `${config.webServicePath}/info?env=true`;
		let res = await axios.get(url);
		return res.data;
	}

	async getServerSummary() {
		let url = `${config.webServicePath}/summary`;
		let res = await axios.get(url);
		return res.data;
	}

	async getServerSocket() {
		let url = `${config.webServicePath}/socket`;
		let res = await axios.get(url);
		return res.data;
	}

	async getServerPool() {
		let url = `${config.webServicePath}/pool`;
		let res = await axios.get(url);
		return res.data;
	}

	async pvGet(pvname) {
		let url = `${config.webServicePath}/pvget?pv=${pvname}`;
		let res = await axios.get(url);
		return res.data;
	}

	async pvPut(pvname, value) {
		let data = { pv: pvname, value: value };
		let url = `${config.webServicePath}/pvput`;
		let res = await axios.put(url, data);
		return res.data;
	}

	async pvInfo(pvname) {
		let url = `${config.webServicePath}/pvinfo?pv=${pvname}`;
		let res = await axios.get(url);
		return res.data;
	}

}