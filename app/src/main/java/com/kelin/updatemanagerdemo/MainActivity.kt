package com.kelin.updatemanagerdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.SignatureType
import com.kelin.apkUpdater.SimpleUpdateInfo
import com.kelin.apkUpdater.callback.IUpdateCallback
import com.kelin.apkUpdater.dialog.ApkUpdateDialog

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val apkUpdater: ApkUpdater by lazy {
        ApkUpdater.Builder()
                .setDialogGenerator {
                    MyUpdateDialog(it)
                }.create()
    }

    private val updateInfo by lazy {
        SimpleUpdateInfo(
                "http://test-cloud-yxholding-com.oss-cn-shanghai.aliyuncs.com/yx-logistics/file/file/20200703/1593709201374.apk",
                131,
                "v1.3.1",
                false,
                "更新内容如下：",
                "1.修复了极端情况下可能导致下单失败的bug。\n2.增加了许多新的玩法，并且增加了app的稳定性。 \n3.这是测试内容，其实什么都没有更新。",
                SignatureType.MD5,
                ""
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.tv_content).setOnClickListener(this)
        findViewById<View>(R.id.btn_check_update).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_check_update -> apkUpdater.check(updateInfo)
            R.id.tv_content -> startActivity(Intent(this, TwoActivity::class.java))
        }
    }

    private inner class ApkCompleteUpdateCallback : IUpdateCallback {

        override fun onSuccess(isAutoCheck: Boolean, haveNewVersion: Boolean, curVersionName: String, isForceUpdate: Boolean) {
            if (!isAutoCheck && !haveNewVersion) {
                Toast.makeText(applicationContext, "$curVersionName 已是最新版本！", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFiled(isAutoCheck: Boolean, isCanceled: Boolean, haveNewVersion: Boolean, curVersionName: String, checkMD5failedCount: Int, isForceUpdate: Boolean) {
            if (isCanceled) {
                if (!isAutoCheck) {
                    Toast.makeText(applicationContext, "更新被取消！$curVersionName", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isForceUpdate) {
                    Toast.makeText(applicationContext, "您必须升级后才能继续使用！$curVersionName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "下载失败！$curVersionName", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCompleted() {
            Toast.makeText(applicationContext, "结束", Toast.LENGTH_SHORT).show()
        }
    }
}

class MyUpdateDialog(apkUpdater: ApkUpdater) : ApkUpdateDialog {
    override fun show(activity: Activity, version: String?, messageTitle: CharSequence?, message: CharSequence?, isForce: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProgress(total: Long, current: Long, percentage: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNetworkError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dismiss() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}