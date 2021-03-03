#  **Addx live video Android Sdk**

## Functional Overview
		Addx video live streaming Android Sdk utilizes the most advanced webrtc technology to provide hardware device live streaming and addx function interface packaging, accelerating the application development process from different levels, and providing a complete set of customized solutions for users without R&D capabilities. Users with R&D capabilities can freely choose to use part of the page or part of the control, which greatly enhances the expansion space for users to freely customize and choose freely

Mainly includes the following functions (please check the detailed interface documentation[addx SDK documentation](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK documentation")）：

Live control
Decoding component
Device List
user settings
Device binding
Device settings
Photo album
sdcard look back
Addx service api for all functions

## Access method
### 1.Join in the main build.gradle

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
### 2.Join in the project build.gradle
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
## Example: (For more examples, please download the demo)
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
				ToastUtils.showShort("Failed to get device");
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
		ToastUtils.showShort("Failed to get device");
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

The latest version (for detailed interface documentation, please refer to [addx SDK documentation](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK documentation"))：

1.0.0
