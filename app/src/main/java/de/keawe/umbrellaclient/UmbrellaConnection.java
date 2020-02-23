package de.keawe.umbrellaclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private String token = null;

    private static String getPref(Context c, String key){
        return c.getSharedPreferences(SettingsActivity.CREDENTIALS,Context.MODE_PRIVATE).getString(key,null);
    }

    public UmbrellaConnection(Context c){
        this(getPref(c,URL),getPref(c,USER),getPref(c,PASS));
    }

    public UmbrellaConnection(String url, String user, String pass){
        this.url = url;
        this.user = user;
        this.pass = pass;
        CookieHandler.setDefault(new CookieManager());
    }

    public void doLogin(final LoginListener listener) {
        listener.started();
        Context context = listener.context();
        if (context == null) throw new NullPointerException("You need to implement the context() method in your LoginListener!");
        RequestQueue queue = Volley.newRequestQueue(context,new HurlStack(){
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setInstanceFollowRedirects(false);
                return connection;
            }
        });
        StringRequest request = new StringRequest(Request.Method.POST, url+"/user/login", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onLoginResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onLoginError();
            }
        }) {
            @Override
            public void deliverError(VolleyError error) {
                super.deliverError(error);
                String token = error.networkResponse.headers.get("Token");
                if (token == null){
                    listener.onLoginFailed();
                } else {
                    UmbrellaConnection.this.token = token;
                    listener.onLoginTokenReceived(UmbrellaConnection.this);
                }
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username",user);
                params.put("pass",pass);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(12000,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    public void storeCredentials(SharedPreferences.Editor credentials) {
        credentials.putString(URL,url);
        credentials.putString(USER,user);
        credentials.putString(PASS,pass);
        credentials.commit();
    }

    public void get(String path, final LoginListener listener) {
        RequestQueue queue = Volley.newRequestQueue(listener.context());
        String glue = path.contains("?") ? "&" : "?";
        String full=url+path+glue+"token="+token;
        StringRequest request = new StringRequest(Request.Method.GET, full, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onLoginResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onLoginError();
            }
        });
        queue.add(request);
    }

    public void openBrowser(final Context c) {
        doLogin(new LoginListener() {
            @Override
            public void started() {
                //Log.d(TAG,"openBrowser.started()");
            }

            @Override
            public Context context() {
                return c;
            }

            @Override
            public void onLoginResponse(String response) {
                //Log.d(TAG,"openBrowser.onLoginResponse("+response+")");
            }

            @Override
            public void onLoginError() {
                //Log.d(TAG,"openBrowser.onLoginError()");
            }

            @Override
            public void onLoginFailed() {
                //Log.d(TAG,"openBrowser.onLoginFailed()");
            }

            @Override
            public void onLoginTokenReceived(UmbrellaConnection login) {
                //Log.d(TAG,"openBrowser.onLoginTokenReceived(...)");
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url+"/user/edit?token="+token));
                c.startActivity(myIntent);
            }
        });
    }

    public void fetchMessages(final MessageHandler messageHandler) {
        doLogin(new LoginListener() {
            @Override
            public void started() {
                //Log.d(TAG,"fetchMessages.started()");
            }

            @Override
            public Context context() {
                return messageHandler.context();
            }

            @Override
            public void onLoginResponse(String response) {
                //Log.d(TAG,"fetchMessages.onLoginResponse("+response+")");
                if (response.trim().equals("[]")){
                    messageHandler.gotNewMessages(0);
                } else if (response.trim().startsWith("[{")) try {
                    JSONArray arr = new JSONArray(response);
                    int count = 0;
                    for (int i = 0; i<arr.length(); i++) {
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
            public void onLoginError() {
                //Log.d(TAG,"fetchMessages.onLoginError()");
            }

            @Override
            public void onLoginFailed() {
                //Log.d(TAG,"fetchMessages.onLoginFailed()");
            }

            @Override
            public void onLoginTokenReceived(UmbrellaConnection login) {
                //Log.d(TAG,"fetchMessages.onLoginTokenReceived(login)");
                Message lastMessage = MessageDB.lastMessage();
                long id = lastMessage == null ? -1 : lastMessage.id();
                login.get("/user/json?messages="+id,this);
            }
        });
    }
}
