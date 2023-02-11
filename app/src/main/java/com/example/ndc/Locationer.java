package com.example.ndc;

import android.content.Context;
import android.util.Log;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

import java.util.Objects;

public class Locationer {
    public LocationClient mLocationClient = null;
    public LocationUtils mListener = null;


    public void initLocation(Context context) throws Exception {

        LocationClient.setAgreePrivacy(true);
        mLocationClient = new LocationClient(context);
        mListener = new LocationUtils();

        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(10000);
        option.setCoorType("BD09ll");
        option.setOpenGnss(true);
        option.setIsEnableBeidouMode(true);

        mLocationClient.registerLocationListener(mListener);
        mLocationClient.setLocOption(option);
    }
    public void stopListener(){
        mLocationClient.stop();
    }
}
