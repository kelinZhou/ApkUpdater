package com.kelin.updatemanagerdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.SignatureType
import com.kelin.apkUpdater.UpdateInfoImpl
import com.kelin.apkUpdater.UpdateType
import com.kelin.apkUpdater.callback.IUpdateCallback

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val apkUpdater: ApkUpdater by lazy {
        ApkUpdater.Builder()
                .setCallback(ApkCompleteUpdateCallback())
                .create()
    }

    private val updateInfo by lazy {
        UpdateInfoImpl(
                "https://paidian-static-sit.oss-cn-shanghai.aliyuncs.com/0001/appversion/2024/07/d7b63271-2d5f-469f-885a-09ae100aa152.apk", //安装包下载地址
                632, //网络上的版本号，用于判断是否可以更新(是否大于本地版本号)。
                "v6.3.2", //版本名称，用于显示在弹窗中，以告知用户将要更到哪个版本。
                UpdateType.UPDATE_WEAK,  //更新类型
                "更新内容如下：",  //升级弹窗的标题。
                "1.修复了极端情况下可能导致下单失败的bug。\n2.增加了许多新的玩法，并且增加了app的稳定性。 \n3.这是测试内容，其实什么都没有更新。", //升级弹窗的消息内容，用于告知用户本次更新的内容。
                SignatureType.MD5, //安装包完整性校验开启，并使用MD5进行校验，如果不想开启，传null。(目前只支持MD5和SHA1)
                ""  //完成性校验的具体值，返回空或null则不会进行校验。
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
            R.id.btn_check_update -> if (!apkUpdater.check(updateInfo, true)) {
                Toast.makeText(this, "正在更新中，请稍后", Toast.LENGTH_SHORT).show()
            }

            R.id.tv_content -> startActivity(Intent(this, TwoActivity::class.java))
        }
    }

    private inner class ApkCompleteUpdateCallback : IUpdateCallback {

        override fun onSuccess(isAutoCheck: Boolean, haveNewVersion: Boolean, curVersionName: String, updateType: UpdateType) {
            if (!isAutoCheck && !haveNewVersion) {
                Toast.makeText(applicationContext, "$curVersionName 已是最新版本！", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFiled(isAutoCheck: Boolean, isCanceled: Boolean, haveNewVersion: Boolean, curVersionName: String, checkMD5failedCount: Int, updateType: UpdateType) {
            if (isCanceled) {
                if (!isAutoCheck) {
                    Toast.makeText(applicationContext, "更新被取消！$curVersionName", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (updateType == UpdateType.UPDATE_FORCE) {
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