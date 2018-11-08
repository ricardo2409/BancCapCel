package com.example.ricardotrevino.banccapcel;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

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

    static TextView tvVoltaje, tvLocalRemoto, tvBloqueo, tvSenal, tvBateria, tvTemperatura;
    Button openButton, closeButton, btnConnect;
    static Button btnDesbloqueo;
    static Switch switchLocalRemoto;
    static RadioButton phase1OpenButton, phase1CloseButton, phase2OpenButton, phase2CloseButton, phase3OpenButton, phase3CloseButton;
    RadioGroup phase1RadioGroup, phase2RadioGroup, phase3RadioGroup;
    boolean connected = false;
    String controlPassword = "OK";
    static Toolbar toolbar;
    boolean boolPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolPassword = false;
        //toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        tvVoltaje = (TextView) findViewById(R.id.voltageTextView);
        tvLocalRemoto = (TextView) findViewById(R.id.tvLocalRemoto);
        tvBloqueo = (TextView) findViewById(R.id.tvBloqueo);
        tvSenal = (TextView) findViewById(R.id.tvSenal);
        tvBateria = (TextView) findViewById(R.id.tvBateria);
        tvTemperatura = (TextView) findViewById(R.id.tvTemperatura);

        switchLocalRemoto = (Switch) findViewById(R.id.switchLocalRemoto);

        openButton = (Button) findViewById(R.id.openButton);
        openButton.setOnClickListener(this);
        closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(this);
        btnDesbloqueo = (Button) findViewById(R.id.btnDesbloqueo);
        btnDesbloqueo.setOnClickListener(this);

        phase1OpenButton = (RadioButton) findViewById(R.id.phase1OpenButton);
        phase1CloseButton = (RadioButton) findViewById(R.id.phase1ClosedButton);
        phase2OpenButton = (RadioButton) findViewById(R.id.phase2OpenButton);
        phase2CloseButton = (RadioButton) findViewById(R.id.phase2ClosedButton);
        phase3OpenButton = (RadioButton) findViewById(R.id.phase3OpenButton);
        phase3CloseButton = (RadioButton) findViewById(R.id.phase3ClosedButton);
        phase1RadioGroup = (RadioGroup) findViewById(R.id.phase1RadioGroup);
        phase2RadioGroup = (RadioGroup) findViewById(R.id.phase2RadioGroup);
        phase3RadioGroup = (RadioGroup) findViewById(R.id.phase3RadioGroup);

        btnConnect.setOnClickListener(this);
        switchLocalRemoto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //Cambio a Local
                    if (connected) {
                        try{
                            System.out.println("Estoy adentro del manon");
                            sendManOn();
                            //tvLocalRemoto.setText("Local");
                            waitMs(1000);

                        }catch(IOException e){
                        }
                    } else {
                        showToast("Bluetooth desconectado");
                    }

                } else {
                    //Cambio a Remoto
                    if (connected) {
                        try{
                            boolPassword = false; //Quita el password cuando se regresa a remoto
                            System.out.println("Estoy adentro del try del manoff");
                            sendManOff();
                            //tvLocalRemoto.setText("Remoto");
                            waitMs(1000);

                        }catch(IOException e){
                        }
                    } else {
                        showToast("Bluetooth desconectado");
                    }

                }
            }
        });


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
        btnConnect.setText("Conectar");
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
        tvLocalRemoto.setText("");
        tvVoltaje.setText("");
        tvBateria.setText("0");
        tvSenal.setText("0");
        tvTemperatura.setText("0");

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
            int phase1State, phase2State, phase3State, paquetes, rssi, senal, bloqueoControl;
            int phase1Transition, phase2Transition, phase3Transition, flagManOn;
            int currentVoltageControl;
            currentVoltage = Float.parseFloat(tokens[1]);
            tvVoltaje.setText(Float.toString(currentVoltage) + " V");
            System.out.println("Se cambió el voltaje a: " + Float.toString(currentVoltage));
            phase1State = Integer.parseInt(tokens[2]);
            //updateLabels(1, phase1State);
            if (phase1State == 0){
                phase1OpenButton.setChecked(true);
                phase1CloseButton.setText("Abierto");

            } else{
                phase1CloseButton.setChecked(true);
                phase1CloseButton.setText("Cerrado");

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

            if (phase2State == 0){
                phase2OpenButton.setChecked(true);
                phase2CloseButton.setText("Abierto");
            } else{
                phase2CloseButton.setChecked(true);
                phase2CloseButton.setText("Cerrado");

            }


            //phase2Transition = Integer.parseInt(tokens[5]);

            phase3State = Integer.parseInt(tokens[6]);
            //updateLabels(3, phase3State);

            if (phase3State == 0){
                phase3OpenButton.setChecked(true);
                phase3CloseButton.setText("Abierto");
            }else{
                phase3CloseButton.setChecked(true);
                phase3CloseButton.setText("Cerrado");
            }

            bloqueoControl = Integer.parseInt(tokens[9]);
            if(bloqueoControl == 1){
                tvBloqueo.setText("Bloqueado");
                btnDesbloqueo.setVisibility(View.VISIBLE);//Mostrar el boton de bloqueo
            }else{
                tvBloqueo.setText("");
                btnDesbloqueo.setVisibility(View.GONE);//quitar el boton de bloqueo

            }

            senal = Integer.parseInt(tokens[12]);
            tvSenal.setText(Integer.toString(senal));
            bateria = Float.parseFloat(tokens[13]);
            tvBateria.setText(Float.toString(bateria) + " V");
            String auxStringTemp = tokens[14].substring(0,4);
            System.out.println("Este es el aux string de temp: " + auxStringTemp);
            if(auxStringTemp.length() < 5){//Validacion porque hay veces en que manda otra cadena pegada y marca error
                //System.out.println("Es menor");
                //temperatura = Float.parseFloat(tokens[14]);
                tvTemperatura.setText(auxStringTemp);
            }else{
                System.out.println("Es mayor");

            }

        }
    }
    void sendOpen() throws IOException
    {
        System.out.println("Estoy en el Open");
        String msg = "$Abrir&";
        outputStream.write(msg.getBytes());
    }
    void sendClose() throws IOException
    {
        System.out.println("Estoy en el Close");
        String msg = "$Cerrar&";
        outputStream.write(msg.getBytes());
    }
    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void sendManOn() throws IOException
    {
        System.out.println("Estoy en el ManOn");
        //manOn = true;
        String msg = "$ManOn&";
        outputStream.write(msg.getBytes());
        //Wait para evitar que se cambie solo
        waitMs(2000);

    }

    void sendManOff() throws IOException
    {
        System.out.println("Estoy en el ManOff");
        //manOn = false;
        String msg = "$ManOff&";
        outputStream.write(msg.getBytes());
        waitMs(2000);
    }
    public static void waitMs(int ms) throws IOException{
        try {
            // thread to sleep for ms milliseconds
            //SystemClock.sleep(ms);
            Thread.sleep(ms);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    void sendPassword(String pass) throws IOException
    {
        System.out.println("El control = Pass");
        //control = "Pass";
        System.out.println("Estoy en el sendPassword");
        String msg = "$PASS=" + pass + ",& ";
        System.out.println("Este es el pass que mando " + msg);
        outputStream.write(msg.getBytes());
    }
    //Input Dialog para ingresar el password para permitir el cambio a remoto
    public void showPasswordDialog(final String title, final String message){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(getApplicationContext());
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
        edittext.setTextColor(getResources().getColor(R.color.black));
        edittext.setRawInputType(Configuration.KEYBOARD_12KEY);
        alert.setMessage(message);
        alert.setTitle(title);
        alert.setView(edittext);

        alert.setPositiveButton("Ingresar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pass = edittext.getText().toString();
                //Pasar el pass con este comando = $PASS=12345,& regresa OK si es correcto o error si incorrecto
                try{
                    sendPassword(pass);
                }catch(IOException e){

                }
                if(controlPassword.matches("OK")){
                    Toast.makeText(getApplicationContext(), "Correcto", Toast.LENGTH_SHORT).show();
                    //eraseColorFromButtons();
                    boolPassword = true;

                }else{
                    showPasswordDialog(title, "Password Inválido");
                    //controlPassword = "ERROR";
                    boolPassword = false;

                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cerrar el input dialog
                dialog.dismiss();
                //Regresate a Remoto

            }
        });

        alert.show();
    }

    public void changeRadioButtonsText(String text){
        phase1CloseButton.setText(text);
        phase2CloseButton.setText(text);
        phase3CloseButton.setText(text);

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
            case R.id.openButton:
                if (connected) {
                    if(boolPassword){
                        try {
                            System.out.println("Botón Abrir");
                            sendOpen();
                        } catch (Exception e) {
                            System.out.println("Error: " + e);
                        }
                    }else{
                        showPasswordDialog("Ingresa el Password" ,"");
                    }

                } else {
                    showToast("Bluetooth desconectado");
                }
                break;
            case R.id.closeButton:
                if(connected) {
                    if(boolPassword){
                        try {
                            System.out.println("Botón Cerrar");
                            sendClose();
                        } catch (Exception e) {
                            System.out.println("Error: " + e);
                        }
                    }else{
                        showPasswordDialog("Ingresa el Password" ,"");
                    }

                } else {
                    showToast("Bluetooth desconectado");
                }
                break;
        }

    }
}
