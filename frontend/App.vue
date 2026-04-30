<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
			 // 这里也可以加，防止应用启动瞬间闪烁、导航栏隐藏
		    uni.hideTabBar()
		},
		onShow: function() {
			console.log('App Show')
			// 在这里加一次，所有页面显示时都会执行
		    uni.hideTabBar()
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
</style>
