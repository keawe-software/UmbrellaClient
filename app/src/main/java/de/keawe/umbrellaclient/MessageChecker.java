package de.keawe.umbrellaclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;
import de.keawe.umbrellaclient.gui.MainActivity;
import de.keawe.umbrellaclient.gui.SettingsActivity;

public class MessageChecker extends Worker implements LoginListener {
    public static final String TAG = "MessageChecker";
    private final Context context;

    public MessageChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        new MessageDB(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG,"doWork called");

        SharedPreferences credentials = context.getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaLogin.URL,null);
        String user = credentials.getString(UmbrellaLogin.USER,null);
        String pass = credentials.getString(UmbrellaLogin.PASS,null);

        UmbrellaLogin login = new UmbrellaLogin(url,user,pass);
        login.doLogin(this);

        return Result.success();
    }

    @Override
    public void started() {
        Log.d(TAG,"Trying to login");
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public void onResponse(String response) {
        if (response.trim().startsWith("[{")) try {
            JSONArray arr = new JSONArray(response);
            for (int i = 0; i<arr.length(); i++) {
                Message msg = new Message(arr.getJSONObject(i)).store();
                if (msg != null){
                    notifyMessage(msg);
                }


            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void notifyMessage(Message msg) throws JSONException {
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
        nb.setContentIntent(PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
        nb.setAutoCancel(true);
        nb.setWhen(msg.time()*1000);
        nb.setOngoing(false);


        Notification notification = nb.build();


        man.notify((int) msg.id(),notification);
    }

    @Override
    public void onError() {
    }

    @Override
    public void onLoginFailed() {
        Log.e(TAG,"loginFailed");
    }

    @Override
    public void onTokenReceived(UmbrellaLogin login) {
        Message lastMessage = MessageDB.lastMessage();
        long id = lastMessage == null ? -1 : lastMessage.id();
        login.get("/user/json?messages="+id,this);
    }
}
