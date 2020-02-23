package de.keawe.umbrellaclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.HashMap;
import java.util.Map;

import de.keawe.umbrellaclient.db.Message;
import de.keawe.umbrellaclient.db.MessageDB;
import de.keawe.umbrellaclient.gui.SettingsActivity;

public class UmbrellaConnection {
    private static final String TAG = "UmbrellaConnection";
    public static final String URL = "url";
    public static final String PASS = "pass";
    public static final String USER = "user";
    private final String url;
    private final String pass;
    private final String user;

    public UmbrellaConnection(Context c) {
        this(getPref(c, URL), getPref(c, USER), getPref(c, PASS));
    }

    public UmbrellaConnection(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
        CookieHandler.setDefault(new CookieManager());
    }

    public void getPage(final String path, final RequestListener listener) {
        Context context = listener.context();
        if (context == null) throw new NullPointerException("You need to implement the context() method in your RequestListener!");
        String address = url + "/user/login?returnTo="+url+"/"+path;
        Log.d(TAG,"getPage("+address+")");
        final RequestQueue queue = Volley.newRequestQueue(context);

        final Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onResponse(response);
            }
        };

        final Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onError();
            }
        };

        StringRequest request = new StringRequest(Request.Method.POST, address, responseListener, errorListener){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", user);
                params.put("pass", pass);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(12000, 10, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    public void fetchMessages(final MessageHandler messageHandler) {
        Message lastMessage = MessageDB.lastMessage();
        long id = lastMessage == null ? -1 : lastMessage.id();
        getPage("user/json?messages=" + id, new RequestListener() {

            @Override
            public Context context() {
                return messageHandler.context();
            }

            @Override
            public void onResponse(String response) {
                //Log.d(TAG,"fetchMessages.onResponse("+response+")");
                if (response.trim().equals("[]")) {
                    messageHandler.gotNewMessages(0);
                } else if (response.trim().startsWith("[{")) try {
                    JSONArray arr = new JSONArray(response);
                    int count = 0;
                    for (int i = 0; i < arr.length(); i++) {
                        Message msg = new Message(arr.getJSONObject(i)).store();
                        if (msg != null) {
                            count++;
                            messageHandler.newMessage(msg);
                        }
                    }
                    messageHandler.gotNewMessages(count);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError() {
                //Log.d(TAG,"fetchMessages.onError()");
                messageHandler.onError();
            }
        });
    }

    private static String getPref(Context c, String key) {
        return c.getSharedPreferences(SettingsActivity.CREDENTIALS, Context.MODE_PRIVATE).getString(key, null);
    }

    public void openBrowser(final Context c) {
        getPage("user/token", new RequestListener() {
            @Override
            public Context context() {
                return c;
            }

            @Override
            public void onResponse(String token) {
                //Log.d(TAG,"openBrowser.onResponse("+response+")");
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + "/user/edit?token=" + token));
                c.startActivity(myIntent);
            }

            @Override
            public void onError() {
                //Log.d(TAG,"openBrowser.onError()");
            }
        });
    }

    public void storeCredentials(SharedPreferences.Editor credentials) {
        credentials.putString(URL, url);
        credentials.putString(USER, user);
        credentials.putString(PASS, pass);
        credentials.commit();
    }
}
