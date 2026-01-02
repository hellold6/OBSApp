package com.example.obsapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.obsapp.databinding.ActivityMainBinding;
import com.example.obsapp.network.ObsWebSocketListener;

import org.json.JSONException;
import org.json.JSONObject;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private OkHttpClient client;
    private WebSocket webSocket;
    private boolean isObsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // The network_security_config.xml file now handles the self-signed certificate.
        // We can now use a simple, clean OkHttpClient.
        client = new OkHttpClient();

        Request request = new Request.Builder().url("wss://192.168.1.105:4455").build();
        ObsWebSocketListener listener = new ObsWebSocketListener(new Function1<JSONObject, Unit>() {
            @Override
            public Unit invoke(JSONObject message) {
                runOnUiThread(() -> processObsMessage(message));
                return Unit.INSTANCE;
            }
        });
        webSocket = client.newWebSocket(request, listener);

        Button mediaSourceButton = findViewById(R.id.media_source_button);
        mediaSourceButton.setOnClickListener(v -> {
            if (isObsReady) {
                getSceneItemId("camera", "Media Source");
            } else {
                Toast.makeText(MainActivity.this, "OBS not ready yet. Please restart the app.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getSceneItemId(String sceneName, String sourceName) {
        JSONObject request = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put("requestType", "GetSceneItemId");
            data.put("sceneName", sceneName);
            data.put("sourceName", sourceName);

            request.put("op", 6);
            request.put("d", data);
            request.put("requestId", "get-scene-item-id");

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
        }
        Log.d(TAG, "Sending GetSceneItemId request: " + request.toString());
        webSocket.send(request.toString());
    }

    private void processObsMessage(JSONObject message) {
        Log.d(TAG, "Processing message: " + message.toString());
        try {
            int opCode = message.getInt("op");
            switch (opCode) {
                case 0: // Hello
                    Log.d(TAG, "OBS says Hello. Sending Identify.");
                    identifyObs();
                    break;
                case 2: // Identified
                    Log.d(TAG, "Successfully Identified with OBS. Ready to send commands.");
                    isObsReady = true;
                    break;
                case 7: // RequestResponse
                    JSONObject responseData = message.getJSONObject("d");
                    String requestId = responseData.getString("requestId");
                    if ("get-scene-item-id".equals(requestId)) {
                        Log.d(TAG, "Received response for GetSceneItemId");
                        int sceneItemId = responseData.getJSONObject("responseData").getInt("sceneItemId");
                        getSceneItemEnabled(responseData.getJSONObject("requestStatus").getString("sceneName"), sceneItemId);
                    } else if ("get-scene-item-enabled".equals(requestId)){
                        Log.d(TAG, "Received response for GetSceneItemEnabled");
                        boolean isEnabled = responseData.getJSONObject("responseData").getBoolean("sceneItemEnabled");
                        int sceneItemId = responseData.getJSONObject("requestStatus").getInt("sceneItemId");
                        setSceneItemEnabled(responseData.getJSONObject("requestStatus").getString("sceneName"), sceneItemId, !isEnabled);
                    }
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error processing JSON message", e);
        }
    }

    private void identifyObs() {
        JSONObject request = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put("rpcVersion", 1);
            // ** IF YOU HAVE A PASSWORD, ADD IT HERE. **
            // data.put("authentication", "YOUR_PASSWORD_HERE");

            request.put("op", 1); // Identify op code
            request.put("d", data);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating Identify message", e);
        }
        Log.d(TAG, "Sending Identify message: " + request.toString());
        webSocket.send(request.toString());
    }
    
    private void getSceneItemEnabled(String sceneName, int sceneItemId) {
        JSONObject request = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put("requestType", "GetSceneItemEnabled");
            data.put("sceneName", sceneName);
            data.put("sceneItemId", sceneItemId);

            request.put("op", 6);
            request.put("d", data);
            request.put("requestId", "get-scene-item-enabled");

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
        }
        Log.d(TAG, "Sending GetSceneItemEnabled request: " + request.toString());
        webSocket.send(request.toString());
    }

    private void setSceneItemEnabled(String sceneName, int sceneItemId, boolean enabled) {
        JSONObject setRequest = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put("requestType", "SetSceneItemEnabled");
            data.put("sceneName", sceneName);
            data.put("sceneItemId", sceneItemId);
            data.put("sceneItemEnabled", enabled);

            setRequest.put("op", 6); // Operation code for requests
            setRequest.put("d", data);
            setRequest.put("requestId", "set-scene-item-enabled");

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for toggle request", e);
        }
        Log.d(TAG, "Sending toggle request: " + setRequest.toString());
        webSocket.send(setRequest.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close(1000, "Closing connection");
        client.dispatcher().executorService().shutdown();
    }
}
