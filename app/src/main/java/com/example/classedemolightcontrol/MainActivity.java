package com.example.classedemolightcontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class MainActivity extends Activity {

    private static final String STR_CONNECT_TO_SERVER = "Connect to server";
    private static final String STR_DISCONNECT_TO_SERVER = "Disconnect to server";

    Socket mSocket;
    Button btnConnect;
    AutoCompleteTextView txtServerAddress, txtServerPort;
    TextView txtStatus, txtServerAddressLabel;
    private String[] mServersAddress = {"https://taliserver.herokuapp.com", "http://192.168.43.72"};
    private String[] mServersPort = {"80", "3484"};
    ImageView imgStatusIcon, btnLight;
    private boolean isLighting = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btn_connect_to_server);
        imgStatusIcon = findViewById(R.id.img_status_icon);

        txtServerAddress = findViewById(R.id.txt_server_address);
        txtServerPort = findViewById(R.id.txt_server_port);
        txtStatus = findViewById(R.id.txt_status);
        txtServerAddressLabel = findViewById(R.id.txt_server_address_label);

        btnLight = findViewById(R.id.btn_light);

        ArrayAdapter<String> adapter1 = new ArrayAdapter<>
                (this, android.R.layout.select_dialog_item, mServersAddress);
        txtServerAddress.setThreshold(0); //will start working from first character
        txtServerAddress.setAdapter(adapter1);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>
                (this, android.R.layout.select_dialog_item, mServersPort);
        txtServerPort.setThreshold(0); //will start working from first character
        txtServerPort.setAdapter(adapter2);

        addButtonListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addButtonListener() {
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                btnConnect.setEnabled(false);
                if (mSocket == null || !mSocket.connected()) {
                    if (mSocket != null) {
                        mSocket.disconnect();
                        mSocket.close();
                    }
                    try {
                        if (txtServerAddress.getText().toString().toCharArray()[txtServerAddress.getText().toString().length() - 1] == 'm') {
                            mSocket = IO.socket(txtServerAddress.getText().toString());
                        } else {
                            mSocket = IO.socket(txtServerAddress.getText().toString() + ":" + txtServerPort.getText().toString());
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "ConnectString is incorrect", Toast.LENGTH_SHORT).show();
                    }
                    if (mSocket != null) {
                        txtStatus.setText("Status: Connecting...");
                        registerSocketEventCallback();
                        imgStatusIcon.setImageResource(R.drawable.ic_loading);

                        float px = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                12f,
                                getResources().getDisplayMetrics()
                        );
                        RotateAnimation anim = new RotateAnimation(0, 360, px, px);
                        anim.setDuration(600);
                        anim.setRepeatCount(Animation.INFINITE);
                        imgStatusIcon.startAnimation(anim);
                        mSocket.connect();
                    }
                } else {
                    if (mSocket != null && mSocket.connected()) {
                        mSocket.disconnect();
                    }
                }

            }

        });

        btnLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSocket != null && mSocket.connected()) {
                    isLighting = !isLighting;
                    if(isLighting) {
                        mSocket.emit("move", "on");
                        btnLight.setImageResource(R.drawable.idea);
                    } else {
                        mSocket.emit("move", "off");
                        btnLight.setImageResource(R.drawable.lightbulb);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please check connection to server!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void registerSocketEventCallback() {
        mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        txtStatus.setText("Status: Disconnect");
                        imgStatusIcon.setImageResource(R.drawable.ic_disconnected);
                        imgStatusIcon.clearAnimation();
                        btnConnect.setEnabled(true);
                        btnConnect.setText(STR_CONNECT_TO_SERVER);
                    }
                });
                mSocket.close();
            }
        });

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        txtStatus.setText("Status: Connected");
                        btnConnect.setEnabled(true);
                        btnConnect.setText(STR_DISCONNECT_TO_SERVER);
                        imgStatusIcon.setImageResource(R.drawable.ic_connected);
                        imgStatusIcon.clearAnimation();
                    }
                });
            }
        });

        mSocket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        mSocket.close();
                        Toast.makeText(MainActivity.this, "timeout", Toast.LENGTH_SHORT).show();
                        imgStatusIcon.setImageResource(R.drawable.ic_disconnected);
                        imgStatusIcon.clearAnimation();
                        txtStatus.setText("Status: Connect error");
                        btnConnect.setEnabled(true);
                        btnConnect.setText(STR_CONNECT_TO_SERVER);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSocket != null) {
            if(mSocket.connected()) {
                mSocket.disconnect();
                mSocket.close();
            }
            mSocket = null;
        }
    }
}
