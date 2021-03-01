<b>Addx视频直播 Android Sdk</b>


<b>功能概述</b><br />
Addx视频直播 Android Sdk利用了最先进的webrtc技术提供了硬件设备直播以及addx功能的接口封装，从不同的层面，加速应用开发过程，<br />

对于没有研发能力的用户提供了一整套的定制解决方案，对于有研发能力的用户可以自由选择使用部分页面还是部分控件，大大的提升了用户自由定制自由选择的扩展空间<br />

主要包括了以下功能（详细的接口文档请查看）：<br />

直播控件<br />
解码组件<br />
设备列表<br />
用户设置<br />
设备绑定<br />
设备设置<br />
相册<br />
sdcard回看<br />
所有功能的addx服务api<br />

<b>接入方法</b><br />
1.在主build.gradle加入<br />

allprojects {<br />
        maven{url 'file:///home/yuanshiyue/addx/addx_video_open_sdk/mavenlibs'}<br />
        google()<br />
        jcenter()<br />
        maven { url "https://jitpack.io" }<br />
        maven { url 'https://zendesk.jfrog.io/zendesk/repo' }<br />
        maven { url 'http://developer.huawei.com/repo/' }<br />
        maven { url 'https://zendesk.jfrog.io/zendesk/repo' }<br />
    }<br />
}<br />
2.在工程build.gradle加入<br />

defaultConfig {<br />
 flavorDimensions "versionCode"<br />
 multiDexEnabled true<br />
}<br />
android {<br />
buildTypes {<br />
params {<br />
    Map<String, Object> placeHolderMap = new HashMap<>()<br />
    placeHolderMap.put("XG_ACCESS_ID", "1500010573")<br />
    placeHolderMap.put("XG_ACCESS_KEY", "A1ML07L4XF4L")<br />
    placeHolderMap.put("huaweiId", "100926381")<br />
    placeHolderMap.put("bugsnagKey", "0e1800896236cfa86bd5bece344f7a0e")<br />
    manifestPlaceholders(placeHolderMap)<br />
}<br />

debug {<br />
    initWith params<br />
}<br />

release {<br />
    initWith params<br />
}<br />

}<br />

variantFilter { variant -><br />
    def names = variant.flavors*.name<br />
    def buildType = variant.buildType.name<br />
    def name0 = names.get(0)<br />
    if (buildType.contains("params")) {<br />
        setIgnore(true)<br />
        return<br />
 }<br />
    println("names" + names + ",name0=" + name0 + ",buildType=" + buildType + ",ignore=false")<br />
}<br />
}<br />
configurations {<br />
    compile.exclude group: 'org.jetbrains', module: 'annotations'<br />
}<br />
<b>事例：(更多事例请下载demo)</b><br />
private Runnable autoPlayRunnable = new Runnable() {<br />
    @Override<br />
 public void run() {<br />
        LogUtils.d(TAG,"initPlayer========autoPlayRunnable");<br />
 mNoControlAddxVideoView.startPlay();<br />
 }<br />
};<br />
void listDevice() {<br />
    Subscription subscribe = ApiClient.getInstance()<br />
            .listDevice(new BaseEntry())<br />
            .subscribeOn(Schedulers.io())<br />
            .subscribe(new HttpSubscriber<AllDeviceResponse>() {<br />
                @Override<br />
 public void doOnNext(AllDeviceResponse allDeviceResponse) {<br />
                    mNoControlAddxVideoView.post(()->{<br />
                        LogUtils.d(TAG,"initPlayer========doOnNext");<br />
 loadding.setVisibility(View.INVISIBLE);<br />
 if (allDeviceResponse.getResult() < Const.ResponseCode.CODE_OK<br />
 || allDeviceResponse.getData() == null<br />
 || allDeviceResponse.getData().getList() == null) {<br />
                            ToastUtils.showShort("获取设备失败");<br />
 return;<br />
 }<br />
                        LogUtils.d(TAG,"initPlayer========doOnNext===ok");<br />
 allDevice = allDeviceResponse.getData().getList();<br />
 DeviceManager.getInstance().putOrUpdate(allDevice);<br />
 if(allDevice != null && !allDevice.isEmpty()){<br />
                            initPlayer();<br />
 beginAutoPlay();<br />
 }<br />
                    });<br />
 }<br />
                @Override<br />
 public void doOnError(Throwable e) {<br />
                    super.doOnError(e);<br />
 mNoControlAddxVideoView.post(() -> {<br />
                        ToastUtils.showShort("获取设备失败");<br />
 loadding.setVisibility(View.INVISIBLE);<br />
 });<br />
 }<br />
            });<br />
 mSubscription.add(subscribe);<br />
}<br />

protected void beginAutoPlay(){<br />
    if(mNoControlAddxVideoView != null){<br /><br />
        mNoControlAddxVideoView.postDelayed(autoPlayRunnable, 400);<br />
 }<br />
}<br />
private void initPlayer() {<br />
    LogUtils.d(TAG,"initPlayer========");<br />
 if (mNoControlAddxVideoView != null) {<br />
        LogUtils.d(TAG,"initPlayer========1111");<br />
 mNoControlAddxVideoView.setDeviceBean(allDevice.get(0));<br />
 mNoControlAddxVideoView.init(this);<br />
 mNoControlAddxVideoView.setMVideoCallBack(new SimpleAddxViewCallBack() {<br />
            @Override<br />
 public void onStartPlay() {<br />
                if(getLifecycle().getCurrentState() == Lifecycle.State.RESUMED){<br />
                }<br />
            }<br />
        });<br /><br />
 View view = mNoControlAddxVideoView.findViewById(R.id.tv_download_speed);<br />
 if(view != null){<br /><br />
            view.setVisibility(View.GONE);<br />
 }<br />
    }<br />
}<br />

<b>最新版本</b><br />
最新版本（详细的接口文档请查看）：<br />

1.0.0
