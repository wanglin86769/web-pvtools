<template>
    <div class="grid">
        <div class="col-12">
            <div class="card">
                <h4 style="color: RGB(29,149,243); font-size: 1.2em;">1. Introduction</h4>
                <p><b>web-pvtools</b> provides a series of EPICS PV tools for web, the PV client part is based on Phoebus core-pv, which supports both Channel Access and PV Access.</p>
                <h4 style="color: RGB(29,149,243); font-size: 1.2em;">2. Frontend</h4>
                <p>The frontend is a single page application based on Vue.js framework and provides the following PV tools,</p>
                <ul>
                    <li>PV status monitor</li>
                    <li>pvget</li>
                    <li>pvput</li>
                    <li>pvmonitor</li>
                    <li>pvinfo</li>
                    <li>probe</li>
                    <li>StripTool</li>
                    <li>X-Y Plot</li>
                </ul>
                <h4 style="color: RGB(29,149,243); font-size: 1.2em;">3. Backend</h4>
                <p>The backend is a customized version of PVWS (PV WebSocket) renamed with PVWA (PV Web Access) including the following modifications,</p>
                <ul>
                    <li>Replace Java Servlet with Spring Boot to simplify the REST API code</li>
                    <li>Add "connected" field in WebSocket response to show PV connection status</li>
                    <li>Replace the Base64 encoding array value with raw data to simplify both WebSocket client and server</li>
                    <li>Add separate REST APIs for pvget, pvput and pvinfo</li>
                </ul>
                <h4 style="color: RGB(29,149,243); font-size: 1.2em;">4. MIT license</h4>
                <p>Institute of High Energy Physics (IHEP) Copyright © 2024</p>
                <p>China Spallation Neutron Source (CSNS) Copyright © 2024</p>
                <h4 style="color: RGB(29,149,243); font-size: 1.2em;">5. Contact information</h4>
                <p>Author: Lin Wang</p>
                <p>Email: wanglin@ihep.ac.cn</p>
            </div>
        </div>
    </div>
</template>

<script>
export default {
	
}
</script>

<style scoped>

</style>
