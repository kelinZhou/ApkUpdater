package com.kelin.updatemanagerdemo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.kelin.apkUpdater.Updater;
import com.kelin.apkUpdater.callback.UpdateCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Updater mUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_check_update).setOnClickListener(this);

        mUpdater = new Updater.Builder(this)
                .setCallback(new ApkUpdateCallback())
                .setCheckWiFiState(true)
                .builder();
    }

    @Override
    public void onClick(View v) {
        mUpdater.check(new UpdateModel());
    }

    private class ApkUpdateCallback extends UpdateCallback {
        private static final String TAG = "ApkUpdateCallback";
        /**
         * 开始下载，在开始执行下载的时候调用。如果你在构建 {@link Updater.Builder} 的时候调用了
         * {@link Updater.Builder#setNoDialog()} 方法关闭了默认对话框的话，那么你需要在这做显示下载进度操作。
         */
        @Override
        public void onStartLoad() {
            Log.i(TAG, "onStartLoad: 开始下载");
        }

        /**
         * 下载进度更新。
         *
         * @param total      文件总大小(字节)。
         * @param current    当前的进度(字节)。
         * @param percentage 当前下载进度的百分比。
         */
        @Override
        public void onProgress(long total, long current, int percentage) {
            Log.i(TAG, "onProgress: 下载进度更新：total=" + total + "|current=" + current + "|percentage=" + percentage);
        }

        /**
         * 下载完成。
         *
         * @param downUri 已经下载好的APK存储地址。
         * @param isCache 是否是缓存，如果改参数为true说明本次并没有真正的执行下载任务，因为上一次用户下载完毕后并没有进行
         */
        @Override
        public void onLoadSuccess(Uri downUri, boolean isCache) {
            Log.i(TAG, "onLoadSuccess: 下载成功：downUri=" + downUri + "|是否是缓存？" + (isCache ? "是" : "否"));
        }

        /**
         * 当下载失败的时候调用。
         */
        @Override
        public void onLoadFailed() {
            Log.i(TAG, "onLoadFailed: 下载失败。");
        }

        /**
         * 下载暂停。
         */
        @Override
        public void onLoadPaused() {
            Log.i(TAG, "onLoadPaused: 下载被暂停。");
        }

        /**
         * 等待下载。
         */
        @Override
        public void onLoadPending() {
            Log.i(TAG, "onLoadPending: 等待下载。");
        }

        /**
         * 检查更新被取消。如果当前设备无网络可用则会执行该方法。
         */
        @Override
        public void onCheckCancelled() {
            Log.i(TAG, "onCheckCancelled: 检查更新被取消。");
        }

        /**
         * 当下载被取消后调用。即表明用户不想进行本次更新，强制更新一般情况下是不能取消的，除非你设置了需要检查WIFI而WIFI又没有链接。
         */
        @Override
        public void onLoadCancelled() {
            Log.i(TAG, "onLoadCancelled: 下载被取消。");
        }

        /**
         * 当任务完毕后被调用。无论任务成功还是失败，也无论是否需要更新。如果在检查更新阶段发现没有新的版本则会直接执行
         * 该方法，如果检查更新失败也会执行该方法，如果检测到了新的版本的话，那么这个方法就不会再检查更新阶段调用，一直
         * 等到下载完成或下载失败之后才会被执行。
         *
         * @param haveNewVersion 是否有新的版本。
         *                       <code color="blue">true</code>表示有新的版本,
         *                       <code color="blue">false</code>则表示没有新的版本。
         * @param curVersionName 当前app的版本名称。
         */
        @Override
        public void onCompleted(boolean haveNewVersion, String curVersionName) {
            Log.i(TAG, "onCompleted: 完成。");
            if (!haveNewVersion) {
                Toast.makeText(getApplicationContext(), "当前已是最新版本：" + curVersionName, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
