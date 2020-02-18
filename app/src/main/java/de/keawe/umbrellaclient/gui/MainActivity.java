package de.keawe.umbrellaclient.gui;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new MessageDB(this);

        Button settingsBtn = findViewById(R.id.settings_btn);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
    }

    private void openSettings() {
        Intent settings = new Intent(this,SettingsActivity.class);
        startActivity(settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Message> messages = MessageDB.loadLast(10);
        Log.d(TAG,"messages: "+messages);
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
