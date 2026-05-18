<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
			uni.hideTabBar()
		},
		onShow: function() {
			console.log('App Show')
			uni.hideTabBar()
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	page {
		--theme-bg: #f7f4ee;
		--theme-surface: rgba(255, 255, 255, 0.92);
		--theme-surface-strong: #ffffff;
		--theme-primary: #8c80d8;
		--theme-primary-deep: #746fc2;
		--theme-accent: #b9a0d5;
		--theme-ink: #343030;
		--theme-muted: #8f88a6;
		--theme-line: rgba(140, 128, 216, 0.18);
		--theme-shadow: 0 18rpx 46rpx rgba(121, 110, 176, 0.14);
		--theme-shadow-soft: 0 10rpx 28rpx rgba(121, 110, 176, 0.09);
		--theme-gradient: linear-gradient(135deg, #efe8fb 0%, #e1d8f7 52%, #f8f4ef 100%);
		--theme-gradient-strong: linear-gradient(135deg, #b9a0d5 0%, #8c80d8 56%, #746fc2 100%);
		--theme-radius-xl: 32rpx;
		--theme-radius-lg: 24rpx;
		--theme-radius-md: 18rpx;
		background: var(--theme-bg);
		color: var(--theme-ink);
	}

	body,
	uni-page-body {
		background: var(--theme-bg);
		color: var(--theme-ink);
		font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
	}

	view,
	text,
	input,
	button {
		box-sizing: border-box;
	}

	button {
		margin: 0;
	}

	button::after {
		border: none;
	}
</style>
