# ApkUpdater
基于DownLoadManager实现安装包更新，安装包缓存，支持MD5校验，支持断点续传，自定义UI，提供了默认UI。


## 演示
按照惯例还是先上图吧。从图片中你可以看出apk是做了缓存的，也就是下载完成后如果没有安装下次再次检查更新时如果发现服务端的版本和缓存的版本一致且MD5值一致则会跳过下载，直接安装。

![demonstrate](materials/gif_apk_updater.gif)

## 下载
#### 第一步：添加 JitPack 仓库到你项目根目录的 gradle 文件中。
```groovy
allprojects {
    repositories {
        //省略部分代码...
        maven { url 'https://jitpack.io' }
    }
}
```
#### 第二步：添加这个依赖。
```groovy
dependencies {
    implementation 'com.github.kelinZhou:ApkUpdater:${Last version here!}'
}
```

## 使用
#### 添加权限
你需要在你的清单文件中添加以下权限：
```html
    <!--网络访问权限-->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!--不弹出通知栏权限-->
    <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION"/>
    <!--DownloadManager-->
    <uses-permission android:name="android.permission.ACCESS_DOWNLOAD_MANAGER"/>
    <!--获取网络状态权限-->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <!--APK安装权限-->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```

#### 清单文件配置
你需要在你清单文件中的**Application**节点下添加如下配置：
```html
<!--Android7.0一上安装Apk所需要的文件提供者-->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileProvider" 
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/apk_updater_file_paths" />
</provider>

<!--版本更新服务-->
<service android:name="com.kelin.apkUpdater.DownloadService" />
```
provider标签中```android:authorities```的值可以自定义，需要在初始化```ApkUpdater.init(context,fileProvider)```方法中传入该值。

在Android7.0以上的设备如果不能正常下载你可能还需要在清单文件的Application节点下增加networkSecurityConfig配置。例如：
```html
<application
        android:name=".App"
        android:networkSecurityConfig="@xml/network_security_config">

        <!--此处省略了你的Activity、Service等四大组件-->
        
</application>
```
其中 network_security_config 文件需要定义在res的xml文件夹下，它的代码如下:
```html
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```
#### 初始化
你需要在Application的onCreate方法中调用``` ApkUpdater.init(context, fileProvider) ```初始化ApkUpdater。例如:
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApkUpdater.init(this, "$packageName.fileProvider")
    }
}
```
**注意:** 别忘记在清单文件中使用你的Application:
```html
<application
        android:name=".App"  //这里是你自定义的Application。
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        
        <!--此处省略了你的Activity、Service等四大组件-->
        
</application>
```

#### 获取更新信息
首先利用你项目的网络访问能力从服务器端获取更新信息并转换为**javaBean**对象，然后让这个对象实现**UpdateInfo**接口。下面是这个接口中所有方法：
```kotlin
interface UpdateInfo {
    /**
     * 网络上的版本号。
     */
    val versionCode: Int

    /**
     * 网络上的版本名称。
     */
    val versionName: String?

    /**
     * 最新版本 apk 的下载链接。
     */
    val downLoadsUrl: String?

    /**
     * 是否强制更新。`true` 表示强制更新, `false` 则相反。
     */
    val isForceUpdate: Boolean

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 [.isForceUpdate]
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     *
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 [.isForceUpdate] 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    val forceUpdateVersionCodes: IntArray?

    /**
     * 更新标题，例如"更新以下内容"，用于显示在弹窗中。
     */
    val updateMessageTitle: CharSequence?

    /**
     * 获取更新的内容。就是你本次更新了那些东西可以在这里返回，用于显示在弹窗中。
     */
    val updateMessage: CharSequence?

    /**
     * 服务端可提供的文件签名类型，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
     */
    val signatureType: SignatureType?

    /**
     * 服务端提供的文件签名，目前只支持MD5或SHA1。用于Apk完整性校验，防止下载的过程中丢包或Apk遭到恶意串改。
     */
    val signature: String?
}
```
#### 构建**Updater**对象
这个对象是使用构造者模式创建的，可以配置Api提供的Dialog中的Icon、Title、Message以及NotifyCation的Title和Desc。

|方法名|说明|
|-----|------|
|fun setCallback(callback: IUpdateCallback?): Builder|设置监听对象。|
|fun setDialogGenerator(generator: (updater: ApkUpdater) -> ApkUpdateDialog): Builder|使用自定义弹窗。|
|fun create(): ApkUpdater|完成**Updater**对象的构建。|
#### 自定义Dialog
```kotlin
ApkUpdater.Builder()
        .setDialogGenerator {
            MyUpdateDialog(it)
        }.create()
```
MyUpdateDialog 是ApkUpdateDialog接口的实现类。
#### 检查更新
检查更新的代码如下：
```kotlin
apkUpdater.check(updateInfo)
```
#### 安装APK
安装是不许要你关心的，下载完成后会自动进入安装页面。除非你禁用了自动安装，或是想安装一个现有的Apk。如果是这样的话你可以使用**UpdateHelper**的```fun installApk(context: Context, apkFile: File?): Boolean```方法。

#### 其他
该项目中提供了两个工具类：UpdateHelper 和 NetWorkStateUtil。

* * *
### License
```
Copyright 2016 kelin410@163.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
