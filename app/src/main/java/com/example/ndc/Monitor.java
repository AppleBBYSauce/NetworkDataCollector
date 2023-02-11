package com.example.ndc;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;



public class Monitor extends AppCompatActivity {


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        TextView monitor = findViewById(R.id.deviceInfo);
        DeviceInfo info = new DeviceInfo();
        monitor.setText(info.PatternInfoCompile(this) +"\n" +View.Device_Manifests + "\n" + View.Activity_Device);
    }
}

