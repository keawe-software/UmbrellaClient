package de.keawe.umbrellaclient.gui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.CheckService;
import de.keawe.umbrellaclient.LoginListener;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.TimeOption;
import de.keawe.umbrellaclient.UmbrellaConnection;


public class SettingsActivity extends AppCompatActivity implements LoginListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "SettingsActivity";
    public static final String CREDENTIALS = "credentials";
    public static final String INTERVAL_MINUTES = "interval";
    private static int HOUR = 60;

    private Handler handler = new Handler();
    private Button testConnectionButton;
    private ProgressDialog dialog;
    private Button serviceButton;
    private SharedPreferences prefs;
    private Spinner intervalSelector;

    private Runnable checkService = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"checkService()");
            if (serviceRunning()){
                serviceButton.setText(R.string.disable);
                findViewById(R.id.schedule_options).setVisibility(View.VISIBLE);
            } else serviceButton.setText(R.string.enable);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        testConnectionButton = findViewById(R.id.test_btn);
        testConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });

        serviceButton = findViewById(R.id.service_btn);
        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleService();
            }
        });

        intervalSelector = findViewById(R.id.interval);
        ArrayList<TimeOption> options = new ArrayList<>();
        options.add(new TimeOption(getString(R.string.check5),5));
        options.add(new TimeOption(getString(R.string.check15),15));
        options.add(new TimeOption(getString(R.string.check20),20));
        options.add(new TimeOption(getString(R.string.check30),30));
        options.add(new TimeOption(getString(R.string.check_hourly),HOUR));
        options.add(new TimeOption(getString(R.string.check2),2*HOUR));
        options.add(new TimeOption(getString(R.string.check4),4*HOUR));
        options.add(new TimeOption(getString(R.string.check_twice_per_day),12*HOUR));
        options.add(new TimeOption(getString(R.string.check_daily),24*HOUR));

        intervalSelector.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options));

        prefs = getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
        if (prefs != null){
            EditText urlInput = findViewById(R.id.url);
            urlInput.setText(prefs.getString(UmbrellaConnection.URL,getString(R.string.url_example)));

            EditText userInput = findViewById(R.id.username);
            userInput.setText(prefs.getString(UmbrellaConnection.USER,null));

            EditText passInput = findViewById(R.id.password);
            passInput.setText(prefs.getString(UmbrellaConnection.PASS,null));

            int minutes = prefs.getInt(INTERVAL_MINUTES,0);
            for (int i = 0; i< options.size(); i++){
                Object item = intervalSelector.getItemAtPosition(i);
                if (item instanceof TimeOption){
                    TimeOption option = (TimeOption) item;
                    if (option.minutes() == minutes) intervalSelector.setSelection(i);
                }
            }
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                intervalSelector.setOnItemSelectedListener(SettingsActivity.this);
            }
        };
        new Handler().postDelayed(r,1000);

    }

    private void toggleService() {
        if (serviceRunning()){
            stopService();
        } else startService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkService();
    }

    private void checkService() {
        handler.postDelayed(checkService,500);
    }

    private void stopService() {
        Log.d(TAG,"stopService()");
        storeInterval(0);
        CheckService.stop();
        checkService();
    }

    private boolean serviceRunning() {
        boolean result = CheckService.running();
        Log.d(TAG,"serviceRunning() => " + result);
        return result;
    }

    private void startService() {
        Log.d(TAG,"startService()");
        setInterValFromSelection();
        startService(new Intent(this, CheckService.class));
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
        testConnectionButton.setEnabled(false);
        findViewById(R.id.schedule_options).setVisibility(View.INVISIBLE);

        EditText urlInput = findViewById(R.id.url);
        final String url = urlInput.getText().toString().trim();

        EditText userInput = findViewById(R.id.username);
        final String username = userInput.getText().toString().trim();

        EditText passInput = findViewById(R.id.password);
        final String password = passInput.getText().toString().trim();

        UmbrellaConnection login = new UmbrellaConnection(url, username, password);
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

    private void setInterValFromSelection(){
        Log.d(TAG,"setIntervalFromSelection()");
        Object item = intervalSelector.getSelectedItem();
        if (item instanceof TimeOption) storeInterval(((TimeOption) item).minutes());
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG,"onItemSelected()");
        setInterValFromSelection();
    }

    private void storeInterval(int minutes){
        Log.d(TAG,"storeInterval("+minutes+" min)");
        prefs.edit().putInt(INTERVAL_MINUTES,minutes).commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onLoginResponse(String response) {
        Log.d(TAG,"Response: "+response);
    }

    @Override
    public void onLoginError() {
        Log.d(TAG,"onLoginError");
    }

    @Override
    public void onLoginFailed() {
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.login_failed);
        testConnectionButton.setEnabled(true);
        if (dialog!=null) dialog.cancel();
        fadeBackground(tv,255,0,0);
    }

    @Override
    public void onLoginTokenReceived(UmbrellaConnection login) {
        if (dialog!=null) dialog.cancel();
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.logged_in);
        findViewById(R.id.schedule_options).setVisibility(View.VISIBLE);
        testConnectionButton.setEnabled(true);
        fadeBackground(tv,0,255,0);
        login.storeCredentials(prefs.edit());
    }
}
