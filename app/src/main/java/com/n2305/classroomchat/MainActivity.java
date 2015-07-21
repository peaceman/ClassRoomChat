package com.n2305.classroomchat;

import android.app.ListActivity;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.drafts.Draft_76;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;


public class MainActivity extends ListActivity {
    private ArrayAdapter<String> mChatListAdapter;
    private WebSocketClient mWebSocketClient;
    private EditText mChatInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupChatInput();

        mChatListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        setListAdapter(mChatListAdapter);
    }

    private void setupChatInput() {
        mChatInput = (EditText)findViewById(R.id.chatInput);

        mChatInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            mWebSocketClient.send(mChatInput.getText().toString());
                            mChatInput.setText("");
                            mChatInput.clearFocus();
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
                final String msg = message;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChatListAdapter.add(msg);
                    }
                });
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
