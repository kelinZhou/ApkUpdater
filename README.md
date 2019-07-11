# ApkUpdater
基于DownLoadManager实现安装包更新，安装包缓存，支持MD5校验，支持断点续传，自定义UI，提供了默认UI。


## 演示
按照惯例还是先上图吧。从图片中你可以看出apk是做了缓存的，也就是下载完成后如果没有安装下次再次检查更新时如果发现服务端的版本和缓存的版本一致且MD5值一致则会跳过下载，直接安装。

![demonstrate](materials/gif_apk_updater.gif)

## 下载
#### 第一步：添加 JitPack 仓库到你项目根目录的 gradle 文件中。
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
#### 第二步：添加这个依赖。
```
dependencies {
    implementation 'com.github.kelinZhou:ApkUpdater:2.2.1'
}
```

## 使用
#### 添加权限
你需要在你的清单文件中添加以下权限：
```
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
```
<!--Android7.0一上安装Apk所需要的文件提供者-->
<provider
    android:name="android.support.v4.content.FileProvider"
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
#### 初始化
你需要在你的Application的onCreate生命周期方法中调用``` ApkUpdater.init(this) ```，如下:
```
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //……省略N行代码
        ApkUpdater.init(this);
    }
}
```
**注意:** 别忘记在清单文件中使用你的Application:
```
<application
        android:name=".MyApplication"  //这里是你自定义的Application。
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
```java
public interface UpdateInfo {
    /**
     * 获取网络上的版本号。
     * @return 返回当前对象的版本号字段的值。
     */
    int getVersionCode();

    /**
     * 获取网络上的版本名称。
     * @return 返回当前对象的版本名称字段的值。
     */
    String getVersionName();

    /**
     * 获取最新版本的下载链接。
     * @return 返回当前对象的下载链接字段的值。
     */
    String getDownLoadsUrl();

