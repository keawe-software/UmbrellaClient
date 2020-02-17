package de.keawe.umbrellaclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MessageChecker extends Worker implements LoginListener {
    public static final String TAG = "MessageChecker";
    private final Context context;

    public MessageChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG,"doWork called");

        SharedPreferences credentials = context.getSharedPreferences(MainActivity.CREDENTIALS, Context.MODE_PRIVATE);
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
            for (int i = 0; i<arr.length(); i++) notifyMessage(arr.getJSONObject(i));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void notifyMessage(JSONObject msg) throws JSONException {
        Log.d(TAG,"new message");
        int id = msg.getInt("message_id");
        long time = msg.getLong("timestamp")*1000;
        Log.d(TAG,"id: "+id);

        NotificationManager man = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder nb;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("umbrella","Umbrella", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("This is Channel 1");
            man.createNotificationChannel(channel);
            nb = new NotificationCompat.Builder(context,channel.getId());
        } else {
            nb = new NotificationCompat.Builder(context);
        }

        nb.setSmallIcon(R.drawable.umbrella100px);
        nb.setContentTitle(msg.getString("subject"));
        nb.setContentText(context.getString(R.string.tap_to_display));
        nb.setWhen(time);
        nb.setAutoCancel(true);

        man.notify(id,nb.build());
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
        Log.d(TAG,"token: "+login.token());
        login.get("/user/json?messages=WAITING",this);
    }
}
