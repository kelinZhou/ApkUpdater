package com.kelin.updatemanagerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.kelin.apkUpdater.Updater;
import com.kelin.apkUpdater.callback.CompleteUpdateCallback;
import com.kelin.apkUpdater.callback.IUpdateCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Updater mUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv_content).setOnClickListener(this);
        findViewById(R.id.btn_check_update).setOnClickListener(this);

        mUpdater = new Updater.Builder()
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

    private class ApkCompleteUpdateCallback implements IUpdateCallback {
        private static final String TAG = "ApkCompleteUpdateCallback";

        @Override
        public void onSilentDownload() {
            Toast.makeText(getApplicationContext(), "静默下载", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSuccess(boolean isAutoCheck, boolean haveNewVersion, String curVersionName, boolean isForceUpdate) {
            if (!isAutoCheck && !haveNewVersion) {
                Toast.makeText(getApplicationContext(), curVersionName + " 已是最新版本！", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFiled(boolean isAutoCheck, boolean isCanceled, boolean haveNewVersion, String curVersionName, int checkMD5failedCount, boolean isForceUpdate) {
            if (isCanceled) {
                if (!isAutoCheck) {
                    Toast.makeText(getApplicationContext(), "更新被取消！" + curVersionName, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (isForceUpdate) {
                    Toast.makeText(getApplicationContext(), "您必须升级后才能继续使用！" + curVersionName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "下载失败！" + curVersionName, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onCompleted() {
            Toast.makeText(getApplicationContext(), "完成", Toast.LENGTH_SHORT).show();
        }
    }
}
