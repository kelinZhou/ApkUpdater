package com.kelin.updatemanagerdemo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

/**
 * **描述:** ${TODO}
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2018/9/25  上午10:59
 * <p>
 * **版本:** v 1.0.0
 */
public class TwoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two);
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TwoActivity.this, TwoActivity.class));
            }
        });
    }
}