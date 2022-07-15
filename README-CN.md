#  **Addx Android Sdk**


（详细的接口文档请查看[addx SDK 文档](https://www.showdoc.com.cn/A4XAndroidSdk "addx SDK 文档")）：


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
    implementation 'com.addx.ai:addxvideo:1.0.3'
    implementation 'com.addx.ai:addxbind:1.0.3'
    implementation 'com.addx.ai:addxsettings:1.0.4'
```
**3. apptheme需要继承自Theme.MaterialComponents.*
```
```
## 2. 事例：(更多事例请下载demo)
```
AddxContext.getInstance().initA4xSdk(getApplicationContext(), "netvue", "zh", "CN", AddxContext.BuildEnv.STAGING, AddxNode.STRAGE_NODE_CN, token, null);
```
最新版本（详细的接口文档请查看[addx SDK 文档](https://www.showdoc.com.cn/A4XAndroidSdk "addx SDK 文档")）：

1.0.0
