package de.keawe.umbrellaclient;

import android.content.Context;

interface LoginListener {
    void started();

    Context context();

    void onResponse(String response);

    void onError();

    void onLoginFailed();

    void onTokenReceived(UmbrellaLogin login);
}
