import {createRouter, createWebHistory} from 'vue-router';


const routes = [
	{
		path: '/',
		name: 'home',
		component: () => import('./components/Home.vue')
	},
    {
        path: '/caget',
        name: 'caget',
        component: () => import('./components/tools/CAGet.vue'),
    },
    {
        path: '/caput',
        name: 'caput',
        component: () => import('./components/tools/CAPut.vue'),
    },
    {
        path: '/camonitor',
        name: 'camonitor',
        component: () => import('./components/tools/CAMonitor.vue'),
    },
    {
        path: '/cainfo',
        name: 'cainfo',
        component: () => import('./components/tools/CAInfo.vue'),
    },
    {
        path: '/probe',
        name: 'probe',
        component: () => import('./components/tools/Probe.vue'),
    },
    {
        path: '/striptool',
        name: 'striptool',
        component: () => import('./components/tools/StripTool.vue'),
    },
    {
        path: '/xyplot',
        name: 'xyplot',
        component: () => import('./components/tools/XYPlot.vue'),
    },
    {
        path: '/config',
        name: 'config',
        component: () => import('./components/status/Config.vue'),
    },
    {
        path: '/info',
        name: 'info',
        component: () => import('./components/status/Info.vue'),
    },
    {
        path: '/summary',
        name: 'summary',
        component: () => import('./components/status/Summary.vue'),
    },
	{
		path: '/about',
		name: 'about',
		component: () => import('./components/About.vue')
	},
    {
        path: "/:catchAll(.*)",
        name: 'notfound',
        component: () => import('./components/NotFound.vue')
    },
];

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
    routes: routes
});


export default router;