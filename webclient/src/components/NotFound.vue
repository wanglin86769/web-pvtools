<template>
	<div class="grid">
        <div class="col-12">
            <div class="card">
				<h1>Page Not Found</h1>
				<p>The requested resource is not available.</p>
				<Button label="Go to homepage" @click="goHomepage" />
			</div>
		</div>
	</div>
</template>

<script>
	export default {
		methods: {
			goHomepage(){
				this.$router.push({path: '/'});
			}
		}
	}
</script>

<style scoped>

</style>