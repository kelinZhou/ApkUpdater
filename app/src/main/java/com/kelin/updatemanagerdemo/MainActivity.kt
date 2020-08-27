package com.kelin.updatemanagerdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.callback.IUpdateCallback

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val mUpdater by lazy(
            ApkUpdater.Builder().setCallback(ApkCompleteUpdateCallback())::builder
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.tv_content).setOnClickListener(this)
        findViewById<View>(R.id.btn_check_update).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_check_update -> mUpdater.check(UpdateModel(), false)
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