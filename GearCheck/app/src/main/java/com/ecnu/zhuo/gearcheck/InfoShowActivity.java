package com.ecnu.zhuo.gearcheck;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class InfoShowActivity extends AppCompatActivity {

    MyBroadcastReceiver mBroadcastReciver;

   // Button btnOn, btnOff, btnDis;
    Button On, Off, Discnt;
    TextView Info;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private InputStream inStream = null;
    private OutputStream outStream = null;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        registerBroadcastReceiver();
        Intent newint = getIntent();
        address = newint.getStringExtra("BLE_MAC"); //receive the address of the bluetooth device
        Log.i("address",address);
        //view of the ledControl
        setContentView(R.layout.activity_info_show);

        //call the widgets
        On = findViewById(R.id.on_btn);
        Off =findViewById(R.id.off_btn);
        Discnt = findViewById(R.id.dis_btn);
        Info = findViewById(R.id.info);


        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        On.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });


    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("0".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        Log.i("click","click");
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("1".getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s, Toast.LENGTH_LONG).show();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
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



    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(InfoShowActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
                try
                {
                    inStream = btSocket.getInputStream();
                    outStream = btSocket.getOutputStream();
                }
                catch (IOException e)
                {
                    msg("Error");
                }
                new InfoThread().start();
                try
                {
                    outStream.write("1".getBytes());
                }
                catch (IOException e)
                {
                    msg("Error");
                }
            }
            progress.dismiss();
        }
    }
    private class InfoThread extends Thread{
        @Override
        public void run() {
            Log.i("run","run");
            super.run();
            byte buffer[] = new byte[1024];
            byte[] buf = new byte[1024];
            int bufLen = 0;
            int bytes;
            while (true){
                try {
                    bytes = inStream.read(buffer);
                    if (bytes > 0){
                        System.arraycopy(buffer, 0, buf, bufLen, bytes);
                        bufLen += bytes;
                        String end = new String(buf, bufLen-1,1);
                        Log.e("end",end);
                        if(end.equals("n")){
                            String params_str = new String(buf,0,bufLen);
                            Intent intent = new Intent();
                            intent.putExtra("params",params_str);
                            intent.setAction(Constant.ACTION_INFO_UPDATE);
                            Context context = getApplicationContext();
                            context.sendBroadcast(intent);
                            bufLen = 0;
                        }

                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    void registerBroadcastReceiver() {
        if (mBroadcastReciver == null) {
            mBroadcastReciver = new MyBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constant.ACTION_INFO_UPDATE);
            registerReceiver(mBroadcastReciver, filter);
        }
    }

    void unregisterBroadcastReceiver() {
        if (mBroadcastReciver != null) {
            unregisterReceiver(mBroadcastReciver);
            mBroadcastReciver = null;
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constant.ACTION_INFO_UPDATE)) {
                String params_str = intent.getStringExtra("params");
                String num1 = params_str.substring(0,params_str.indexOf("a"));
                String num2 = params_str.substring(params_str.indexOf("a")+1,params_str.indexOf("b"));
                String num3 = params_str.substring(params_str.indexOf("b")+1,params_str.indexOf("c"));
                String num4 = params_str.substring(params_str.indexOf("c")+1,params_str.indexOf("d"));
                String num5 = params_str.substring(params_str.indexOf("d")+1,params_str.indexOf("e"));
                String num6 = params_str.substring(params_str.indexOf("e")+1,params_str.indexOf("n"));
                Info.setText(num1+num2+num3+num4+num5+num6);
            }

        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
    }
}
