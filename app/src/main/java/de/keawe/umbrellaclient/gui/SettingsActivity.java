package de.keawe.umbrellaclient.gui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import de.keawe.umbrellaclient.LoginListener;
import de.keawe.umbrellaclient.MessageChecker;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.UmbrellaLogin;
import de.keawe.umbrellaclient.db.MessageDB;


public class SettingsActivity extends AppCompatActivity implements LoginListener {
    private static final String TAG = "SettingsActivity";
    public static final String CREDENTIALS = "credentials";

    private Handler handler = new Handler();
    private Button btn;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btn = findViewById(R.id.test_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });

        final Button serviceButton = findViewById(R.id.service_btn);
        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceRunning()){
                    stopService();
                } else {
                    startService();
                }
            }
        });

        SharedPreferences prefs = getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
        if (prefs != null){
            EditText urlInput = findViewById(R.id.url);
            urlInput.setText(prefs.getString(UmbrellaLogin.URL,getString(R.string.url_example)));

            EditText userInput = findViewById(R.id.username);
            userInput.setText(prefs.getString(UmbrellaLogin.USER,null));

            EditText passInput = findViewById(R.id.password);
            passInput.setText(prefs.getString(UmbrellaLogin.PASS,null));
        }

        Spinner intervalSelector = findViewById(R.id.interval);
        ArrayList<String> options = new ArrayList<String>();
        options.add(getString(R.string.check15));
        options.add(getString(R.string.check20));
        options.add(getString(R.string.check30));
        options.add(getString(R.string.check_hourly));
        options.add(getString(R.string.check2));
        options.add(getString(R.string.check4));
        options.add(getString(R.string.check_twice_per_day));
        options.add(getString(R.string.check_daily));

        intervalSelector.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkService();
    }

    private void checkService() {
        final Button serviceButton = findViewById(R.id.service_btn);
        if (serviceRunning()){
            serviceButton.setText(R.string.disable);
            serviceButton.setVisibility(View.VISIBLE);
        } else serviceButton.setText(R.string.enable);
    }

    private void stopService() {
        WorkManager.getInstance(this).cancelAllWork();
        checkService();
    }

    private boolean serviceRunning() {
        try {
            List<WorkInfo> list = WorkManager.getInstance(this).getWorkInfosForUniqueWork(MessageChecker.TAG).get();
            if (list.size()<1) return false;
            Log.d(TAG,"list: "+list);
            boolean state = false;
            for (WorkInfo wi: list) {
                if (wi.getState().equals(WorkInfo.State.ENQUEUED)) state = true;
            }
            return state;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void startService() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(MessageChecker.class,15, TimeUnit.MINUTES).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(MessageChecker.TAG, ExistingPeriodicWorkPolicy.REPLACE,workRequest);
        checkService();
    }

    private void fadeBackground(final View tv, final int r, final int g, final int b) {
        Runnable runnable = new Runnable() {
            int a = 255;
            @Override
            public void run() {
                tv.setBackgroundColor(Color.argb(a,r,g,b));
                if (a>0) {
                    a = a-2;
                    if (a<0) a= 0;
                    handler.postDelayed(this,10);
                }
            }
        };
        handler.postDelayed(runnable,10);
    }

    private void testConnection() {
        btn.setEnabled(false);
        findViewById(R.id.interval_legend).setVisibility(View.INVISIBLE);
        findViewById(R.id.interval).setVisibility(View.INVISIBLE);
        findViewById(R.id.service_btn).setVisibility(View.INVISIBLE);

        EditText urlInput = findViewById(R.id.url);
        final String url = urlInput.getText().toString().trim();

        EditText userInput = findViewById(R.id.username);
        final String username = userInput.getText().toString().trim();

        EditText passInput = findViewById(R.id.password);
        final String password = passInput.getText().toString().trim();

        UmbrellaLogin login = new UmbrellaLogin(url, username, password);
        login.doLogin(this);

    }

    @Override
    public void started() {
        dialog = new ProgressDialog(SettingsActivity.this);
        dialog.setMessage(getString(R.string.trying_to_connect));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public Context context() {
        return this;
    }

    @Override
    public void onResponse(String response) {
        Log.d(TAG,"Response: "+response);
    }

    @Override
    public void onError() {
        Log.d(TAG,"onError");
    }

    @Override
    public void onLoginFailed() {
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.login_failed);
        btn.setEnabled(true);
        if (dialog!=null) dialog.cancel();
        fadeBackground(tv,255,0,0);
    }

    @Override
    public void onTokenReceived(UmbrellaLogin login) {
        if (dialog!=null) dialog.cancel();
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.logged_in);
        findViewById(R.id.interval_legend).setVisibility(View.VISIBLE);
        findViewById(R.id.interval).setVisibility(View.VISIBLE);
        findViewById(R.id.service_btn).setVisibility(View.VISIBLE);
        btn.setEnabled(true);
        fadeBackground(tv,0,255,0);
        login.storeCredentials(getSharedPreferences(CREDENTIALS, MODE_PRIVATE).edit());
    }
}
