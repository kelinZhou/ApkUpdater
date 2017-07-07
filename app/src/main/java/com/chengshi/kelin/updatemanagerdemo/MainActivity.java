package com.chengshi.kelin.updatemanagerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.chengshi.apkUpdater.Updater;
import com.chengshi.apkUpdater.callback.UpdateCallback;
import com.chengshi.kelin.updatemanagerdemo.net.NetApiImpl;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Updater mUpdater;
    private boolean mIsForceUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_check_update).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mUpdater = new Updater.Builder(this).setCallback(new UpdateCallback() {
            @Override
            public void onLoadCancelled(boolean isForceUpdate) {
                if (isForceUpdate) {
                    finish();
                }
            }
        }).builder();

        checkUpdate();
    }

    private void checkUpdate() {
        NetApiImpl netApi = new NetApiImpl("http://update.useonline.cn/api/project/msg/Test_uhu");
        netApi.getApkUpdateInfo().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UpdateModel>() {

            @Override
            public void onCompleted() {}

            @Override
            public void onError(Throwable e) {
                Toast.makeText(getApplicationContext(), "检查更新失败。", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(UpdateModel updateModel) {
                mIsForceUpdate = updateModel.isForceUpdate();
                mUpdater.check(updateModel);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Updater.REQUEST_CODE_INSTALL_APK) {
            boolean installed = resultCode != RESULT_CANCELED;
            if (!installed && mIsForceUpdate) {
                finish();
            }
            Toast.makeText(getApplicationContext(), installed ? "安装成功！" : "应用未被安装！", Toast.LENGTH_SHORT).show();
        }
    }
}
