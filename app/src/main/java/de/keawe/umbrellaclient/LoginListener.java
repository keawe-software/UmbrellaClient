package de.keawe.umbrellaclient;

import android.content.Context;

public interface LoginListener {
    void started();

    Context context();

    void onLoginResponse(String response);

    void onLoginError();

    void onLoginFailed();

    void onLoginTokenReceived(UmbrellaConnection login);
}
