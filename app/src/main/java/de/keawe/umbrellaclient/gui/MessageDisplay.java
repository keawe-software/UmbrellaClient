package de.keawe.umbrellaclient.gui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;

public class MessageDisplay extends AppCompatActivity {
    private static final String TAG = "MessageDisplay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_display);
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
        ((TextView)findViewById(R.id.content)).setText(msg.content());

    }
}
