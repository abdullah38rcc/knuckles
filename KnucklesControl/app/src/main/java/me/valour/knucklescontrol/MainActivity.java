package me.valour.knucklescontrol;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import me.valour.knucklescontrol.helpers.Commander;


public class MainActivity extends ActionBarActivity implements PlaceholderFragment.PlaceholderFragmentListener {

    PlaceholderFragment frag;
    FragmentManager manager;
    RequestQueue requestQueue;

    public static String boardHost = "http://192.168.0.111:8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Startup","Initiating startup sequence.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frag = new PlaceholderFragment();
        manager = getFragmentManager();

        if (savedInstanceState == null) {
            manager.beginTransaction().add(R.id.body, frag).commit();
            /*
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, frag)
                    .commit(); */
        }
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private long time = 0;

            @Override
            public void run()
            {
                // do stuff then
                // can call h again after work!
                time += 2000;
                Log.d("TimerExample", "Going for... " + time);
                updateStatus();
                h.postDelayed(this, 2000);
            }
        }, 1000); // 1 second delay (takes millis)
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showAlert("Settings");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    public void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Log.d("voice", spokenText);
            frag.setText(spokenText);
            int command = Commander.recognize(spokenText);
            switch(command) {
                case Commander.LIGHTS:
                    toggleLights();
                    break;
            }
            // requestTemperatureChange(command);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(this);
        }
        return requestQueue;
    }

    private void updateStatus() {
        requestQueue = getRequestQueue();
        String url = boardHost+"/status";

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("json", response.toString());
                try {
                    boolean isHeating = response.getBoolean("is_heating");
                    if(isHeating) {

                        frag.registerTempChange(1);
                        Log.d("Heat","Heating");
                    } else {

                        frag.registerTempChange(-1);
                        Log.d("Cool","Cooling");
                    }
                    JSONArray temps = response.getJSONArray("temps");
                    int len = temps.length();
                    for(int i=0; i<len; i++){
                        frag.registerFingerTemperatures(i, (float)temps.getDouble(i) );
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.e("json", error.getMessage());
                Log.e("json", "Error reaching server");
            }
        });

        requestQueue.add(jsonRequest);
    }

    private void toggleLights() {
        Log.d("LIGHTS","LIGHTS");
        /*
        requestQueue = getRequestQueue();
        String url = boardHost+"/light";

        JSONObject obj = new JSONObject();
        try {
            obj.put("on", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("json", response.toString());
                try {
                    boolean success = response.getBoolean("on");
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String msg = (error == null) ? "Error reaching server" : error.getMessage();
                Log.e("json", msg);
            }
        });

        requestQueue.add(jsonRequest);
        */
    }

    private void requestTemperatureChange(int delta) {
        if(delta != Commander.COLDER && delta != Commander.HOTTER) {
            showAlert("Unknown command: "+delta);
           return;
        }
        requestQueue = getRequestQueue();
        String url = boardHost+"/heat";

        Log.d("Request temp change", ""+delta);

        JSONObject obj = new JSONObject();
        try {
            obj.put("adjustment", delta);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, obj, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("json", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("json", error.getMessage());
            }
        });

        requestQueue.add(jsonRequest);
    }

    public void showAlert(String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Temp Control");
        alertDialog.setMessage("Message: "+msg);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // here you can add functions
            }
        });
        alertDialog.setIcon(R.drawable.warning);
        alertDialog.show();
    }

}
