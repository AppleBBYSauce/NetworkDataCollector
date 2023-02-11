package com.example.ndc;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.content.UnusedAppRestrictionsBackportService;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;


public class LocationUtils extends BDAbstractLocationListener {

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        Log.e("Location", "Call");
        StringBuilder Location = new StringBuilder();
        Location.append(bdLocation.getLongitude()).append('\t');
        Location.append(bdLocation.getLatitude()).append('\t');
        Location.append(bdLocation.getLocType()).append('\t');
        Location.append(bdLocation.getSpeed());
        Utils.getCommUtils().Location = Location;
    }
}
