package com.example.gps_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ExplainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explain);

        Button backButton = findViewById(R.id.explain_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        textView = findViewById(R.id.explain_text);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onBackPressed() {
        finish();
    } // 뒤로가기 버튼 클릭했을 때
}