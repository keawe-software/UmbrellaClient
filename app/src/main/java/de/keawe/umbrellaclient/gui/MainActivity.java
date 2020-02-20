package de.keawe.umbrellaclient.gui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.LoginListener;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.UmbrellaLogin;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;

public class MainActivity extends AppCompatActivity implements LoginListener {
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
    }

    private void refresh() {
        findViewById(R.id.refresh_btn).setEnabled(false);
        SharedPreferences credentials = getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaLogin.URL,null);
        String user = credentials.getString(UmbrellaLogin.USER,null);
        String pass = credentials.getString(UmbrellaLogin.PASS,null);

        UmbrellaLogin login = new UmbrellaLogin(url,user,pass);
        login.doLogin(this);
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

        updateMessageList();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();


    }

    public void showMessage(Message message) {
        Intent msgDisplay = new Intent(this,MessageDisplay.class);
        msgDisplay.putExtra(Message.TAG,message);
        startActivity(msgDisplay);
    }

    @Override
    public void started() {

    }

    @Override
    public Context context() {
        return this;
    }

    @Override
    public void onResponse(String response) {
        if (response.trim().startsWith("[{")) try {
            JSONArray arr = new JSONArray(response);
            for (int i = 0; i<arr.length(); i++) new Message(arr.getJSONObject(i)).store();
            updateMessageList();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        findViewById(R.id.refresh_btn).setEnabled(true);
    }

    private void updateMessageList() {
        List<Message> messages = MessageDB.loadLast(10);
        LinearLayout msgList = findViewById(R.id.message_list);
        msgList.removeAllViews();
        for (Message msg : messages) msgList.addView(msg.view(this));
    }

    @Override
    public void onError() {

    }

    @Override
    public void onLoginFailed() {
        findViewById(R.id.refresh_btn).setEnabled(true);
    }

    @Override
    public void onTokenReceived(UmbrellaLogin login) {
        Message lastMessage = MessageDB.lastMessage();
        long id = lastMessage == null ? -1 : lastMessage.id();
        login.get("/user/json?messages="+id,this);
    }
}
