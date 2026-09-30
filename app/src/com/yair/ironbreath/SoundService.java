package com.yair.ironbreath;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Constructor;

/**
 * Keeps the process alive (foreground) while soundscapes play so that sleep sounds keep going with
 * the screen off. Channel APIs are called reflectively because the app compiles against API 23.
 */
public class SoundService extends Service {
    private static final String CHANNEL = "iron_sound";
    private static final String ACTION_STOP = "com.yair.ironbreath.STOP";
    private static boolean started;

    static void start(Context c) {
        if (started) return;
        started = true;
        try {
            c.startService(new Intent(c, SoundService.class));
        } catch (Exception e) {
            started = false;
        }
    }

    static void stop(Context c) {
        if (!started) return;
        started = false;
        c.stopService(new Intent(c, SoundService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            SoundEngine.get(this).stopAll();
            stopForeground(true);
            stopSelf();
            started = false;
            return START_NOT_STICKY;
        }
        try {
            startForeground(7, build());
        } catch (Exception ignored) {
        }
        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    private Notification build() throws Exception {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> chCls = Class.forName("android.app.NotificationChannel");
            Constructor<?> cc = chCls.getConstructor(String.class, CharSequence.class, int.class);
            Object ch = cc.newInstance(CHANNEL, "נגן צלילים", 2 /* IMPORTANCE_LOW */);
            NotificationManager.class.getMethod("createNotificationChannel", chCls).invoke(nm, ch);
            b = Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
            b.setPriority(Notification.PRIORITY_LOW);
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | 0x04000000 /* FLAG_IMMUTABLE */);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, SoundService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | 0x04000000);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("נשימת ברזל · סאונד פעיל")
                .setContentText("הצלילים ממשיכים ברקע. הקש כדי לחזור.")
                .setColor(Ui.EMBER)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .addAction(0, "עצור", stop);
        return b.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        SoundEngine.get(this).stopAll();
        stopSelf();
        started = false;
    }

    @Override
    public void onDestroy() {
        started = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
