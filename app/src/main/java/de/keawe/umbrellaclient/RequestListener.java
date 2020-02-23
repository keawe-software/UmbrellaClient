package de.keawe.umbrellaclient;

import android.content.Context;

public interface RequestListener {

    Context context();

    void onResponse(String response);

    void onError();

}
