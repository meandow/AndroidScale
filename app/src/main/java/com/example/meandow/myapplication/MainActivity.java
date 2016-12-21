package com.example.meandow.myapplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    Button Zero, Add, Send, Reset, Knapp1, Knapp2;
    Button btnClosePopup;
    Button btnDeletePopup;
    Button btnSavePopup;
    TextView sensorView, totalView;
    Handler bluetoothIn;
    int totalen = 0;
    int nummer = 0;
    int data = 0;
    int last =0;
    public Spinner gaitSpinner, materialSpinner;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    public String Kund="";
    public String Plats="";
    public String Material="";
    public String Datum="";
    static Handler mHandler;
    public EditText editText_plats, editText_kund;
    // Seriellt UUID nummer (måste användas)
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String för address till vald enhet
    private static String address;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //Initierar knappar och textfönster
        sensorView = (TextView) findViewById(R.id.sensorView);
        totalView = (TextView) findViewById(R.id.totalView);
        Zero = (Button) findViewById(R.id.button);
        Add = (Button) findViewById(R.id.button2);
        Send = (Button) findViewById(R.id.button3);
        Reset = (Button) findViewById(R.id.button4);
        //Knapp1 = (Button) findViewById(R.id.button5);


        //Sätter typsnitt på texten, valfritt
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "digital-7.ttf");
        sensorView.setTypeface(custom_font);
        totalView.setTypeface(custom_font);
        sensorView.setText("00000");

        //Aktiverar listeners för att veta om någon tryckt på knappen
        Zero.setOnClickListener(onZeroListener());
        Add.setOnClickListener(onAddListener());
        Send.setOnClickListener(onSendListener());
        Reset.setOnClickListener(onResetListener());

        //Tvingar den in i landskapsläge, fungerar bäst för min del, inget måste
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mHandler = new Handler();
        //Handler för att uppdatera texten med datan kontinuerligt
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                         //Jämför meddelandet med vad vi vill ha
                    String readMessage = (String) msg.obj;                              // Hämta hem meddelande från objekt till sträng
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");                    // ~ är slutet av meddelandet
                    if (endOfLineIndex > 0) {                                           // Se till så att det finns data innan
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // Plocka ut all data
                        if (recDataString.charAt(0) == '#')                             // # är början av våran data
                        {
                            Log.i("DB", "Full string: " + recDataString);
                          //  String sensor = recDataString.substring(1, endOfLineIndex-1);              //Plocka ut datan från position 1 - 5
                           // nummer = Float.parseFloat(sensor);
                          //  sensorView.setText(""+nummer);
                            int cntr=0;
                            for(int i=1;i<endOfLineIndex;i++){
                                if(recDataString.charAt(i) == '+'){
                                    i = endOfLineIndex;
                                }
                                else{
                                    cntr++;
                                }
                            }
                            String sensor0 = recDataString.substring(1, cntr+1);             //get sensor value from string between indices 1-5
                           // sensorView.setText(sensor0);
                            data = Integer.parseInt(sensor0);
                            last = data - nummer;
                            setViews();
                        }
                        recDataString.delete(0, recDataString.length());                    //rensa all gammal data
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // Aktivera Bluetoothadaptern
        checkBTState();                                         // Kontrollera så att bluetooth är igång på mobilen
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //Skapar en koppling till bluetoothmodulen
    }

    @Override
    public void onResume() {
        super.onResume();

        //Få värden ifrån DeviceList aktiviteten
        Intent intent = getIntent();

        //Sätter address till mac adressen av vald enhet
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //Skapar en bluetooth enhet utav macadressen
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        //Skapar en socket till enheten som skapades ovan
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        //Försöker connecta socketen
        try
        {
            btSocket.connect();
        } catch (IOException e) {               //Misslyckades med felmeddelande i e
            try
            {
                btSocket.close();
            } catch (IOException e2)            //Misslyckades med felmeddelande i e2
            {

            }
        }
        //Skapar en ny tråd för kopplingen mellan mobilen och bluetoothenheten som har hand om all input och output
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
       // mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Stänger ner bluetoothsocketen när appen pausas (bör göras)
            btSocket.close();
        } catch (IOException e2) {                  //Misslyckades med felmeddelande i e2
            //insert code to deal with this
        }
    }
    //Listeners för de olika knapparna
    private View.OnClickListener onZeroListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zero();
            }
        };
    }
    private View.OnClickListener onAddListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        };
    }
    private View.OnClickListener onSendListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        };
    }

    private View.OnClickListener onResetListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        };
    }

    //Metoder för de olika knapparna
    private void zero(){
        Toast.makeText(getBaseContext(), "Nollställt", Toast.LENGTH_LONG).show();
        nummer = data;
    }
    private void add(){
        totalen = totalen + last;
        setViews();
    }
    private void send(){

        initiatePopupWindow2();
    }
    private void reset(){
        Toast.makeText(getBaseContext(), "Resettat", Toast.LENGTH_LONG).show();
        totalen = 0;
        nummer = 0;
        setViews();
    }

    private void setViews(){
            sensorView.setText(String.format("%05d",last));
            totalView.setText(String.format("%05d",totalen));
    }




    //Kollar så att bluetooth är aktiverad på mobilen, om den inte är det frågar den om man vill aktivera
    private void checkBTState() {
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Skapa I/O streams
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

    public PopupWindow pWindow2;
    public void initiatePopupWindow2() {
        try {
            LinearLayout viewGroup = (LinearLayout) findViewById(R.id.popup_element);
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.screen_popup_save, viewGroup);
            editText_kund = (EditText) layout.findViewById(R.id.editText_kund);
            editText_plats = (EditText) layout.findViewById(R.id.editText_plats);
            materialSpinner = (Spinner) layout.findViewById(R.id.spinner_material);
            List<String> material = new ArrayList<String>();
            material.add("Matjord");
            material.add("Fylle");
            material.add("Makadam");
            material.add("Väggrus");
            material.add("Flis");
            material.add("Sten");
            material.add("Sand");
            material.add("Frånharp");
            ArrayAdapter<String> materialAdapter = new ArrayAdapter<String>
                    (this, android.R.layout.simple_spinner_item,material);
            materialAdapter.setDropDownViewResource
                    (android.R.layout.simple_spinner_dropdown_item);
            materialSpinner.setAdapter(materialAdapter);
            btnDeletePopup = (Button) layout.findViewById(R.id.btn_delete_popup);
            btnDeletePopup.setOnClickListener(delete_button_click_listener);
            btnSavePopup = (Button) layout.findViewById(R.id.btn_save_popup);
            btnSavePopup.setOnClickListener(save_button_click_listener);
            pWindow2 = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            int wpx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics());
            int hpx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
            pWindow2.setWidth(wpx);
            pWindow2.setHeight(hpx);
            pWindow2.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private View.OnClickListener delete_button_click_listener = new View.OnClickListener() {
        public void onClick(View v) {
            pWindow2.dismiss();
        }
    };
    private View.OnClickListener save_button_click_listener = new View.OnClickListener() {
        public void onClick(View v) {
            pWindow2.dismiss();
            Kund = editText_kund.getText().toString();
            Plats = editText_plats.getText().toString();
            Material = materialSpinner.getSelectedItem().toString();
            Datum = GetDate();
            upload();
        }
    };
    public String GetDate() {
        Date date = new Date();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String s = formatter.format(date);
        return s;
    }


    public void upload() {
        String fullUrl = "https://docs.google.com/forms/d/1blD3DjEhCN2pITGimggCLnRlDDN2jW8UstLR75m2lxs/formResponse";
        HttpRequest mReq = new HttpRequest();
        String tot = Integer.toString(totalen);
        String data = "" +
                "entry.973849135=" + URLEncoder.encode(Kund) + "&" +
                "entry.1762221396=" + URLEncoder.encode(Plats) + "&" +
                "entry.270833407=" + URLEncoder.encode(Material) + "&" +
                "entry.923168846=" + URLEncoder.encode(tot);
        String response = mReq.sendPost(fullUrl, data);
    }
}
/*
----Upload list ----
Kund
Plats
Material
Vikt
Datum
Tid
 */




