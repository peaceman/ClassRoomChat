package com.n2305.classroomchat;

import android.app.ListActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SimpleAdapter;

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


public class MainActivity extends ListActivity {
    private SimpleAdapter mChatListAdapter;
    private WebSocketClient mWebSocketClient;
    private EditText mChatInput;
    private List<HashMap<String, String>> chatEntries = new ArrayList<HashMap<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupChatInput();

        mChatListAdapter = new SimpleAdapter(
                this, chatEntries, R.layout.chat_list_item,
                new String[]{"Time", "Content"},
                new int[]{R.id.time, R.id.content}
        );
        setListAdapter(mChatListAdapter);
    }

    private void setupChatInput() {
        mChatInput = (EditText) findViewById(R.id.chatInput);

        mChatInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            mWebSocketClient.send(mChatInput.getText().toString());
                            mChatInput.setText("");
                            return true;
                    }
                }

                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        openWebSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }
    }

    private void openWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://peacedesk.n:1338/chat");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

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

                    final HashMap<String, String> messageMap = new HashMap<String, String>();
                    messageMap.put("Time", String.format("%02d:%02d:%02d", time.hour, time.minute, time.second));
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
            }

            @Override
            public void onError(Exception e) {
                Log.i("WebSocket", "Error " + e.getMessage());
            }
        };

        mWebSocketClient.connect();
        Log.i("WebSocket", "Started connection thread");
    }
}
