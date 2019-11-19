package com.ocfk.d2s.server;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ocfk.d2s.server.Cloud.ServerManager;

import org.iotivity.base.ModeType;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcPlatformInfo;
import org.iotivity.base.PayloadType;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;
import org.iotivity.ca.CaInterface;
import org.iotivity.ca.OicCipher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.iotivity.base.OcPlatform.Shutdown;

public class D2SActivity extends AppCompatActivity implements OnMsgAddListener{

    ImageView btn_power;
    TextView btn_txt;
    Button btn_setip, btn_signup, btn_signin, btn_signout;

    private final static String TAG = D2SActivity.class.getSimpleName();
    private D2SActivity.MessageReceiver mMessageReceiver = new D2SActivity.MessageReceiver();
    private TextView mConsoleTextView;
    private ScrollView mScrollView;

    PlatformConfig platformConfig;
    private final int REQUEST_LOGIN = 1;

    Light mLight;

    private static final String MSG_ACTION = "com.ocfk.d2s.server.D2SActivity";
    public Context mContext;
    public static D2SActivity _instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d2c_main);
        registerReceiver(mMessageReceiver, new IntentFilter(MSG_ACTION));

        mContext = this;
        _instance = this;


        btn_setip = (Button)findViewById(R.id.btn_setip);
        btn_signup = (Button)findViewById(R.id.btn_signup);
        btn_signin = (Button)findViewById(R.id.btn_signin);
        btn_signout = (Button)findViewById(R.id.btn_signout);

        btn_txt = (TextView)findViewById(R.id.btn_power_txt);

        btn_power = (ImageView)findViewById(R.id.btn_normalpower);
        btn_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.isSelected()) {
                    btn_txt.setText("OFF");
                    v.setSelected(false);
                }
                else {
                    btn_txt.setText("ON");
                    v.setSelected(true);
                }
                changeValue(v);
            }
        });

        mConsoleTextView = (TextView) findViewById(R.id.consoleTextView);
        mConsoleTextView.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mScrollView.fullScroll(View.FOCUS_DOWN);


        startServer();

        mServerManager = new ServerManager(_instance,mContext);
        mServerManager.setOnMsgAddLister(this);

        // mServerManager.initOcPlatform(ModeType.CLIENT_SERVER);

        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        if (null == savedInstanceState) {
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    toggleButton.setEnabled(false);
                    if (!isChecked) {
                        new Thread(new Runnable() {
                            public void run() {
                                startServer();
                            }
                        }).start();
                    } else {
                        new Thread(new Runnable() {
                            public void run() {
                                stopServer();
                            }
                        }).start();
                    }
                }
            });
        } else {
            String consoleOutput = savedInstanceState.getString("consoleOutputString");
            mConsoleTextView.setText(consoleOutput);
            boolean buttonCheked = savedInstanceState.getBoolean("toggleButtonChecked");
            toggleButton.setChecked(buttonCheked);
        }

    }

    public void startServer() {
        Context context = this;
        String filePath = getFilesDir().getPath() + "/";


        platformConfig = new PlatformConfig(
                this,
                context,
                ServiceType.IN_PROC,
                ModeType.CLIENT_SERVER,// //ModeType.SERVER,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );


        msg("Configuring OcPlatform.");
        OcPlatform.Configure(platformConfig);

        registerPlatformInfo();
        setDeviceInfo("LIGHT");


        createResource();

        msg("Waiting for the requests...");
        printLine();

        enableStartStopButton();
    }

    private void stopServer() {
        if (mLight !=null) {
            try {
                mLight.unregisterResource();
            } catch (OcException e) {
                Log.e(TAG, e.toString());
                msg("Failed to unregister a light resource");
            }

            Shutdown();
        }
        mLight =null;

        msg("All created resources have been unregistered");
        printLine();
        enableStartStopButton();
    }

    private void enableStartStopButton() {
        runOnUiThread(new Runnable() {
            public void run() {
                ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
                toggleButton.setEnabled(true);
            }
        });
    }

    private void setDummyInitValue(){
        mLight.setmState(true);
    }


    public void createResource(){
        mLight = new Light();
        String temp;

        mLight.setContext(this);

        setDummyInitValue();

        try {
            mLight.registerResource();
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to register a smartplug resource");
        }


        msg("Create and register mLight as a resource");
        msg(" * DeviceType : "+ mLight.DEVICE_TYPE);
        msg(" * ResourceType: "+ mLight.RESOURCE_TYPE);
        msg(" * ResourceUri "+ mLight.RESOURCE_URI);


        if(mLight.getmValue())
            temp= "on";
        else
            temp="off";

        msg("ResourceType value :" + temp);

    }

    public void publishResourceRD(){
        if(mServerManager!=null)
            mServerManager._publishResourceToRD(mLight.getResourceHandle());
    }

    /** Device Name.*/
    public static final String OC_RSRVD_DEVICE_NAME = "n";
    /** Device specification version.*/
    public static final String OC_RSRVD_SPEC_VERSION = "icv";
    /** Device data model.*/
    public static final String OC_RSRVD_DATA_MODEL_VERSION = "dmv";

    public static void setDeviceInfo(String name) {
        try {
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(),
                    OC_RSRVD_DEVICE_NAME, name);
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(),
                    OC_RSRVD_DATA_MODEL_VERSION, "ocf.res.1.3.1,ocf.sh.1.3.1"); // OC_RSRVD_DATA_MODEL_VERSION
            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(),
                    OC_RSRVD_SPEC_VERSION, "ocf.1.3.1");

            OcPlatform.setPropertyValue(PayloadType.DEVICE.getValue(), "piid", "d3051946-5d1a-4f5a-8d0b-2d01e94241d4");

        } catch (OcException e) {
            e.printStackTrace();
        }
    }

    public static void registerPlatformInfo() {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        s.setTimeZone(TimeZone.getTimeZone("UTC"));
        String systemTime = s.format(new Date());
        Log.d(TAG, "registerPlatformInfo, systemTime: " + systemTime);

        OcPlatformInfo platformInfo = new OcPlatformInfo(
                "d3051946-5d1a-4f5a-8d0b-2d01e94241d4",
                "OCFK",                              // Manufacturer Name
                "http://https://www.ocfk.org",                      // Manufacturer Link
                "OCFKModelNumber",                               // Model Number
                "2019-06-01",                                  // Date of Manufacture
                "1.0",                           // Platform Version
                "android",                     // Operating System Version
                "1.0",                           // Hardware Version
                "1.0",                           // Firmware Version
                "https://www.ocfk.org",               // Support Link
                systemTime
        );

        try {
            OcPlatform.registerPlatformInfo(platformInfo);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }


    private void changeValue(View v){
        if(v.isSelected()){
            if(mLight!=null) {
                mLight.setmValue(true);
                mLight.notifyObservers();
            }
        }else{
            if(mLight!=null) {
                mLight.setmValue(false);
                mLight.notifyObservers();
            }
        }


    }

    ServerManager mServerManager;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_LOGIN) {
            String authCode = data.getStringExtra("authCode");
            msg("\tauthCode: " + authCode);
            mServerManager._mSignUP(data);
        }
    }

    public void mAccountMgrClick(View v){

        if(mServerManager==null)
            return;

        if(v.getId()==R.id.btn_setip){
            mServerManager.showTCPInput();
        }else if(v.getId()==R.id.btn_signup){
            mServerManager.signUp();
        }else if(v.getId()==R.id.btn_signin){
            mServerManager.signIn();
        }else if(v.getId()==R.id.btn_signout){
            mServerManager.signOut();
        }

    }














    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String message = intent.getStringExtra("message");
            msg(message);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent with changes sending broadcast IN ");

        Intent i = new Intent();
        i.setAction(intent.getAction());
        i.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES,
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES));
        sendBroadcast(i);
        Log.d(TAG, "Initialize Context again resetting");
    }



    private void msg(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                mConsoleTextView.append("\n");
                mConsoleTextView.append(text);
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        Log.i(TAG, text);
    }

    private void printLine() {
        msg("-------------------------------------------------------------------------------------------");
    }


    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void message(String s) {
        msg(s);
    }


}
