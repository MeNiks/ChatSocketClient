package com.niks.chatappclient;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.niks.baseutils.CustomLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import static com.niks.chatappclient.R.id.disconnect;

public class MainActivity extends AppCompatActivity {

    static final int SocketServerPORT = 8080;

    LinearLayout loginPanel, chatPanel;

    EditText editTextUserName, editTextAddress;
    Button buttonConnect;
    TextView chatMsg, textPort;

    EditText editTextSay;
    Button buttonSend;
    Button buttonDisconnect;

    String msgLog = "";

    ChatClientThread chatClientThread = null;
    private String TAG=MainActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();
        CustomLogger.enable_logs=true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginPanel = (LinearLayout) findViewById(R.id.loginpanel);
        chatPanel = (LinearLayout) findViewById(R.id.chatpanel);

        editTextUserName = (EditText) findViewById(R.id.username);
        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("Port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonDisconnect = (Button) findViewById(disconnect);
        chatMsg = (TextView) findViewById(R.id.chatmsg);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        editTextSay = (EditText) findViewById(R.id.say);
        buttonSend = (Button) findViewById(R.id.send);

        buttonSend.setOnClickListener(buttonSendOnClickListener);
    }

    OnClickListener buttonDisconnectOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (chatClientThread == null) {
                return;
            }
            chatClientThread.disconnect();
        }

    };

    OnClickListener buttonSendOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSay.getText().toString().equals("")) {
                return;
            }

            if (chatClientThread == null) {
                return;
            }

            chatClientThread.sendCommandToServer(editTextSay.getText().toString());
        }

    };

    OnClickListener buttonConnectOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();
            if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Addresse",
                        Toast.LENGTH_LONG).show();
                return;
            }

            msgLog = "";
            chatMsg.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread = new ChatClientThread(textUserName, textAddress, SocketServerPORT);
            chatClientThread.start();
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatClientThread.disconnect();
    }

    private class ChatClientThread extends Thread {
        private Socket socket = null;
        private DataOutputStream dataOutputStream = null;
        private DataInputStream dataInputStream = null;
        private String name;
        private String dstAddress;
        private int dstPort;

        boolean is_connected = true;

        ChatClientThread(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                while (is_connected) {
                    if (dataInputStream.available() > 0) {
                        final String server_reply = dataInputStream.readUTF();
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                sendReplyToUI(server_reply);
                            }
                        });
                    }
                }
            } catch (Exception e) {

            } finally {
               disconnect();

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendCommandToServer(String command_json) {
            try {
                dataOutputStream.writeUTF(command_json);
                dataOutputStream.flush();
            } catch (Exception e) {
                is_connected = false;
            } finally {
            }
        }

        private void disconnect() {
            is_connected = false;
            closeConnection();
        }
        private void closeConnection() {
            try {
                if (socket != null)
                    socket.close();

                if (dataInputStream != null)
                    dataInputStream.close();

                if (dataOutputStream != null)
                    dataOutputStream.close();
            } catch (Exception e) {
            }
        }
    }

    private void sendReplyToUI(String server_reply) {
        CustomLogger.Log(TAG,server_reply);
    }

}
