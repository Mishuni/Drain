/*
 *******************************************************************
 *
 * Copyright 2015 Intel Corporation.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.ocfk.d2d.simpleserver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
//overrideLibrary="org.iotivity.base";
import org.iotivity.base.ErrorCode;
import org.iotivity.base.ModeType;
import org.iotivity.base.ObserveType;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;
import org.iotivity.base.OcResourceIdentifier;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.iotivity.base.OcPlatform.Shutdown;

/**
 * SimpleClient
 * <p/>
 * SimpleClient is a sample client app which should be started after the simpleServer is started.
 * It finds resources advertised by the server and calls different operations on it (GET, PUT,
 * POST, DELETE and OBSERVE).
 */
public class SimpleClient extends Activity implements
        OcPlatform.OnResourceFoundListener,
        OcResource.OnGetListener,
        OcResource.OnObserveListener {

    private Map<OcResourceIdentifier, OcResource> mFoundResources = new HashMap<>();
    private OcResource mFoundLightResource = null;
    //local representation of a server's light resource
    private Sensor mSensor = new Sensor();

    private OcConnectivityType adapterFlag = OcConnectivityType.CT_ADAPTER_IP;
    //flags related TCP transport test
    private boolean isRequestFlag = false;
    private boolean isTCPContained = false;
    public int a =0; // initial try
    ImageView waterleakImage;

    /**
     * A local method to configure and initialize platform, and then search for the light resources.
     */

    public void stopClient(){
        //button.setText("again");
        timer.cancel();
        enableStopButton();
        //timer=null;
        //timer.purge();

        //Shutdown();
        //finish();
    }
    private void enableStopButton() {
        runOnUiThread(new Runnable() {
            public void run() {
                //button = (Button) findViewById(R.id.button);
                button.setText("Start");
                //button.setText("Start");
                button.setEnabled(true);
            }
        });
    }

    private void startSimpleClient(OcConnectivityType type) {
        Context context = this;
        adapterFlag = type;

        //mSensor = new Sensor();

        PlatformConfig platformConfig = new PlatformConfig(
                this,
                context,
                ServiceType.IN_PROC,
                ModeType.CLIENT,
                "0.0.0.0", // By setting to "0.0.0.0", it binds to all available interfaces
                0,         // Uses randomly available port
                QualityOfService.LOW
        );
        msg("Configuring platform.");
        OcPlatform.Configure(platformConfig);

        try {
            msg("Finding all resources of type /oic/res.");
            //String requestUri = "/oic/res";
            String requestUri = OcPlatform.WELL_KNOWN_QUERY + "?rt=" + "oic.r.sensor.water";

            OcPlatform.findResource("",
                    requestUri,
                    EnumSet.of(OcConnectivityType.CT_DEFAULT),
                    this
            );
            sleep(1);

            /*Find resource is done twice so that we discover the original resources a second time.
            These resources will have the same uniqueidentifier (yet be different objects),
            so that we can verify/show the duplicate-checking code in foundResource(above);
             */

            OcPlatform.findResource("",
                    requestUri,
                    EnumSet.of(OcConnectivityType.CT_DEFAULT),
                    this
            );

        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to invoke find resource API");
        }

        printLine();
    }

    /**
     * An event handler to be executed whenever a "findResource" request completes successfully
     *
     * @param ocResource found resource
     */
    @Override
    public synchronized void onResourceFound(OcResource ocResource) {
        if (null == ocResource) {
            msg("Found resource is invalid");
            return;
        }

        if (mFoundResources.containsKey(ocResource.getUniqueIdentifier())) {
            msg("Found a previously seen resource again!");
        } else {
            msg("Found resource for the first time on server with ID: " + ocResource.getServerId());
            mFoundResources.put(ocResource.getUniqueIdentifier(), ocResource);
        }

        if (null != mFoundLightResource) {
            if (ocResource.getUri().equals("/WaterValueURI")) {
                if (ocResource.getConnectivityTypeSet().contains(OcConnectivityType.CT_ADAPTER_TCP)) {
                    msg("Found resource which has TCP transport");
                    if (isTCPContained == false)
                    {
                        isTCPContained = true;
                        return;
                    }
                }
            }
            msg("Found another resource, ignoring");
            //mFoundLightResource = ocResource;
            // Call a local method which will internally invoke "get" API on the foundLightResource
            //getLightResourceRepresentation();
            return;

        }
        // Get the resource URI
        String resourceUri = ocResource.getUri();
        // Get the resource host address
        String hostAddress = ocResource.getHost();
        msg("\tURI of the resource: " + resourceUri);
        msg("\tHost address of the resource: " + hostAddress);
        // Get the resource types
        msg("\tList of resource types: ");
        for (String resourceType : ocResource.getResourceTypes()) {
            msg("\t\t" + resourceType);
        }
        msg("\tList of resource interfaces:");
        for (String resourceInterface : ocResource.getResourceInterfaces()) {
            msg("\t\t" + resourceInterface);
        }
        msg("\tList of resource connectivity types:");
        for (OcConnectivityType connectivityType : ocResource.getConnectivityTypeSet()) {
            msg("\t\t" + connectivityType);
        }
        printLine();

        //In this example we are only interested in the water sensor resources
        if (resourceUri.equals("/WaterValueURI")) {
            //Assign resource reference to a global variable to keep it from being
            //destroyed by the GC when it is out of scope.
            if (OcConnectivityType.CT_ADAPTER_TCP == adapterFlag)
            {
                if (ocResource.getConnectivityTypeSet().contains(OcConnectivityType.CT_ADAPTER_TCP))
                {
                    msg("set mFoundLightResource which has TCP transport");
                    mFoundLightResource = ocResource;
                    // Call a local method which will internally invoke "get" API
                    getLightResourceRepresentation();
                    return;
                }
            }
            else
            {
                msg("set mFoundLightResource which has UDP transport");
                mFoundLightResource = ocResource;
                // Call a local method which will internally invoke "get" API on the foundLightResource
                getLightResourceRepresentation();
            }
        }
    }

    @Override
    public synchronized void onFindResourceFailed(Throwable throwable, String uri) {
        msg("findResource request has failed");
        Log.e(TAG, throwable.toString());
    }

    /**
     * Local method to get representation of a found water sensor resource
     */
    private void getLightResourceRepresentation() {
        //msg("Getting Light Representation...");

        Map<String, String> queryParams = new HashMap<>();
        try {
            // Invoke resource's "get" API with a OcResource.OnGetListener event
            // listener implementation
            //sleep(1);
            mFoundLightResource.get(queryParams, this);
            sleep(2);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Error occurred while invoking \"get\" API");
        }
    }



    Timer timer = new Timer();


    /**
     * An event handler to be executed whenever a "get" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     */
    @Override
    public synchronized void onGetCompleted(List<OcHeaderOption> list,
                                            OcRepresentation ocRepresentation) {
        //msg("GET request was successful");
        //msg("Resource URI: " + ocRepresentation.getUri());

        try {
            //Read attribute values into local representation of a light
            mSensor.setOcRepresentation(ocRepresentation);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to read the attributes of a water sensor resource");
        }
        msg("물 막힘: ");
        msg(mSensor.toString());
        printLine();

        /**
         * get 하면 1초 마다 주기적으로 get을 서버로 전송 함.
         */

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                waterleakImage.setVisibility(View.VISIBLE);
            }
        });


        if(timer!=null) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getLightResourceRepresentation();
                }
            }, 1000, 3000);
        }


        /**
         *  mSensor가 true이면 누수 발생
         */

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mSensor.getValue()){
                    waterleakImage.setImageResource(R.drawable.clean2);
                    msg("청소 중입니다.");
                }else{
                    waterleakImage.setImageResource(R.drawable.normal2);
                }
            }
        });


        enableStartButton();


    }

    /**
     * An event handler to be executed whenever a "get" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onGetFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            Log.e(TAG, ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            //do something based on errorCode
            msg("Error code: " + errCode);
        }
        msg("Failed to get representation of a found water sensor resource");
    }








    /**
     * Local method to start observing this light resource
     */
    private void observeFoundLightResource() {
        try {
            sleep(1);
            // Invoke resource's "observe" API with a observe type, query parameters and
            // OcResource.OnObserveListener event listener implementation
            mFoundLightResource.observe(ObserveType.OBSERVE, new HashMap<String, String>(), this);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Error occurred while invoking \"observe\" API");
        }
    }

    // holds current number of observations
    private static int mObserveCount = 0;

    /**
     * An event handler to be executed whenever a "post" request completes successfully
     *
     * @param list             list of the header options
     * @param ocRepresentation representation of a resource
     * @param sequenceNumber   sequence number
     */
    @Override
    public synchronized void onObserveCompleted(List<OcHeaderOption> list,
                                                OcRepresentation ocRepresentation,
                                                int sequenceNumber) {

        if (sequenceNumber != OcResource.OnObserveListener.MAX_SEQUENCE_NUMBER + 1)
        {
            msg("OBSERVE Result:");
            msg("\tSequenceNumber:" + sequenceNumber);
            try {
                mSensor.setOcRepresentation(ocRepresentation);
            } catch (OcException e) {
                Log.e(TAG, e.toString());
                msg("Failed to get the attribute values");
            }
            msg(mSensor.toString());

            if ((++mObserveCount) == 11) {
                msg("Cancelling Observe...");
                try {
                    mFoundLightResource.cancelObserve(QualityOfService.HIGH);
                } catch (OcException e) {
                    Log.e(TAG, e.toString());
                    msg("Error occurred while invoking \"cancelObserve\" API");
                }

                sleep(10);
                resetGlobals();
                if (true == isTCPContained && false == isRequestFlag)
                {
                    msg("Start TCP test...");
                    startSimpleClient(OcConnectivityType.CT_ADAPTER_TCP);
                    isRequestFlag = true;
                    return;
                } else if (true == isRequestFlag)
                {
                    msg("End TCP test...");
                    isRequestFlag = false;
                }

                msg("DONE");
                //prepare for the next restart of the SimpleClient
                enableStartButton();
            }
        }
    }

    /**
     * An event handler to be executed whenever a "observe" request fails
     *
     * @param throwable exception
     */
    @Override
    public synchronized void onObserveFailed(Throwable throwable) {
        if (throwable instanceof OcException) {
            OcException ocEx = (OcException) throwable;
            Log.e(TAG, ocEx.toString());
            ErrorCode errCode = ocEx.getErrorCode();
            //do something based on errorCode
            msg("Error code: " + errCode);
        }
        msg("Observation of the found water sensor resource has failed");
    }

    //******************************************************************************
    // End of the OIC specific code
    //******************************************************************************

    private final static String TAG = SimpleClient.class.getSimpleName();
    private TextView mConsoleTextView;
    private ScrollView mScrollView;
    Button button;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_client);

        mConsoleTextView = (TextView) findViewById(R.id.consoleTextView);
        mConsoleTextView.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mScrollView.fullScroll(View.FOCUS_DOWN);
        button = (Button) findViewById(R.id.button);


        waterleakImage = (ImageView)findViewById(R.id.img_water);
        waterleakImage.setImageResource(R.drawable.clean);

        if (null == savedInstanceState) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //button.setText("Stop");
                    button.setEnabled(false);
                    new Thread(new Runnable() {
                        public void run() {
                            isTCPContained = false;
                            if(button.getText().equals("Start")){
                                if(a==0){
                                    a++;
                                    startSimpleClient(OcConnectivityType.CT_ADAPTER_IP);
                                }{
                                    startSimpleClient(OcConnectivityType.CT_ADAPTER_IP);
                                }
                               }
                            else
                                //button.setText("Start");
                                stopClient();
                        }
                    }).start();
                }
            });
        } else {
            String consoleOutput = savedInstanceState.getString("consoleOutputString");
            mConsoleTextView.setText(consoleOutput);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("consoleOutputString", mConsoleTextView.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String consoleOutput = savedInstanceState.getString("consoleOutputString");
        mConsoleTextView.setText(consoleOutput);
    }

    private void enableStartButton() {
        runOnUiThread(new Runnable() {
            public void run() {
                button = (Button) findViewById(R.id.button);
                button.setText("Stop");
                //button.setText("Start");
                button.setEnabled(true);
            }
        });
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
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
        msg("------------------------------------------------------------------------");
    }

    private synchronized void resetGlobals() {
        mFoundLightResource = null;
        mFoundResources.clear();

        mObserveCount = 0;
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

}
