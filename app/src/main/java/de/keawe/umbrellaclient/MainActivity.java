package de.keawe.umbrellaclient;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.ArrayList;
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

        btn = findViewById(R.id.test_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
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

        Spinner intervalSelector = findViewById(R.id.interval);
        ArrayList<String> options = new ArrayList<String>();
        options.add(getString(R.string.check5));
        options.add(getString(R.string.check10));
        options.add(getString(R.string.check15));
        options.add(getString(R.string.check20));
        options.add(getString(R.string.check30));
        options.add(getString(R.string.check_hourly));
        options.add(getString(R.string.check2));
        options.add(getString(R.string.check4));
        options.add(getString(R.string.check_twice_per_day));
        options.add(getString(R.string.check_daily));

        intervalSelector.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,options));
    }

    private void connectionTest(final String urlString, final String username, final String password) {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage(getString(R.string.trying_to_connect));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
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
            public void onResponse(String response) {
                dialog.cancel();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.cancel();
            }
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
                dialog.cancel();
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
        findViewById(R.id.interval_legend).setVisibility(View.VISIBLE);
        findViewById(R.id.interval).setVisibility(View.VISIBLE);
        findViewById(R.id.setup_btn).setVisibility(View.VISIBLE);

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

    private void testConnection() {
        btn.setEnabled(false);
        findViewById(R.id.interval_legend).setVisibility(View.INVISIBLE);
        findViewById(R.id.interval).setVisibility(View.INVISIBLE);
        findViewById(R.id.setup_btn).setVisibility(View.INVISIBLE);

        EditText urlInput = findViewById(R.id.url);
        final String url = urlInput.getText().toString().trim();

        EditText userInput = findViewById(R.id.username);
        final String username = userInput.getText().toString().trim();

        EditText passInput = findViewById(R.id.password);
        final String password = passInput.getText().toString().trim();

        connectionTest(url,username,password);


    }
}
