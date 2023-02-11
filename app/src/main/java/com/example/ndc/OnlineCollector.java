package com.example.ndc;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class OnlineCollector extends IntentService {

    public Locationer LE = new Locationer();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(3);


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

    public OnlineCollector() {
        super("online");
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        Runnable broadcast = new Runnable() {
            @Override
            public void run() {
                Utils.getCommUtils().BroadCast_();
            }
        };
        Runnable online_collector = new Runnable() {
            @Override
            public void run() {
                Utils.getCommUtils().view.OnlineCollector_();
            }
        };

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE|PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        wakeLock.acquire();
        startForeground(18741872, getNotification("OfflineCollector", "OfflineCollector", "OfflineCollector"));

        service.scheduleAtFixedRate(broadcast, 1, 5, TimeUnit.SECONDS);
        service.scheduleAtFixedRate(online_collector, 1, 5, TimeUnit.SECONDS);

        try {
            LE.initLocation(Utils.context);
            Log.e("Location", "start Location service");
        } catch (Exception e) {
            Log.e("Location", "start Location fail");
            e.printStackTrace();
        }
        LE.mLocationClient.start();

        Utils.getCommUtils().RunUDPServer();
        Log.e("Pytorch", "start all server");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        service.shutdownNow();
    }
}