package com.example.ndc;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class Utils extends Application {
    public static CommunicationUtils CommUtils;
    @SuppressLint("StaticFieldLeak")
    public static Context context;

    public Notification getNotification(String ChannelID, String ChannelName, String Content) {

        Intent intent = new Intent(Utils.context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(ChannelID, ChannelName, NotificationManager.IMPORTANCE_HIGH);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ChannelID);
        return notificationBuilder
                .setOngoing(true)
                .setContentTitle("Network Collector")
                .setContentText(Content)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }


    @Override
    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        try {
            CommUtils = new CommunicationUtils();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        try {
            CommUtils.TerminateServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CommunicationUtils getCommUtils(){
        return CommUtils;
    }
//    public static LocationUtils getLocUtils(){return LocUtils;}
}
