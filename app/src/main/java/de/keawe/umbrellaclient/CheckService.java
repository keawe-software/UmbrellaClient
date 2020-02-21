package de.keawe.umbrellaclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.gui.MainActivity;
import de.keawe.umbrellaclient.gui.SettingsActivity;

public class CheckService extends Service implements MessageHandler {
    private static final String TAG = "CheckService";
    private static Handler handler;
    private static Runnable scheduled = null;
    private static final int SECOND = 1000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        Log.d(TAG, "onBind()");
        return null;
    }

    @Override
    public void onCreate() {
  //      Log.d(TAG, "onCreate()");
        super.onCreate();
        handler = new Handler();
        schedule(checkForMessages(),5*SECOND); // for automatic start/stop
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand(..., flags = " + flags + ", startId = " + startId + ")");
        schedule(checkForMessages(),5*SECOND); // for manual start/stop
        return super.onStartCommand(intent, flags, startId);
    }

    private Runnable checkForMessages() {
        return new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences(SettingsActivity.CREDENTIALS, MODE_PRIVATE);
                int minutes = prefs.getInt(SettingsActivity.INTERVAL_MINUTES, 0);
                Log.d(TAG, "checkForMessages(version 16 @ " + (new Date().getTime()) + ") – minutes = "+minutes);
                new UmbrellaConnection(getBaseContext()).fetchMessages(CheckService.this);
                if (minutes > 0) {
                    schedule(this,minutes*60*SECOND);
                } else stop();
            }
        };
    }

    public static void schedule(Runnable r,int delayMilis){
        stop();
//        Log.d(TAG,"schedule(r, "+delayMilis+" ms)");
        scheduled = r;
        handler.postDelayed(scheduled,delayMilis);
    }

    public static boolean running(){
        boolean result = scheduled != null;
//        Log.d(TAG,"running() => "+result);
        return result;
    }

    public static void stop(){
//        Log.d(TAG,"stop()");
        if (!running()) return;
        handler.removeCallbacks(scheduled);
        scheduled = null;
    }

    @Override
    public void newMessage(Message msg) {
        //Log.d(TAG,"newMessage("+msg+");");
        Context context = getBaseContext();
        NotificationManager man = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder nb;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("umbrella","Umbrella", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Umbrella Messages");

            man.createNotificationChannel(channel);
            nb = new NotificationCompat.Builder(context,channel.getId());
        } else {
            nb = new NotificationCompat.Builder(context);
        }

        nb.setSmallIcon(R.drawable.umbrella100px);
        nb.setContentTitle(msg.subject());
        nb.setContentText(context.getString(R.string.tap_to_display));
        nb.setContentIntent(PendingIntent.getActivity(context,0,new Intent(context, MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
        nb.setAutoCancel(true);
        nb.setWhen(msg.time()*1000);
        nb.setOngoing(false);


        Notification notification = nb.build();


        man.notify((int) msg.id(),notification);
    }

    @Override
    public void gotNewMessages(int count) {

    }

    @Override
    public Context context() {
        return this;
    }
}
