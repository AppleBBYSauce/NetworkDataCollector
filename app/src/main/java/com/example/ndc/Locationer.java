package com.example.ndc;


import android.app.Notification;
import android.content.Context;


import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

public class Locationer {
    public  LocationClient mLocationClient = null;
    public  LocationUtils mListener = null;

    public void initLocation(Context context) throws Exception {

        LocationClient.setAgreePrivacy(true);
        mLocationClient = new LocationClient(context);
        mListener = new LocationUtils();

        LocationClientOption option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOnceLocation(true);
        option.setCoorType("bd09ll");
        option.setNeedDeviceDirect(true);
        option.setOpenGnss(true);

        mLocationClient.registerLocationListener(mListener);
        mLocationClient.setLocOption(option);
    }
}
