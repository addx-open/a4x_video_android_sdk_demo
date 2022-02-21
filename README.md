#  **Addx Android Sdk**

(please check the detailed interface documentation[addx SDK documentation](https://docs.vicoo.tech/#/app/androidSdk "addx SDK documentation")）：

## get demo code
```
git clone https://github.com/addx-open/a4x_video_android_sdk_demo.git
cd a4x_video_android_sdk_demo
git submodule add  https://github.com/addx-open/android-demo.git  ./demo/android-demo
git submodule init
git submodule update
```
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
    implementation 'com.addx.ai:addxvideo:1.2.0_beta'
    implementation 'com.addx.ai:addxbind:1.2.0_beta'
    implementation 'com.addx.ai:addxsettings:1.2.0_beta'
    implementation 'com.addx.ai:thememain:1.2.0_beta'
```
**3. your apptheme need to extends Theme.MaterialComponents.* for example  Theme.MaterialComponents.Light.NoActionBar
```
```
## Example: (For more examples, please download the demo)
```java
A4xContext.getInstance().initA4xSdk(getApplicationContext(), "netvue", "zh", "CN", AddxContext.BuildEnv.STAGING, AddxNode.STRAGE_NODE_CN, token, null);
```

The latest version (for detailed interface documentation, please refer to [addx SDK documentation](https://docs.vicoo.tech/#/app/androidSdk "addx SDK documentation"))：

1.2.0
