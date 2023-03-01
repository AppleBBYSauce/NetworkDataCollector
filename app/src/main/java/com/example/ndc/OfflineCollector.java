package com.example.ndc;

import static android.os.SystemClock.sleep;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class OfflineCollector extends IntentService {


    public OfflineCollector() {
        super("OfflineCollector");
    }

    ScheduledExecutorService service = Executors.newScheduledThreadPool(3);
    public Locationer LE = new Locationer();


    public Notification getNotification(String ChannelID, String ChannelName, String Content) {
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
                .build();
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        wakeLock.acquire();
        Notification notif = getNotification("OfflineCollector", "OfflineCollector", "OfflineCollector");
        startForeground(18741872, notif);


        Runnable broadcast = new Runnable() {
            @Override
            public void run() {
                Utils.getCommUtils().BroadCast_();
            }
        };
        Runnable offline_collector = new Runnable() {
            @Override
            public void run() {
                Utils.getCommUtils().view.OfflineCollector_();
            }
        };
        Runnable status_recorder = new Runnable() {
            @Override
            public void run() {
                Utils.getCommUtils().view.StatusRecorder();
            }
        };

        if (Utils.getCommUtils().period <= 20) {
            service.scheduleAtFixedRate(broadcast, 1, Utils.getCommUtils().period, TimeUnit.SECONDS);
            service.scheduleAtFixedRate(offline_collector, 1, Utils.getCommUtils().period, TimeUnit.SECONDS);
        }

        service.scheduleAtFixedRate(status_recorder, 1, Utils.getCommUtils().period, TimeUnit.SECONDS);
        Utils.getCommUtils().RunUDPServer();
        while (true) {
            sleep(600);
        }

    }


}