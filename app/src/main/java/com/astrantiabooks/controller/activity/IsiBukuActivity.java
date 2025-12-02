package com.astrantiabooks.controller;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.astrantiabooks.R;

public class IsiBukuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sinopsis);
        TextView tv = findViewById(R.id.tv_content_sinopsis);
        ImageButton back = findViewById(R.id.btn_back_sinopsis);

        String s = getIntent().getStringExtra("EXTRA_SYNOPSIS");
        tv.setText(s != null ? s : "Tidak ada sinopsis.");
        back.setOnClickListener(v -> finish());
    }
}