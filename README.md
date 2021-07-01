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

## prepare code
git clone https://github.com/addx-open/addx_video_open_sdk.git
cd addx_video_open_sdk
git submodule add  https://github.com/addx-open/android-demo.git  *your dir*/addx_video_open_sdk/demo/android-demo
git submodule init
git submodule update

## Access method
### 1.Join in the main build.gradle

```groovy
allprojects {
    repositories {
        maven { url "http://82.157.61.22:8081/repository/maven-releases" }
        maven { url "https://jitpack.io" }
        maven { url 'https://zendesk.jfrog.io/zendesk/repo' }
    }
}
```
### 2.Join in the project build.gradle
```groovy
 defaultConfig {
        javaCompileOptions{
            annotationProcessorOptions{
                includeCompileClasspath = true
            }
        }
    }
    implementation 'com.addx.ai:addxvideo:1.0.3'
    implementation 'com.addx.ai:addxbind:1.0.3'
    implementation 'com.addx.ai:addxsettings:1.0.4'
```
## Example: (For more examples, please download the demo)
```java
AddxContext.getInstance().initA4xSdk(getApplicationContext(), "netvue", "zh", "CN", AddxContext.BuildEnv.STAGING, AddxNode.STRAGE_NODE_CN, token, null);
```

The latest version (for detailed interface documentation, please refer to [addx SDK documentation](https://www.showdoc.com.cn/AddxAndroidSdk "addx SDK documentation"))：

1.0.0
