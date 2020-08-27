package com.kelin.updatemanagerdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kelin.updatemanagerdemo.TwoActivity

/**
 * **创建人:** kelin
 *
 *
 * **创建时间:** 2018/9/25  上午10:59
 *
 *
 * **版本:** v 1.0.0
 */
class TwoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two)
        findViewById<View>(R.id.btnStart).setOnClickListener { startActivity(Intent(this@TwoActivity, TwoActivity::class.java)) }
    }
}