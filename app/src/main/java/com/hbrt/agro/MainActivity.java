package com.hbrt.agro;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    private OkHttpClient client;
    private WebSocket ws;
    private boolean isAuto = true;

    private EditText ipInput;
    private Button connectBtn;
    private TextView statusText, tempText, humText, soilText, waterText, soilTempText;
    private Button modeBtn, fanBtn, pumpBtn, lightsBtn, heaterBtn, ventBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient();

        ipInput = findViewById(R.id.ipInput);
        connectBtn = findViewById(R.id.connectBtn);
        statusText = findViewById(R.id.statusText);
        tempText = findViewById(R.id.tempText);
        humText = findViewById(R.id.humText);
        soilText = findViewById(R.id.soilText);
        waterText = findViewById(R.id.waterText);
        soilTempText = findViewById(R.id.soilTempText);
        modeBtn = findViewById(R.id.modeBtn);
        fanBtn = findViewById(R.id.fanBtn);
        pumpBtn = findViewById(R.id.pumpBtn);
        lightsBtn = findViewById(R.id.lightsBtn);
        heaterBtn = findViewById(R.id.heaterBtn);
        ventBtn = findViewById(R.id.ventBtn);

        connectBtn.setOnClickListener(v -> connectToESP());
        
        modeBtn.setOnClickListener(v -> sendCmd("auto", !isAuto));
        fanBtn.setOnClickListener(v -> sendCmd("fan", !fanBtn.getText().toString().contains("ON")));
        pumpBtn.setOnClickListener(v -> sendCmd("pump", !pumpBtn.getText().toString().contains("ON")));
        lightsBtn.setOnClickListener(v -> sendCmd("lights", !lightsBtn.getText().toString().contains("ON")));
        heaterBtn.setOnClickListener(v -> sendCmd("heater", !heaterBtn.getText().toString().contains("ON")));
        ventBtn.setOnClickListener(v -> sendCmd("servo", !ventBtn.getText().toString().contains("OPEN")));
    }

    private void connectToESP() {
        String ip = ipInput.getText().toString().trim();
        if (ip.isEmpty()) return;

        if (ws != null) ws.close(1000, "Closing");

        String url = "ws://" + ip + ":81";
        Request request = new Request.Builder().url(url).build();
        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                runOnUiThread(() -> {
                    statusText.setText("CONNECTED");
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(text);
                        isAuto = json.getBoolean("auto");
                        
                        tempText.setText("Temp: " + json.getDouble("temperature") + "°C");
                        humText.setText("Hum: " + json.getDouble("humidity") + "%");
                        soilText.setText("Soil: " + json.getInt("soil") + "%");
                        waterText.setText("Tank: " + json.getInt("water") + "%");
                        soilTempText.setText("Soil Temp: " + json.getDouble("soilTemp") + "°C");

                        boolean fan = json.getBoolean("fan");
                        boolean pump = json.getBoolean("pump");
                        boolean lights = json.getBoolean("lights");
                        boolean heater = json.getBoolean("heater");
                        boolean servo = json.getBoolean("servo");

                        fanBtn.setText("FAN: " + (fan ? "ON" : "OFF"));
                        pumpBtn.setText("PUMP: " + (pump ? "ON" : "OFF"));
                        lightsBtn.setText("LIGHTS: " + (lights ? "ON" : "OFF"));
                        heaterBtn.setText("HEATER: " + (heater ? "ON" : "OFF"));
                        ventBtn.setText("VENT: " + (servo ? "OPEN" : "CLOSED"));

                        boolean lock = isAuto;
                        fanBtn.setEnabled(!lock);
                        pumpBtn.setEnabled(!lock);
                        lightsBtn.setEnabled(!lock);
                        heaterBtn.setEnabled(!lock);
                        ventBtn.setEnabled(!lock);

                        modeBtn.setText(isAuto ? "SWITCH TO MANUAL" : "SWITCH TO AUTO");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                runOnUiThread(() -> {
                    statusText.setText("DISCONNECTED");
                    statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                });
            }
        });
    }

    private void sendCmd(String device, boolean value) {
        if (ws != null) {
            try {
                JSONObject cmd = new JSONObject();
                cmd.put("device", device);
                cmd.put("value", value);
                ws.send(cmd.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ws != null) {
            ws.close(1000, "App closed");
        }
    }
}
