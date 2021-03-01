#  **Addx视频直播 Android Sdk**

## 功能概述
Addx视频直播 Android Sdk利用了最先进的webrtc技术提供了硬件设备直播以及addx功能的接口封装，从不同的层面，加速应用开发过程，

对于没有研发能力的用户提供了一整套的定制解决方案，对于有研发能力的用户可以自由选择使用部分页面还是部分控件，大大的提升了用户自由定制自由选择的扩展空间

主要包括了以下功能（详细的接口文档请查看[addx SDK 文档](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK 文档")）：

直播控件
解码组件
设备列表
用户设置
设备绑定
设备设置
相册
sdcard回看
所有功能的addx服务api

## 接入方法
### 1.在主build.gradle加入

```groovy
allprojects {
		google()
		jcenter()
		maven { url "https://jitpack.io" }
		maven { url 'https://zendesk.jfrog.io/zendesk/repo' }
		maven { url 'http://developer.huawei.com/repo/' }
		maven { url 'https://zendesk.jfrog.io/zendesk/repo' }
}
```
### 2.在工程build.gradle加入
```groovy

defaultConfig {
	flavorDimensions "versionCode"
	multiDexEnabled true
}
android {
	buildTypes {
		params {
			Map<String, Object> placeHolderMap = new HashMap<>()
			placeHolderMap.put("XG_ACCESS_ID", "1500010573")
			placeHolderMap.put("XG_ACCESS_KEY", "A1ML07L4XF4L")
			placeHolderMap.put("huaweiId", "100926381")
			placeHolderMap.put("bugsnagKey", "0e1800896236cfa86bd5bece344f7a0e")
			manifestPlaceholders(placeHolderMap)
		}
		debug {
			initWith params
		}
		release {
			initWith params
		}
	}

	variantFilter { variant ->
		def names = variant.flavors*.name
		def buildType = variant.buildType.name
		def name0 = names.get(0)
		if (buildType.contains("params")) {
			setIgnore(true)
			return
		}
		println("names" + names + ",name0=" + name0 + ",buildType=" + buildType + ",ignore=false")
	}
}
configurations {
	compile.exclude group: 'org.jetbrains', module: 'annotations'
}
```
## 事例：(更多事例请下载demo)
```java
private Runnable autoPlayRunnable = new Runnable() {
	@Override
	public void run() {
		LogUtils.d(TAG,"initPlayer========autoPlayRunnable");
		mNoControlAddxVideoView.startPlay();
	}
};
void listDevice() {
	Subscription subscribe = ApiClient.getInstance()
	.listDevice(new BaseEntry())
	.subscribeOn(Schedulers.io())
	.subscribe(new HttpSubscriber() {
	@Override
	public void doOnNext(AllDeviceResponse allDeviceResponse) {
		mNoControlAddxVideoView.post(()->{
			LogUtils.d(TAG,"initPlayer========doOnNext");
			loadding.setVisibility(View.INVISIBLE);
			if (allDeviceResponse.getResult() < Const.ResponseCode.CODE_OK
			|| allDeviceResponse.getData() == null
			|| allDeviceResponse.getData().getList() == null) {
				ToastUtils.showShort("获取设备失败");
				return;
			}
			LogUtils.d(TAG,"initPlayer========doOnNext===ok");
			allDevice = allDeviceResponse.getData().getList();
			DeviceManager.getInstance().putOrUpdate(allDevice);
			if(allDevice != null && !allDevice.isEmpty()){
				initPlayer();
				beginAutoPlay();
			}
	});
}
@Override
public void doOnError(Throwable e) {
	super.doOnError(e);
	mNoControlAddxVideoView.post(() -> {
		ToastUtils.showShort("获取设备失败");
		loadding.setVisibility(View.INVISIBLE);
	});
	}
	});
	mSubscription.add(subscribe);
}

protected void beginAutoPlay(){
	if(mNoControlAddxVideoView != null){
		mNoControlAddxVideoView.postDelayed(autoPlayRunnable, 400);
	}
}
private void initPlayer() {
	LogUtils.d(TAG,"initPlayer========");
	if (mNoControlAddxVideoView != null) {
	LogUtils.d(TAG,"initPlayer========1111");
	mNoControlAddxVideoView.setDeviceBean(allDevice.get(0));
	mNoControlAddxVideoView.init(this);
	mNoControlAddxVideoView.setMVideoCallBack(new SimpleAddxViewCallBack() {
		@Override
		public void onStartPlay() {
			if(getLifecycle().getCurrentState() == Lifecycle.State.RESUMED){
			}
		}
	});
	View view = mNoControlAddxVideoView.findViewById(R.id.tv_download_speed);
	if(view != null){
		view.setVisibility(View.GONE);
	}
	}
}
```

最新版本（详细的接口文档请查看[addx SDK 文档](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK 文档")）：

1.0.0
