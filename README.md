#  **Addx Android Sdk**

(please check the detailed interface documentation[addx SDK documentation](https://docs.vicoo.tech/#/app/androidSdk "addx SDK documentation")）：

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
    implementation 'com.addx.ai:addxvideo:1.5.3_beta'
    implementation 'com.addx.ai:addxbind:1.5.3_beta'
    implementation 'com.addx.ai:addxsettings:1.5.3_beta'
    implementation 'com.addx.ai:thememain:1.5.3_beta'
```
### 3.Join in gradle.properties
```groovy
android.useAndroidX=true
android.enableJetifier=true
```
**4. your apptheme need to extends Theme.MaterialComponents.* for example  Theme.MaterialComponents.Light.NoActionBar
```
```
## Example: (For more examples, please download the demo)
```java
A4xContext.getInstance().initA4xSdk(getApplicationContext(), "netvue", "zh", "CN", AddxContext.BuildEnv.STAGING, AddxNode.STRAGE_NODE_CN, token, null);
```

The latest version (for detailed interface documentation, please refer to [addx SDK documentation](https://docs.vicoo.tech/#/app/androidSdk "addx SDK documentation"))：

1.2.0
