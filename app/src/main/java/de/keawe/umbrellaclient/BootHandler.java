package de.keawe.umbrellaclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static de.keawe.umbrellaclient.gui.SettingsActivity.CREDENTIALS;
import static de.keawe.umbrellaclient.gui.SettingsActivity.ENABLE_SERVICE;

public class BootHandler extends BroadcastReceiver {

    private static final String TAG = "BootHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_BOOT_COMPLETED)){
            SharedPreferences prefs = context.getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
            if (prefs.getBoolean(ENABLE_SERVICE,false)){
                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(MessageChecker.class,15, TimeUnit.MINUTES).build();
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(MessageChecker.TAG, ExistingPeriodicWorkPolicy.REPLACE,workRequest);
            }
        }
    }
}
