package com.ocfk.d2s.server.Cloud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ocfk.d2s.server.D2SActivity;
import com.ocfk.d2s.server.OnMsgAddListener;
import com.ocfk.d2s.server.R;

import org.iotivity.base.EncodingType;
import org.iotivity.base.ErrorCode;
import org.iotivity.base.ModeType;
import org.iotivity.base.ObserveType;
import org.iotivity.base.OcAccountManager;
import org.iotivity.base.OcConnectivityType;
import org.iotivity.base.OcException;
import org.iotivity.base.OcHeaderOption;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcPresenceHandle;
import org.iotivity.base.OcProvisioning;
import org.iotivity.base.OcRDClient;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResource;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.PlatformConfig;
import org.iotivity.base.QualityOfService;
import org.iotivity.base.ServiceType;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class ServerManager
{


    static String TAG = "ServerManager";



    Context mContext;

    D2SActivity mActivity;


    private OcAccountManager mAccountManager;
    private String mAccessToken;
    private String mRefreshToken;
    private String mUserUuid;
    private String mGroupId;
    private String mGroupMasterId;
    private String mInviterUserId;
    private String mInviteeUuid;
    private final int REQUEST_LOGIN = 1;
    private final String EOL = System.getProperties().getProperty("line.separator");
    private final Pattern PORT_NUMBER = Pattern.compile("(\\d{1,5})");


    // variables related observer
    private int maxSequenceNumber = 0xFFFFFF;
    private Thread mObserverNotifier;

    private boolean mSecured = false;
    private QualityOfService mQos = QualityOfService.LOW;
    OnMsgAddListener msgLogListner;
    private int credId=0;
    public void setOnMsgAddLister(OnMsgAddListener listener){
        msgLogListner = listener;
    }

    public ServerManager(D2SActivity mActivity, Context context) {
        this.mActivity = mActivity;
        this.mContext = context;
    }



    public OcAccountManager getmAccountManager() {
        return mAccountManager;
    }

    public void showTCPInput() {

        LayoutInflater inflater =(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View inputView = inflater.inflate(R.layout.input, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setView(inputView);

        final RadioGroup radioGroup = (RadioGroup) inputView.getRootView().findViewById(R.id.radioGroup);
        final RadioButton radioIP = (RadioButton) inputView.getRootView().findViewById(R.id.radioIP);
        final EditText editText = (EditText) inputView.getRootView().findViewById(R.id.inputText);
        final CheckBox isSecured = (CheckBox) inputView.getRootView().findViewById(R.id.secured);

        radioGroup.setVisibility(View.VISIBLE);
        isSecured.setVisibility(View.VISIBLE);
        isSecured.setChecked(mSecured);

        StringBuilder sb = new StringBuilder();
        sb.append(Common.TCP_ADDRESS);
        sb.append(Common.PORT_SEPARATOR);
        sb.append(Common.TCP_PORT);
        editText.setText(sb.toString());

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        final String hosts = editText.getText().toString();
                        boolean isValid = false;

                        if (!hosts.isEmpty() && hosts.contains(Common.PORT_SEPARATOR)) {
                            isValid = true;
                        }

                        if (isValid) {
                            final String host[] = hosts.split(Common.PORT_SEPARATOR);
                            mSecured = isSecured.isChecked();

                            if (2 > host.length || !PORT_NUMBER.matcher(host[1]).matches()) {
                                isValid = false;
                            } else if (radioIP.isChecked()) {
                                if (Patterns.IP_ADDRESS.matcher(host[0]).matches()) {
                                    Common.TCP_ADDRESS = host[0];
                                    Common.TCP_PORT = host[1];
                                } else {
                                    isValid = false;
                                }
                            } else {
                                if (Patterns.DOMAIN_NAME.matcher(host[0]).matches()
                                        && !Patterns.IP_ADDRESS.matcher(host[0]).matches()) {
                                    Thread thread = new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                Common.TCP_ADDRESS = InetAddress
                                                        .getByName(host[0]).getHostAddress();
                                                Common.TCP_PORT = host[1];
                                            } catch (UnknownHostException e) {
                                                e.printStackTrace();
                                                msgLogListner.message("Failed to get host address.");
                                            }
                                        }
                                    };
                                    thread.start();
                                    try {
                                        thread.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    isValid = false;
                                }
                            }
                        }

                        if (isValid) {
                            StringBuilder sb = new StringBuilder();
                            if (mSecured) {
                                sb.append(Common.COAPS_TCP);
                            } else {
                                sb.append(Common.COAP_TCP);
                            }
                            sb.append(Common.TCP_ADDRESS);
                            sb.append(Common.PORT_SEPARATOR);
                            sb.append(Common.TCP_PORT);
                            Common.HOST = sb.toString();
                            msgLogListner.message("Set Host : " + Common.HOST);
                        } else {
                            Toast.makeText(mContext, "Invalid activity_smartplug_d2c_main", Toast.LENGTH_SHORT).show();
                            showTCPInput();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }




    public void _mSignUP(Intent data){
       String authCode = data.getStringExtra("authCode");

       //authCode = "529a367d003d0fc31e59";
       try {
           if(mAccountManager!=null)
               mAccountManager.signUp("github", authCode, onSignUp);
           else
               Toast.makeText(mContext,"_mSignUP, mAccountManager is NULL",Toast.LENGTH_LONG).show();
       }catch (OcException e){}
    }




    //when push signUp button
    public void signUp(){


        msgLogListner.message("signUp()");
        try {
            mAccountManager = OcPlatform.constructAccountManagerObject(
                    Common.HOST,
                    //"coap+tcp://192.168.1.140:5683",
                    EnumSet.of(OcConnectivityType.CT_ADAPTER_TCP));
        } catch (OcException e) {
            e.printStackTrace();
        }

        Intent intentLogin = new Intent(mContext, LoginActivity.class);
        mActivity.startActivityForResult(intentLogin, REQUEST_LOGIN);

        /**
         * _mSingUp()이 결국 호출 됨.
         */
    }

    public void signIn() {
        try {
            msgLogListner.message("[singIn] mUserUuid :" + mUserUuid+"AccessToken :" + mAccessToken );
            mAccountManager.signIn(mUserUuid, mAccessToken, onSignIn);
        } catch (OcException e) {
            e.printStackTrace();
        }
    }

    public void signOut() {
        try {
            msgLogListner.message("[signOut] AccessToken :" + mAccessToken );

            mAccountManager.signOut(mAccessToken, onSignOut);
        } catch (OcException e) {
            e.printStackTrace();
        }
    }




    OcAccountManager.OnPostListener onSignUp = new OcAccountManager.OnPostListener() {
        @Override
        public synchronized void onPostCompleted(List<OcHeaderOption> list,
                                                 OcRepresentation ocRepresentation) {
           // msg("signUp was successful");
            msgLogListner.message("signUp was successful");
            try {
                mUserUuid = ocRepresentation.getValue("uid");
                mAccessToken = ocRepresentation.getValue("accesstoken");
                mRefreshToken = ocRepresentation.getValue("refreshtoken");
                String tokenType = ocRepresentation.getValue("tokentype");
                msgLogListner.message("\tuserID: " + mUserUuid);
                msgLogListner.message("\taccessToken: " + mAccessToken);
                msgLogListner.message("\trefreshToken: " + mRefreshToken);
                msgLogListner.message("\ttokenType: " + tokenType);

                if (ocRepresentation.hasAttribute("expiresin")) {
                    int expiresIn = ocRepresentation.getValue("expiresin");
                    msgLogListner.message("\texpiresIn: " + expiresIn);
                }
            } catch (OcException e) {
                Log.e(TAG, e.toString());
            }
        }

        @Override
        public synchronized void onPostFailed(Throwable throwable) {
            msgLogListner.message("[signUp] Failed to signUp");
            if (throwable instanceof OcException) {
                OcException ocEx = (OcException) throwable;
                Log.e(TAG, ocEx.toString());
                ErrorCode errCode = ocEx.getErrorCode();
                msgLogListner.message("Error code: " + errCode);
            }
        }
    };

    OcAccountManager.OnPostListener onSignIn = new OcAccountManager.OnPostListener() {
        @Override
        public synchronized void onPostCompleted(List<OcHeaderOption> list,
                                                 OcRepresentation ocRepresentation) {
            msgLogListner.message("signIn was successful");

            mActivity.publishResourceRD();// publish RD

        }

        @Override
        public synchronized void onPostFailed(Throwable throwable) {
            msgLogListner.message("Failed to signIn");
            if (throwable instanceof OcException) {
                OcException ocEx = (OcException) throwable;
                Log.e(TAG, ocEx.toString());
                ErrorCode errCode = ocEx.getErrorCode();
                msgLogListner.message("Error code: " + errCode);

                if (ErrorCode.UNAUTHORIZED_REQ != errCode) {
                    RefreshToken();
                }
            }
        }
    };

    public void RefreshToken() {

        /*
        try {
            OcResource authResource = OcPlatform.constructResourceObject(CIServer, "/.well-known/ocf/account/tokenrefresh",
                    EnumSet.of(OcConnectivityType.CT_ADAPTER_TCP, OcConnectivityType.CT_IP_USE_V4),
                    false, Arrays.asList("oic.wk.account"), Arrays.asList(OcPlatform.DEFAULT_INTERFACE));
            OcRepresentation rep = new OcRepresentation();

            runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    Toast.makeText(EasysetupActivity.this, "RefreshToken in progress..", Toast.LENGTH_SHORT).show();
                }
            });

            rep.setValue("di", deviceID);
            rep.setValue("granttype", "refresh_token");
            rep.setValue(REFRESHTOKEN, mRefreshtoken);
            rep.setValue("uid", mUserID);
            authResource.post(rep, new HashMap<String, String>(), onRefreshTokenPost);
        }
        catch(OcException e)
        {
            e.printStackTrace();
        }

        Log.d(TAG, "No error while executing login");
        */
    }

    OcAccountManager.OnPostListener onSignOut = new OcAccountManager.OnPostListener() {
        @Override
        public synchronized void onPostCompleted(List<OcHeaderOption> list,
                                                 OcRepresentation ocRepresentation) {
            msgLogListner.message("signOut was successful");
        }

        @Override
        public synchronized void onPostFailed(Throwable throwable) {
            msgLogListner.message("Failed to signOut");
            if (throwable instanceof OcException) {
                OcException ocEx = (OcException) throwable;
                Log.e(TAG, ocEx.toString());
                ErrorCode errCode = ocEx.getErrorCode();
                msgLogListner.message("Error code: " + errCode);
            }
        }
    };




    private OcPresenceHandle mOcPresenceHandle = null;



    private List<OcResourceHandle> mResourceHandleList;
    public void _publishResourceToRD(List<OcResourceHandle> mResourceHandle){
        mResourceHandleList = mResourceHandle;
        publishResourceToRD();
    }

    public void publishResourceToRD() {


        try {
            // Publish Virtual Resource to Resource-Directory.
            Log.d(TAG, "Publish Virtual Resource to Resource-Directory.");
            OcRDClient.publishResourceToRD(
                    Common.HOST, EnumSet.of(OcConnectivityType.CT_ADAPTER_TCP),
                    resourcePublishListener
            );

            // Publish Local Resource to Resource-Directory.
            Log.d(TAG, "Publish Local Resource to Resource-Directory.");
            OcRDClient.publishResourceToRD(
                    Common.HOST, EnumSet.of(OcConnectivityType.CT_ADAPTER_TCP), mResourceHandleList,
                    resourcePublishListener
            );
        } catch (OcException e) {
            Log.e(TAG, e.toString());
        }
    }



    private OcResource mFoundResource = null;

    OcPlatform.OnResourceFoundListener resourceFoundListener =
            new OcPlatform.OnResourceFoundListener() {
                @Override
                public void onResourceFound(OcResource ocResource) {
                    synchronized (mActivity) {
                        final String resourceUri = ocResource.getUri();

                        if (mFoundResource == null) {
                            if (resourceUri.contains(Common.RESOURCE_URI)) {
                                msgLogListner.message("onResourceFound : " + ocResource.getUri());
                                mFoundResource = ocResource;
                            }
                        }
                    }
                }

                @Override
                public void onFindResourceFailed(Throwable throwable, String uri) {
                    synchronized (mActivity) {
                        msgLogListner.message("findResource request has failed");
                    }
                }
            };


    OcRDClient.OnPublishResourceListener resourcePublishListener =
            new OcRDClient.OnPublishResourceListener() {
                @Override
                public void onPublishResourceCompleted(OcRepresentation ocRepresentation) {
                    msgLogListner.message("onPublishResourceCompleted");

                    for (OcRepresentation child : ocRepresentation.getChildren()) {
                        try {
                            msgLogListner.message("\tPublished Resource URI : " + child.getValue("href"));
                        } catch (OcException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onPublishResourceFailed(Throwable throwable) {
                    msgLogListner.message("onPublishResourceFailed has failed");
                }
            };





}
