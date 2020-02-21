package de.keawe.umbrellaclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static de.keawe.umbrellaclient.gui.SettingsActivity.CREDENTIALS;
import static de.keawe.umbrellaclient.gui.SettingsActivity.INTERVAL_MINUTES;

public class BootHandler extends BroadcastReceiver {

    private static final String TAG = "BootHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_BOOT_COMPLETED)){
            SharedPreferences prefs = context.getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
            int minutes = prefs.getInt(INTERVAL_MINUTES,0);
            if (minutes > 0) context.startService(new Intent(context, CheckService.class));
        }
    }
}
