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
**1.在主build.gradle加入**
```
allprojects {
    repositories {
        maven { url "http://82.157.61.22:8081/repository/maven-releases" }
        maven { url "https://jitpack.io" }
        maven { url 'https://zendesk.jfrog.io/zendesk/repo' }
    }
}
```
**2.在工程build.gradle加入**
```
    defaultConfig {
        javaCompileOptions{
            annotationProcessorOptions{
                includeCompileClasspath = true
            }
        }
    }
    implementation 'com.addx.ai:addxvideo:1.0.2'
    implementation 'com.addx.ai:addxbind:1.0.2'
    implementation 'com.addx.ai:addxsettings:1.0.2'
```
## 2. 事例：(更多事例请下载demo)
```
AddxContext.getInstance().initA4xSdk(getApplicationContext(), "netvue", "zh", "CN", AddxContext.BuildEnv.STAGING, AddxNode.STRAGE_NODE_CN, token, null);
```
最新版本（详细的接口文档请查看[addx SDK 文档](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK 文档")）：

1.0.0
