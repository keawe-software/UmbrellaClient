package de.keawe.umbrellaclient.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;
import java.util.Vector;

import androidx.annotation.Nullable;

public class MessageDB extends SQLiteOpenHelper {
    private static final String TAG = "MessageDB";
    private static final String MESSAGES = "messages";
    private static final String MESSAGE_ID = "message_id";
    private static final int VERSION = 1;
    private static final String AUTHOR = "author";
    private static final String CONTENT = "content";
    private static final String SUBJECT = "subject";
    private static final String TIME = "time";
    private static final String AUTHOR_ID = "author_id";
    private static MessageDB db = null;

    public MessageDB(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        MessageDB.db = this;
    }

    public MessageDB(Context c) {
        this(c,TAG,null,VERSION);
    }

    public static Message store(Message message) {
        if (load(message.id()) == null) {
            ContentValues values = new ContentValues();
            values.put(MESSAGE_ID, message.id());
            values.put(AUTHOR, message.author());
            values.put(AUTHOR_ID, message.authorId());
            values.put(TIME, message.time());
            values.put(SUBJECT, message.subject());
            values.put(CONTENT, message.content());
            writer().insert(MESSAGES, null, values);
            return message;
        }
        return null;
    }

    private static Message load(long id) {
        String[] args = new String[]{""+id};
        Cursor cursor = reader().query(MESSAGES, null, MESSAGE_ID+" = ?", args, null, null, null);
        if (cursor.moveToFirst()) return new Message(
                cursor.getInt(cursor.getColumnIndex(MESSAGE_ID)),
                cursor.getString(cursor.getColumnIndex(AUTHOR)),
                cursor.getInt(cursor.getColumnIndex(AUTHOR_ID)),
                cursor.getLong(cursor.getColumnIndex(TIME)),
                cursor.getString(cursor.getColumnIndex(SUBJECT)),
                cursor.getString(cursor.getColumnIndex(CONTENT))
        );
        return null;
    }

    private static SQLiteDatabase writer() {
        return db.getWritableDatabase();
    }

    public static List<Message> loadLast(int limit) {
        List<Message> messages = new Vector<Message>();
        Cursor cursor = reader().query(MESSAGES, null, null, null, null, null, MESSAGE_ID+" DESC", ""+limit);
        if (cursor.moveToFirst()){
            do {
                Message message = new Message(
                        cursor.getInt(cursor.getColumnIndex(MESSAGE_ID)),
                        cursor.getString(cursor.getColumnIndex(AUTHOR)),
                        cursor.getInt(cursor.getColumnIndex(AUTHOR_ID)),
                        cursor.getLong(cursor.getColumnIndex(TIME)),
                        cursor.getString(cursor.getColumnIndex(SUBJECT)),
                        cursor.getString(cursor.getColumnIndex(CONTENT))
                );
                messages.add(message);
            } while (cursor.moveToNext());
        }
        return messages;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String messageTable = "CREATE TABLE "+MESSAGES+" ("+MESSAGE_ID+" INTEGER PRIMARY KEY, "+AUTHOR+" TEXT, "+AUTHOR_ID+" INT NOT NULL, "+TIME+" LONG, "+SUBJECT+" TEXT, "+CONTENT+" TEXT);";
        db.execSQL(messageTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static Message lastMessage(){
        List<Message> mesages = loadLast(1);
        if (mesages.isEmpty()) return null;
        return mesages.get(0);
    }

    private static SQLiteDatabase reader() {
        return db.getReadableDatabase();
    }
}
