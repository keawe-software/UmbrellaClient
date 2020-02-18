package de.keawe.umbrellaclient;

import android.content.Context;

public interface LoginListener {
    void started();

    Context context();

    void onResponse(String response, String token);

    void onError();

    void onLoginFailed();

    void onTokenReceived(UmbrellaLogin login);
}
