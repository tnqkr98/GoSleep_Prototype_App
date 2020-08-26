package com.example.gosleep;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class GoSleepService extends Service {
    public GoSleepService() {//Log.d("dddd","Service: construct");
    }

    @Override
    public void onCreate() {
       // Log.d("dddd","Service: onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Intent cIntent = new Intent(this, GoSleepActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, cIntent, 0);

        //Log.d("dddd","Service: onStartCommand");
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26){
            String CHANNEL_ID = "channel_id";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,"서비스 앱",NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        }
        else
            builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("gosleep").setContentText("현재 GoSleep 기기와 앱이 연동 상태입니다.");
                //.setContentIntent(pendingIntent);
        startForeground(1,builder.build());

        return super.onStartCommand(intent, flags, startId);
        //return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Log.d("dddd","Service: onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
