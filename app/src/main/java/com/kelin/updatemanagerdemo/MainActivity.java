package com.kelin.updatemanagerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.kelin.apkUpdater.ActivityStackManager;
import com.kelin.apkUpdater.ApkUpdater;
import com.kelin.apkUpdater.callback.IUpdateCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private ApkUpdater mUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_content).setOnClickListener(this);
        findViewById(R.id.btn_check_update).setOnClickListener(this);

        mUpdater = new ApkUpdater.Builder()
                .setCallback(new ApkCompleteUpdateCallback())
                .setCheckWiFiState(true)
                .builder();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_check_update:
                mUpdater.check(new UpdateModel(), false);
                break;
            case R.id.tv_content:
                startActivity(new Intent(this, TwoActivity.class));
                break;
        }
    }

    private static class ApkCompleteUpdateCallback implements IUpdateCallback {
        private static final String TAG = "ApkCompleteUpdateCallback";

        @Override
        public void onSilentDownload(@NonNull ApkUpdater apkUpdater) {
            Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), "静默下载", Toast.LENGTH_SHORT).show();
            //apkUpdater.removeCallback();  //如果在用户点击静默安装之后不希望在监听后续的回调，则可以调用该方法。
        }

        @Override
        public void onSuccess(boolean isAutoCheck, boolean haveNewVersion, String curVersionName, boolean isForceUpdate) {
            if (!isAutoCheck && !haveNewVersion) {
                Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), curVersionName + " 已是最新版本！", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFiled(boolean isAutoCheck, boolean isCanceled, boolean haveNewVersion, String curVersionName, int checkMD5failedCount, boolean isForceUpdate) {
            if (isCanceled) {
                if (!isAutoCheck) {
                    Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), "更新被取消！" + curVersionName, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (isForceUpdate) {
                    Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), "您必须升级后才能继续使用！" + curVersionName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), "下载失败！" + curVersionName, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onCompleted() {
            Toast.makeText(ActivityStackManager.getInstance().getApplicationContext(), "完成", Toast.LENGTH_SHORT).show();
        }
    }
}
