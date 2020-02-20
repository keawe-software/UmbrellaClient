package de.keawe.umbrellaclient.gui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.UmbrellaLogin;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new MessageDB(this);

        findViewById(R.id.settings_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        SharedPreferences credentials = getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaLogin.URL,null);
        String user = credentials.getString(UmbrellaLogin.USER,null);
        String pass = credentials.getString(UmbrellaLogin.PASS,null);

        UmbrellaLogin login = new UmbrellaLogin(url,user,pass);
        login.openBrowser(this);
    }

    private void openSettings() {
        Intent settings = new Intent(this,SettingsActivity.class);
        startActivity(settings);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences credentials = getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaLogin.URL,null);
        if (url == null) {
            openSettings();
            return;
        }

        List<Message> messages = MessageDB.loadLast(10);
        LinearLayout msgList = findViewById(R.id.message_list);
        msgList.removeAllViews();
        for (Message msg : messages){
            msgList.addView(msg.view(this));
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();


    }

    public void showMessage(Message message) {
        Intent msgDisplay = new Intent(this,MessageDisplay.class);
        msgDisplay.putExtra(Message.TAG,message);
        startActivity(msgDisplay);
    }
}
