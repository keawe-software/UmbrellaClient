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
import de.keawe.umbrellaclient.CheckService;
import de.keawe.umbrellaclient.MessageHandler;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.UmbrellaConnection;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;

import static de.keawe.umbrellaclient.gui.SettingsActivity.INTERVAL_MINUTES;

public class MainActivity extends AppCompatActivity implements MessageHandler {
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

        findViewById(R.id.refresh_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        updateMessageList();
    }

    private void refresh() {
        //Log.d(TAG,"refresh()");
        findViewById(R.id.refresh_btn).setEnabled(false);
        new UmbrellaConnection(this).fetchMessages(this);
    }

    private void login() {
        SharedPreferences credentials = getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaConnection.URL,null);
        String user = credentials.getString(UmbrellaConnection.USER,null);
        String pass = credentials.getString(UmbrellaConnection.PASS,null);

        UmbrellaConnection login = new UmbrellaConnection(url,user,pass);
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

        // go to setitings, if no login has been activated before
        String url = credentials.getString(UmbrellaConnection.URL,null);
        if (url == null) {
            openSettings();
            return;
        }

        // restart service in case it is running but not running
        if (!CheckService.running()){
            int minutes = credentials.getInt(INTERVAL_MINUTES,0);
            if (minutes > 0) startService(new Intent(this, CheckService.class));
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();


    }

    public void showMessage(Message message) {
        Intent msgDisplay = new Intent(this,MessageDisplay.class);
        msgDisplay.putExtra(Message.TAG,message);
        startActivity(msgDisplay);
    }

    private void updateMessageList() {
        List<Message> messages = MessageDB.loadLast(10);
        LinearLayout msgList = findViewById(R.id.message_list);
        msgList.removeAllViews();
        for (Message msg : messages) msgList.addView(msg.view(this));
    }

    @Override
    public void newMessage(Message msg) {

    }

    @Override
    public void gotNewMessages(int count) {
        if (count > 0) updateMessageList();
        findViewById(R.id.refresh_btn).setEnabled(true);
    }

    @Override
    public Context context() {
        return this;
    }
}
