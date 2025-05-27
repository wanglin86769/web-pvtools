<template>
	<div class="layout-wrapper" >
        <Toast position="top-right" />
        
        <div class="above-footer-wrapper">
            <Menubar :model="menu">
                <template #start>
                    
                </template>
                <template #end>
                    <span class="logo">web-pvtools</span>
                    <img alt="logo" src="@/assets/images/logo_white.png" style="vertical-align: middle; margin-right: 10px;" height="30">
                    
                </template>
            </Menubar>

            <div style="overflow-x: hidden">
                <router-view />
            </div>
        </div>

        <div class="layout-footer" style="text-align: center">
            <span class="footer-text">{{ softwareUser }}</span>
        </div>
	</div>
</template>

<script>
import config from './config/configuration.js';

export default {
    data() {
        return {
            softwareUser: config.softwareUser,

            menu: [
                {
                    label: 'Home',
                    icon: 'pi pi-fw pi-home',
                    command: () => {
                        this.$router.push({ name: 'home'});
                    }
                },
                {
                    label: 'Tools',
                    icon: 'pi pi-fw pi-hammer',
                    items: [
                        {
                            label: 'pvget',
                            icon: 'pi pi-fw pi-arrow-circle-down',
                            command: () => {
                                this.$router.push({ name: 'pvget'});
                            }
                        },
                        {
                            label: 'pvput',
                            icon: 'pi pi-fw pi-pen-to-square',
                            disabled: !config.writable,
                            command: () => {
                                this.$router.push({ name: 'pvput'});
                            }
                        },
                        {
                            label: 'pvmonitor',
                            icon: 'pi pi-fw pi-desktop',
                            command: () => {
                                this.$router.push({ name: 'pvmonitor'});
                            }
                        },
                        {
                            label: 'pvinfo',
                            icon: 'pi pi-fw pi-info-circle',
                            command: () => {
                                this.$router.push({ name: 'pvinfo'});
                            }
                        },
                        {
                            separator: true
                        },
                        {
                            label: 'probe',
                            icon: 'pi pi-fw pi-minus',
                            command: () => {
                                this.$router.push({ name: 'probe'});
                            }
                        },
                        {
                            label: 'StripTool',
                            icon: 'pi pi-fw pi-chart-line',
                            command: () => {
                                this.$router.push({ name: 'striptool'});
                            }
                        },
                        {
                            label: 'X/Y Plot',
                            icon: 'pi pi-fw pi-wave-pulse',
                            command: () => {
                                this.$router.push({ name: 'xyplot'});
                            }
                        },
                    ]
                },
                {
                    label: 'Status',
                    icon: 'fa fa-fw fa-cogs',
                    items: [
                        {
                            label: 'Config',
                            icon: 'fa fa-fw fa-cog',
                            command: () => {
                                this.$router.push({ name: 'config'});
                            }
                        },
                        {
                            label: 'Info',
                            icon: 'pi pi-fw pi-info-circle',
                            command: () => {
                                this.$router.push({ name: 'info'});
                            }
                        },
                        {
                            label: 'Summary',
                            icon: 'pi pi-fw pi-list',
                            command: () => {
                                this.$router.push({ name: 'summary'});
                            }
                        },
                    ]
                },
                {
                    label: 'About',
                    icon: 'pi pi-fw pi-question-circle',
                    command: () => {
                        this.$router.push({ name: 'about'});
                    }
                },
            ],
        }
    },
    created() {
        
	},
    methods: {
        
    },
}
</script>

<style lang="scss">
@import './assets/layout/layout.scss';
@import './App.scss';

</style>
