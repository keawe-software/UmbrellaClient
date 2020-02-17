package de.keawe.umbrellaclient;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String CREDENTIALS = "credentials";
    private static final String URL = "url";
    private static final String PASS = "pass";
    private static final String USER = "user";

    private Handler handler = new Handler();
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.setup_btn); 
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn.setEnabled(false);
                setup();                
            }
        });

        SharedPreferences prefs = getSharedPreferences(CREDENTIALS, MODE_PRIVATE);
        if (prefs != null){
            EditText urlInput = findViewById(R.id.url);
            urlInput.setText(prefs.getString(URL,getString(R.string.url_example)));

            EditText userInput = findViewById(R.id.username);
            userInput.setText(prefs.getString(USER,null));

            EditText passInput = findViewById(R.id.password);
            passInput.setText(prefs.getString(PASS,null));
        }
    }

    private void connectionTest(final String urlString, final String username, final String password) {
        Log.d(TAG,"trying to connect to "+urlString+" using "+username+"/"+password);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this,new HurlStack(){
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setInstanceFollowRedirects(false);
                return connection;
            }
        });
        StringRequest request = new StringRequest(Request.Method.POST, urlString+"/user/login", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) { }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { }
        }) {

            @Override
            public void deliverError(VolleyError error) {
                super.deliverError(error);
                String token = error.networkResponse.headers.get("Token");
                if (token == null){
                    loginFailed();
                } else {
                    loginSuccess(urlString,username,password,token);
                }
                btn.setEnabled(true);
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("username",username);
                params.put("pass",password);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(12000,0,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void loginFailed() {
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.login_failed);
        fadeBackground(tv,255,0,0);
    }

    private void fadeBackground(final View tv, final int r, final int g, final int b) {
        Runnable runnable = new Runnable() {
            int a = 255;
            @Override
            public void run() {
                tv.setBackgroundColor(Color.argb(a,r,g,b));
                if (a>0) {
                    a = a-2;
                    if (a<0) a= 0;
                    handler.postDelayed(this,10);
                }
            }
        };
        handler.postDelayed(runnable,10);
    }

    private void loginSuccess(String url, String username, String password, String token) {
        TextView tv = findViewById(R.id.status);
        tv.setText(R.string.logged_in);
        fadeBackground(tv,0,255,0);
        storeCredentials(url,username,password);


    }

    private void storeCredentials(String url, String username, String password) {
        SharedPreferences.Editor credentials = getSharedPreferences(CREDENTIALS, MODE_PRIVATE).edit();
        credentials.putString(URL,url);
        credentials.putString(USER,username);
        credentials.putString(PASS,password);
        credentials.commit();
    }

    private void setup() {
        EditText urlInput = findViewById(R.id.url);
        final String url = urlInput.getText().toString().trim();

        EditText userInput = findViewById(R.id.username);
        final String username = userInput.getText().toString().trim();

        EditText passInput = findViewById(R.id.password);
        final String password = passInput.getText().toString().trim();

        connectionTest(url,username,password);


    }
}
