package de.keawe.umbrellaclient;

import android.content.Context;

import de.keawe.umbrellaclient.db.Message;

public interface MessageHandler {
    void newMessage(Message msg);
    void gotNewMessages(int count);
    Context context();
    void onError();
}
