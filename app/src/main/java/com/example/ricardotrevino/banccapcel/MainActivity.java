package com.example.ricardotrevino.banccapcel;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static BluetoothDevice device;
    public static BluetoothSocket socket;
    public static OutputStream outputStream;
    public static InputStream inputStream;
    static boolean socketConectado;
    boolean stopThread;
    static String s;

    static String a;
    static String b;

    static Thread thread;
    static Handler handler = new Handler();
    static String tokens[];

    static TextView tvVoltaje, tvLocalRemoto;
    Button openButton, closeButton, btnConnect;
    static Switch switchLocalRemoto;
    static RadioButton phase1OpenButton, phase1CloseButton, phase2OpenButton, phase2CloseButton, phase3OpenButton, phase3CloseButton;
    RadioGroup phase1RadioGroup, phase2RadioGroup, phase3RadioGroup;
    boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        tvVoltaje = (TextView) findViewById(R.id.voltageTextView);
        tvLocalRemoto = (TextView) findViewById(R.id.tvLocalRemoto);
        switchLocalRemoto = (Switch) findViewById(R.id.switchLocalRemoto);


        btnConnect.setOnClickListener(this);


    }
    //Identifica el device BT
    public boolean BTinit()
    {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Este dispositivo no soporta bluetooth", Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);
            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Favor de conectar un dispositivo", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {

                //Suponiendo que solo haya un bondedDevice
                device = iterator;
                found = true;
                //Toast.makeText(getApplicationContext(), "Conectado a: " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
        return found;
    }
    //Conexión al device BT
    public boolean BTconnect()
    {
        try
        {
            conectar();

        }
        catch(IOException e)
        {
            Toast.makeText(getApplicationContext(), "Conexión no exitosa", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            connected = false;
        }

        return connected;
    }

    public void conectar() throws IOException{
        socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Crea un socket para manejar la conexión
        socket.connect();
        socketConectado = true;
        Log.d("Socket ", String.valueOf(socket.isConnected()));
        Toast.makeText(getApplicationContext(), "Conexión exitosa", Toast.LENGTH_SHORT).show();
        connected = true;
        //tvConect.setText("Conectado a " + device.getName());
        btnConnect.setText("Desconectar");
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        beginListenForData();

    }

    public void desconectarBluetooth() throws IOException{
        //Desconectar bluetooth
        if(socketConectado){
            System.out.println("Socket Conectado");
            outputStream.close();
            outputStream = null;
            inputStream.close();
            inputStream = null;
            socket.close();
            socket = null;
        }
        resetFields();
        connected = false;
        //tvConect.setText("");
        btnConnect.setText("Conectar a módulo Bluetooth");
        device = null;
        stopThread = true;
        socketConectado = false;

    }

    public void resetFields(){
        resetVoltaje();
    }

    public void resetVoltaje(){
        phase1RadioGroup.clearCheck();
        phase2RadioGroup.clearCheck();
        phase3RadioGroup.clearCheck();
        switchLocalRemoto.setChecked(false);
    }
    void beginListenForData() {
        stopThread = false;
        thread = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        //waitMs(1000);
                        final int byteCount = inputStream.available();
                        if(byteCount > 0) {
                            byte[] packetBytes = new byte[byteCount];
                            inputStream.read(packetBytes);
                            s = new String(packetBytes);
                            System.out.println("Linea: " + s);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //Status
                                    if(s.contains("s,") && s.contains("&")){
                                        if(s.length() >= 37 ){
                                            a = s.substring(s.indexOf("s,"), s.length() - 1);
                                            System.out.println("A: " + a);
                                            readMessage(a);
                                        }
                                    }
                                }
                            });
                        }
                    }
                    catch (IOException ex) {
                        stopThread = true;
                    }
                }
                System.out.println("Stop thread es true");
            }
        });
        thread.start();
    }
    public static void readMessage(String frase)  {
        System.out.println("Estoy en el readMessage: ");
        System.out.println(frase);
        tokens = frase.split(",");
        System.out.println("Tokens: " + Arrays.toString(tokens));
        if (frase.contains("s") && tokens.length >= 8) {
            //System.out.println("Contains Status : ");
            float currentVoltage, bateria, temperatura;
            int phase1State, phase2State, phase3State, paquetes, rssi, senal;
            int phase1Transition, phase2Transition, phase3Transition, flagManOn;
            int currentVoltageControl;
            currentVoltage = Float.parseFloat(tokens[1]);
            tvVoltaje.setText(Float.toString(currentVoltage) + " V");
            System.out.println("Se cambió el voltaje a: " + Float.toString(currentVoltage));
            phase1State = Integer.parseInt(tokens[2]);
            //updateLabels(1, phase1State);
            if (phase1State == 0){
                phase1OpenButton.setChecked(true);
            } else{
                phase1CloseButton.setChecked(true);
            }
            flagManOn = Integer.parseInt(tokens[3]);
            if(flagManOn == 1){
                //System.out.println("El BT dice que el ManOn es True");
                //manOn = true;
                //voltFrag.btnManOn.setText(R.string.ManOff);
                //enableButtons();
                switchLocalRemoto.setChecked(true);
                tvLocalRemoto.setText("Local");

            }else{
                //System.out.println("El BT dice que el ManOn es False");
                //manOn = false;
                //voltFrag.btnManOn.setText(R.string.ManOn);
                //disableButtons();
                switchLocalRemoto.setChecked(false);
                tvLocalRemoto.setText("Remoto");

            }

            phase2State = Integer.parseInt(tokens[4]);
            //updateLabels(2, phase2State);

            if (phase2State == 0)
                phase2OpenButton.setChecked(true);
            else
                phase2CloseButton.setChecked(true);

            //phase2Transition = Integer.parseInt(tokens[5]);

            phase3State = Integer.parseInt(tokens[6]);
            //updateLabels(3, phase3State);

            if (phase3State == 0)
                phase3OpenButton.setChecked(true);
            else
                phase3CloseButton.setChecked(true);



            //waitAndEraseLabels();

        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnConnect:
                if(!connected) {
                    if (BTinit()) {

                        BTconnect();
                        /*
                        try{
                            askGPS();
                        }catch(IOException e){

                        }
                        */

                    }
                }else{
                    try
                    {
                        desconectarBluetooth();
                    }
                    catch (IOException ex) { }
                }
                break;
        }

    }
}