    /**
     * 是否强制更新。
     * @return <code color="blue">true</code> 表示强制更新, <code color="blue">false</code> 则相反。
     */
    boolean isForceUpdate();

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 {@link #isForceUpdate()}
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 {@link #isForceUpdate()} 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    @Nullable int[] getForceUpdateVersionCodes();

    /**
     * 获取Apk文件名(例如 xxx.apk 或 xxx)。后缀名不是必须的。
     * 可以返回null，如果返回null则默认使用日期作为文件名。
     */
    @Nullable String getApkName();

    /**
     * 获取更新的内容。就是你本次更新了那些东西可以在这里返回，这里返回的内容会现在是Dialog的消息中，如果你没有禁用Dialog的话。
     * @return 返回你本次更新的内容。
     */
    CharSequence getUpdateMessage();

    /**
     * 获取网络上最新安装包的MD5值，用户校验下载后的安装包是否完整以及是否修改等。
     * @return 如果你希望使用MD5校验则返回网络上最近安装包的MD5值，如果返回null这表示不进行MD5校验。
     */
    @Nullable String getMd5();
}
```
#### 构建**Updater**对象
这个对象是使用构造者模式创建的，可以配置Api提供的Dialog中的Icon、Title、Message以及NotifyCation的Title和Desc。

|方法名|说明|
|-----|------|
|public Builder setCallback(IUpdateCallback callback)|设置监听对象。|
|public Builder setCheckDialogTitle(CharSequence title)|配置检查更新时对话框的标题。|
|public Builder setDownloadDialogTitle(CharSequence title)|配置下载更新时对话框的标题。|
|public Builder setDownloadDialogMessage(String message)|配置下载更新时对话框的消息。|
|public Builder setNotifyTitle(CharSequence title)|设置通知栏的标题。（强制更新时是没有通知栏通知的。）|
|public Builder setNotifyDescription(CharSequence description)|设置通知栏的描述。（强制更新时是没有通知栏通知的。）|
|public Builder setNoDialog(@NonNull DialogEventCallback callback)|如果你希望自己创建对话框，而不使用默认提供的对话框，可以调用该方法将默认的对话框关闭。如果你关闭了默认的对话框的话就必须自己实现UI交互，并且在用户更新提示做出反应的时候调用 ```updater.setCheckHandlerResult(boolean)``` 方法。实现UI交互的时机都在DialogEventCallback的回调中。|
|public Builder setCheckWiFiState(boolean check)|设置不检查WiFi状态，默认是检查WiFi状态的，也就是说如果在下载更新的时候如果没有链接WiFi的话默认是会提示用户的。但是如果你不希望给予提示，就可以通过调用此方法，禁用WiFi检查。|
|public Updater builder()|完成**Updater**对象的构建。|
#### 自定义Dialog
通过ApkUpdater.Builder.setNoDialog(@NonNull DialogEventCallback callback)方法设置回调后只需要覆盖回调中的抽象方法即可，下面是这个接口中的所有方法：
```java
public interface DialogEventCallback {

    /**
     * 当需要显示检查更新提示对话框的时候调用。你需要在这里进行检查更新提示对话框的显示。
     * 在用户做出相应的操作后，你应当调用{@link ApkUpdater#setCheckHandlerResult(boolean)}方法进行下一步的操作。
     *
     * @param updater {@link ApkUpdater}对象。
     * @param isForce 是否是强制更新。
     * @see ApkUpdater#setCheckHandlerResult(boolean)
     */
    void onShowCheckHintDialog(ApkUpdater updater, @NonNull UpdateInfo updateInfo, boolean isForce);

    /**
     * 当需要加载下载进度对话框的时候调用，你需要在这做显示下载进度对话框的操作。
     * <p>也有可能这个方法会调用不止一次，如果你在构建 {@link ApkUpdater.Builder} 的时候没有调用
     * {@link ApkUpdater.Builder#setCheckWiFiState(boolean)}方法改变检测网络状态的话默认是会检测WIFI状态的。当WIFI状态改变的时候
     * 有可能会再次调用该方法，所以这里你要做好相应的判断，以避免Dialog会显示多次。
     * <p>这里只是单纯的做显示对话的操作，无需做其他任何处理，进度条的更新需要在{@link #onProgress(ApkUpdater, long, long, int)}方法中处理，
     * 您需要覆盖{@link #onProgress(ApkUpdater, long, long, int)}方法。
     *
     * @param isForce 是否是强制更新。
     * @see ApkUpdater.Builder#setCheckWiFiState(boolean)
     * @see #onProgress(ApkUpdater, long, long, int)
     */
    void onShowProgressDialog(ApkUpdater updater,boolean isForce);

    /**
     * 下载进度更新的时候调用。如果您是自定义的UI交互的话您需要覆盖此方法，并在此方法中做更新进度的操作。
     * 您还需要在进度完成后进行销毁dialog的操作。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    void onProgress(ApkUpdater updater,long total, long current, int percentage);
}
```
#### 检查更新
检查更新的代码如下：
```
private void checkUpdate(UpdateModel updateModel) {
    new Updater.Builder(MainActivity.this).builder().check(updateModel);
}
```
Updater的check方法除了```public void check(UpdateInfo updateInfo)```还有以下两个重载:

1、 
```
public void check(@NonNull UpdateInfo updateInfo, boolean isAutoCheck)
```
isAutoCheck参数是指是不是自动更新，因为手动更新时如果没有新的版本你总要告诉一下用户，而这个参数将会在回调方法中传递给你。

2、
```
public void check(@NonNull UpdateInfo updateInfo, boolean isAutoCheck)
```
autoInstall参数是指要不要自动安装，如果你只是想下载一个apk文件而不希望立即安装的话则可以将该参数置为false。

#### 开始下载
如果你调用了检查更新的方法这一步是**不需要你手动调用的**，但是如果你只是单纯的想利用API做下载Apk的动作就可以通过此方法执行，代码如下：
```
new Updater.Builder(MainActivity.this).builder().download(updateModel);
```
与检查更新**check()**方法一样，download方法也提供了重载，所有方法如下：

|方法名|说明|
|-----|-----|
|public void download(@NonNull UpdateInfo updateInfo)|开始下载。updateInfo：更新信息对象。|
|public void download(@NonNull UpdateInfo updateInfo, boolean autoInstall)|updateInfo：更新信息对象。autoInstall：是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。|
|public void download(@NonNull UpdateInfo updateInfo, CharSequence notifyCationTitle, CharSequence notifyCationDesc, boolean autoInstall)|updateInfo：更新信息对象。notifyCationTitle：下载过程中通知栏的标题。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。notifyCationDesc：下载过程中通知栏的描述。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。autoInstall：是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。|
#### 安装APK
安装是不许要你关心的，下载完成后会自动进入安装页面。除非你禁用了自动安装，或是想安装一个现有的Apk。如果是这样的话你可以使用**UpdateHelper**的```public static void installApk(Context context, File apkFile)```方法。

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
