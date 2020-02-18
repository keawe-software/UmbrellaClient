package de.keawe.umbrellaclient.db;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import de.keawe.umbrellaclient.R;
import de.keawe.umbrellaclient.gui.MainActivity;

public class Message implements Serializable {
    public static final String TAG = "Message";
    private final int id;
    private final int author_id;
    private final String author;
    private final String content;
    private final String subject;
    private final long time;
    private static SimpleDateFormat df = null;

    public Message(int message_id, String author, int author_id, long time, String subject, String content) {
        this.id = message_id;
        this.author = author;
        this.author_id = author_id;
        this.subject = subject;
        this.content = content;
        this.time = time;
    }

    public Message(JSONObject json) throws JSONException {
        this(
                json.getInt("message_id"),
                json.getJSONObject("from").getString("login"),
                json.getInt("author"),
                json.getLong("timestamp"),
                json.getString("subject"),
                json.getString("body")
        );
    }

    public long id() {
        return id;
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName()+"(id: "+id+", author: "+author+" ("+author_id+"), time: "+new Date(time) +", subject: "+subject+", content: "+content+")";
    }

    public Message store() {
        return MessageDB.store(this);
    }

    public String author() {
        return author;
    }

    public int authorId(){
        return author_id;
    }

    public  long time(){
        return time;
    }

    public String subject(){
        return subject;
    }

    public String content(){
        return content;
    }

    public View view(final MainActivity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.message,null);
        TextView author = view.findViewById(R.id.author);
        author.setText(author());

        TextView time = view.findViewById(R.id.time);
        time.setText(timeString());

        TextView subject = view.findViewById(R.id.subject);
        subject.setText(subject());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showMessage(Message.this);
            }
        });
        return view;
    }

    public String timeString() {
        if (df == null) df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date(time*1000));
    }
}
