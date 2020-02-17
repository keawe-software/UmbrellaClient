package de.keawe.umbrellaclient;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UmbrellaLogin {
    private static final String TAG = "UmbrellaLogin";
    public static final String URL = "url";
    public static final String PASS = "pass";
    public static final String USER = "user";

    private final String url;
    private final String pass;
    private final String user;
    private String token = null;

    public UmbrellaLogin(String url, String user, String pass){
        this.url = url;
        this.user = user;
        this.pass = pass;
        CookieHandler.setDefault(new CookieManager());
    }

    public void doLogin(final LoginListener listener) {
        listener.started();
        RequestQueue queue = Volley.newRequestQueue(listener.context(),new HurlStack(){
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
                listener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onError();
            }
        }) {
            @Override
            public void deliverError(VolleyError error) {
                super.deliverError(error);
                String token = error.networkResponse.headers.get("Token");
                if (token == null){
                    listener.onLoginFailed();
                } else {
                    UmbrellaLogin.this.token = token;
                    listener.onTokenReceived(UmbrellaLogin.this);
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

    public String token() {
        return token;
    }

    public void get(String path, final LoginListener listener) {
        RequestQueue queue = Volley.newRequestQueue(listener.context());
        String glue = path.contains("?") ? "&" : "?";
        String full=url+path+glue+"token="+token;
        Log.d(TAG,"get("+full+")");
        StringRequest request = new StringRequest(Request.Method.GET, full, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onError();
            }
        });
        queue.add(request);
    }
}
