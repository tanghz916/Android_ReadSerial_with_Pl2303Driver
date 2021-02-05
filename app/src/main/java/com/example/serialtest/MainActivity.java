package com.example.serialtest;

import tw.com.prolific.driver.pl2303g.PL2303GDriver;
import tw.com.prolific.driver.pl2303g.PL2303GDriver.DataBits;
import tw.com.prolific.driver.pl2303g.PL2303GDriver.FlowControl;
import tw.com.prolific.driver.pl2303g.PL2303GDriver.Parity;
import tw.com.prolific.driver.pl2303g.PL2303GDriver.StopBits;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;

public class MainActivity extends AppCompatActivity {

    // debug settings
    // private static final boolean SHOW_DEBUG = false;
    private static final boolean SHOW_DEBUG = true;

    // Defines of Display Settings
    private static final int DISP_CHAR = 0;

    // Linefeed Code Settings
    // private static final int LINEFEED_CODE_CR = 0;
    private static final int LINEFEED_CODE_CRLF = 1;
    private static final int LINEFEED_CODE_LF = 2;

    String TAG = "PL2303G_APLog";

    private Button btRead;
    private Button btStop;

    private TextView tvRead;
    private TextView tvStatus;
    private TextView tvData;

    private int readFlag;

    private String readText;

    PL2303GDriver mSerial;

    //BaudRate.B4800, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS
    private PL2303GDriver.BaudRate mBaudrate = PL2303GDriver.BaudRate.B9600;
    private PL2303GDriver.DataBits mDataBits = PL2303GDriver.DataBits.D8;
    private PL2303GDriver.Parity mParity = PL2303GDriver.Parity.NONE;
    private PL2303GDriver.StopBits mStopBits = PL2303GDriver.StopBits.S1;
    private PL2303GDriver.FlowControl mFlowControl = PL2303GDriver.FlowControl.OFF;

    private static final String ACTION_USB_PERMISSION = "com.example.serialtest.USB_PERMISSION";
    private static final String NULL = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Enter onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /// show weight
        tvRead   = (TextView) findViewById(R.id.WeightView);

        tvStatus = (TextView) findViewById(R.id.StatusView);
        tvData   = (TextView) findViewById(R.id.DataView);
        tvData.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String[] dataBin = readText.split(",");

                if(0 == dataBin[0].compareTo("ST")){
                    tvStatus.setText("稳定");
                }
                else if(0 == dataBin[0].compareTo("US"))
                {
                    tvStatus.setText("不稳定");
                }

                tvRead.setText(dataBin[2]);

            }
        });


        /// start button
        btRead = (Button) findViewById(R.id.startButton);
        btRead.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                readFlag = 1;

                //readDataFromSerial();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true){

                            if(1 == readFlag){
                                readDataFromSerial();
                            }
                        }
                    }
                }).start();
            }
        });

        btStop = (Button)findViewById(R.id.stopButton);
        btStop.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                readFlag = 0;
                mSerial.end();
                tvStatus.setText("未连接");
            }
        });

        // get service
        mSerial = new PL2303GDriver((UsbManager) getSystemService(Context.USB_SERVICE),
                this, ACTION_USB_PERMISSION);

        // check USB host function.
        if (!mSerial.PL2303USBFeatureSupported()) {

            Toast.makeText(this, "No Support USB host API", Toast.LENGTH_SHORT)
                    .show();

            Log.d(TAG, "No Support USB host API");

            mSerial = null;
        }

        if( !mSerial.enumerate() ) {
            Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();
        }

        try {
            Thread.sleep(1500);
            openUsbSerial();
        } catch (Exception e) {
            e.printStackTrace();
        }

        readFlag = 1;

        Log.d(TAG, "Leave onCreate");
    }//onCreate


    private void openUsbSerial() {
        Log.d(TAG, "Enter  openUsbSerial");

        if(mSerial==null) {
            Log.d(TAG, "No mSerial");
            return;
        }

        if (mSerial.isConnected()) {
            if (SHOW_DEBUG) {
                Log.d(TAG, "openUsbSerial : isConnected ");
            }

            mBaudrate = PL2303GDriver.BaudRate.B9600;

            Log.d(TAG, "baudRate:9600");

            if (!mSerial.InitByBaudRate(mBaudrate,700)) {
                if(!mSerial.PL2303Device_IsHasPermission()) {
                    Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
                }

                if(mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
                    Toast.makeText(this, "cannot open, maybe this chip has no support, please use PL2303G chip.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "cannot open, maybe this chip has no support, please use PL2303G chip.");
                }
            } else {

                Toast.makeText(this, "connected : OK" , Toast.LENGTH_SHORT).show();
                Log.d(TAG, "connected : OK");
                Log.d(TAG, "Exit  openUsbSerial");

            }
        }//isConnected
        else {
            Toast.makeText(this, "Connected failed, Please plug in PL2303 cable again!" , Toast.LENGTH_SHORT).show();
            Log.d(TAG, "connected failed, Please plug in PL2303 cable again!");

        }
    }//openUsbSerial


    private void readDataFromSerial() {

        int len;
        byte[] rbuf = new byte[4096];
        //byte[] rbuf = new byte[1024];

        if(null==mSerial)
            return;

        if(!mSerial.isConnected())
            return;

        len = mSerial.read(rbuf);
        if(len<0) {
            Log.d(TAG, "Fail to bulkTransfer(read data)");
            return;
        }

        if (len > 0) {
            if (SHOW_DEBUG) {
                Log.d(TAG, "read len : " + len);
            }
            //rbuf[len] = 0;
            for (int iByte = 0; iByte<len - 17; iByte++){
                if ((('S' == rbuf[iByte]) && ('T' == rbuf[iByte + 1])) || (('U' == rbuf[iByte])&&('S' == rbuf[iByte + 1])) ){

                    if(iByte + 17 > len)
                    {
                        break;
                    }

                    if((0x0D != rbuf[iByte + 16]) || (0x0A != rbuf[iByte + 17])){
                        continue;
                    }

                    readText = new String(rbuf, iByte,18);
                    tvData.setText(readText);

                    break;
                }
            }
        }
        else {
            if (SHOW_DEBUG) {
                Log.d(TAG, "read len : 0 ");
            }
            //tvData.append("empty");
            return;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Leave readDataFromSerial");
    }//readDataFromSerial





}
