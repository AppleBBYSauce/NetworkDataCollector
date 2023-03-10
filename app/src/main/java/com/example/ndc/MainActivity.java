package com.example.ndc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.google.common.geometry.S2LatLng;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Button mBtnText;
    private Button mBtnStartCollect;
    private Button mBtnEndCollect;
    private Button mBtnForecast;
    private EditText ETPeriod;

    // permission manifest
    String[] permission = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };



    // apply permission
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int phone_status = ContextCompat.checkSelfPermission(this, permission[0]);
            int wifi_status = ContextCompat.checkSelfPermission(this, permission[1]);
            int fine_location = ContextCompat.checkSelfPermission(this, permission[2]);
            int write_external_storage = ContextCompat.checkSelfPermission(this, permission[3]);
            if (phone_status != PackageManager.PERMISSION_GRANTED ||
                    wifi_status != PackageManager.PERMISSION_GRANTED ||
                    fine_location != PackageManager.PERMISSION_GRANTED ||
                    write_external_storage != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, permission, 321);
        }
    }


    @SuppressLint({"MissingInflatedId", "CutPasteId", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        setContentView(R.layout.activity_main);
        mBtnText = findViewById(R.id.monitor);
        mBtnEndCollect = findViewById(R.id.Tc);
        mBtnStartCollect = findViewById(R.id.Sc);
        mBtnForecast = findViewById(R.id.forecast);
        ETPeriod = findViewById(R.id.period);
        setListener();
    }

    protected void setListener() {
        OnClick onclick = new OnClick();
        mBtnText.setOnClickListener(onclick);
        mBtnStartCollect.setOnClickListener(onclick);
        mBtnEndCollect.setOnClickListener(onclick);
        mBtnForecast.setOnClickListener(onclick);
    }

    private class OnClick implements View.OnClickListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View V) {
            Intent intent = null;
            switch (V.getId()) {
                case R.id.monitor:
                    intent = new Intent(MainActivity.this, Monitor.class);
                    startActivity(intent);
                    break;
                case R.id.Sc:
                    String period = ETPeriod.getText().toString();
                    if (Objects.equals("", period)) period = "60";
                    Utils.getCommUtils().period =  Integer.valueOf(period);
                    intent = new Intent(MainActivity.this, OfflineCollector.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);

                    }


                    break;
                case R.id.forecast:
                    intent = new Intent(MainActivity.this, NetworkForecast.class);
                    Utils.getCommUtils().period =  Integer.valueOf(ETPeriod.getText().toString());
                    startActivity(intent);
                    break;
                case R.id.Tc:
                    Destroy();
                    break;
            }
        }
    }

    public void Destroy() {
        try {
            Utils.getCommUtils().TerminateServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent1 = new Intent(MainActivity.this, OfflineCollector.class);
        Intent intent2 = new Intent(MainActivity.this, OnlineCollector.class);
        stopService(intent1);
        stopService(intent2);
    }
}


