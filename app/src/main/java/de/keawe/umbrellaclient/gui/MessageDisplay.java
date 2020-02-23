package de.keawe.umbrellaclient.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.Serializable;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.UmbrellaConnection;
import de.keawe.umbrellaclient.db.Message;
import us.feras.mdv.MarkdownView;

public class MessageDisplay extends AppCompatActivity {
    private static final String TAG = "MessageDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_display);
        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        SharedPreferences credentials = getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE);
        String url = credentials.getString(UmbrellaConnection.URL,null);
        String user = credentials.getString(UmbrellaConnection.USER,null);
        String pass = credentials.getString(UmbrellaConnection.PASS,null);

        UmbrellaConnection login = new UmbrellaConnection(url,user,pass);
        login.openBrowser(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Serializable msg = getIntent().getSerializableExtra(Message.TAG);
        if (msg instanceof Message) display((Message) msg);
    }

    private void display(Message msg) {
        ((TextView)findViewById(R.id.author)).setText(msg.author());
        ((TextView)findViewById(R.id.date)).setText(msg.timeString());
        ((TextView)findViewById(R.id.subject)).setText(msg.subject());
        MarkdownView content = findViewById(R.id.content);
        //Log.d(TAG,"displaying "+msg.content());
        content.loadMarkdown(msg.content());
    }
}
