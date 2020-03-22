package com.ocfk.d2s.server;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.ErrorCode;
import org.iotivity.base.ObservationInfo;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.RequestHandlerFlag;
import org.iotivity.base.RequestType;
import org.iotivity.base.ResourceProperty;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
public class Light implements OcPlatform.EntityHandler {

    private static final String MSG_ACTION = "com.commax.ocf.smartplug.server.D2D_SMARGPlugServer";

    public static final String DEVICE_TYPE = "oic.d.light";
    public static final String RESOURCE_TYPE = "oic.r.switch.binary";
    public static final String RESOURCE_INTERFACE = "oic.if.a";
    public static final String RESOURCE_URI = "/binaryswitch";


    private static final String VALUE_KEY = "value";
    private boolean mValue;



    private String mResourceUri;                //resource URI
    private String mResourceTypeName;           //resource type name.
    private String mResourceInterface;          //resource interface.
    private OcResourceHandle mResourceHandle;   //resource handle

    private List<OcResourceHandle> mResourceHandleList = new LinkedList<>();

    private OcResourceHandle mSwitchResourceHandle, mCoapcloudconf;

    public boolean ismValue() {
        return mValue;
    }



    public void setmValue(boolean mValue) {
        this.mValue = mValue;
    }

    public boolean getmValue() {
        return mValue;
    }

    boolean needNotify = false;


    public Light(){
        mResourceUri = RESOURCE_URI;
        mResourceTypeName = RESOURCE_TYPE;
        mResourceInterface = RESOURCE_INTERFACE;
        mResourceHandle = null; //this is set when resource is registered
    }

    public void setmState(boolean state) {
        mValue = state;
    }

    public List<OcResourceHandle> getResourceHandle(){
        return mResourceHandleList;
    }

    public synchronized void registerResource() throws OcException {
        if (null == mResourceHandle) {

            OcResourceHandle deviceHandle = OcPlatform.getResourceHandleAtUri(OcPlatform.WELL_KNOWN_DEVICE_QUERY);
            if (deviceHandle != null) {
                OcPlatform.bindTypeToResource(deviceHandle, DEVICE_TYPE);
            } else {
                Log.e(TAG, "failed to get device uri handle!");
                msg("failed to get device uri handle!");
            }



            mSwitchResourceHandle = OcPlatform.registerResource(
                    RESOURCE_URI,
                    mResourceTypeName,
                    mResourceInterface,
                    this,
                    EnumSet.of(ResourceProperty.DISCOVERABLE,
                            ResourceProperty.OBSERVABLE)
            );
            mResourceHandleList.add(mSwitchResourceHandle);



        }
    }




    /**
     * NOTE: This is just xdd sample implementation of entity handler. Entity handler can be
     * implemented in several ways by the manufacturer.
     *
     * @param request
     * @return
     */
    @Override
    public synchronized EntityHandlerResult handleEntity(final OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        if (null == request) {
            msg("Server request is invalid");
            return ehResult;
        }
        // Get the request flags
        EnumSet<RequestHandlerFlag> requestFlags = request.getRequestHandlerFlagSet();
        if (requestFlags.contains(RequestHandlerFlag.INIT)) {
            msg("\t\tRequest Flag: Init");
            ehResult = EntityHandlerResult.OK;
        }
        if (requestFlags.contains(RequestHandlerFlag.REQUEST)) {
            msg("\t\tRequest Flag: Request");
            ehResult = handleRequest(request);
        }
        if (requestFlags.contains(RequestHandlerFlag.OBSERVER)) {
            msg("\t\tRequest Flag: Observer");
            ehResult = handleObserver(request);
        }
        return ehResult;
    }

    private EntityHandlerResult handleRequest(OcResourceRequest request) {
        EntityHandlerResult ehResult = EntityHandlerResult.ERROR;
        // Check for query params (if any)
        Map<String, String> queries = request.getQueryParameters();
        if (!queries.isEmpty()) {
            msg("Query processing is up to entityHandler");
        } else {
            msg("No query parameters in this request");
        }

        for (Map.Entry<String, String> entry : queries.entrySet()) {
            msg("Query key: " + entry.getKey() + " value: " + entry.getValue());
        }

        //Get the request type
        RequestType requestType = request.getRequestType();
        switch (requestType) {
            case GET:
                msg("\t\t\tRequest Type is GET");
                ehResult = handleGetRequest(request);
                break;
            case PUT:
                msg("\t\t\tRequest Type is PUT");
                ehResult = handlePutRequest(request);
                break;
            case POST:
                msg("\t\t\tRequest Type is POST");
                ehResult = handlePostRequest(request);
                break;
            case DELETE:
                msg("\t\t\tRequest Type is DELETE");
                ehResult = handleDeleteRequest();
                break;
        }
        return ehResult;
    }

    private EntityHandlerResult handleGetRequest(final OcResourceRequest request) {
        EntityHandlerResult ehResult;
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        if (mIsSlowResponse) { // Slow response case
            new Thread(new Runnable() {
                public void run() {
                    handleSlowResponse(request);
                }
            }).start();
            ehResult = EntityHandlerResult.SLOW;
        } else { // normal response case.
            response.setResponseResult(EntityHandlerResult.OK);
            response.setResourceRepresentation(getOcRepresentation());
            ehResult = sendResponse(response);
        }
        return ehResult;
    }

    private EntityHandlerResult handlePutRequest(OcResourceRequest request) {
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        setOcRepresentation(request.getResourceRepresentation());
        response.setResourceRepresentation(getOcRepresentation());
        response.setResponseResult(EntityHandlerResult.OK);
        return sendResponse(response);
    }


    private EntityHandlerResult handlePostRequest(OcResourceRequest request) {

        boolean value = false;
        OcResourceResponse response = new OcResourceResponse();

        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        needNotify = update(request);

        setOcRepresentation(request.getResourceRepresentation());
        response.setResourceRepresentation(getOcRepresentation());
        response.setResponseResult(EntityHandlerResult.OK);

        OcRepresentation rep = getOcRepresentation();
        try {
            value = rep.getValue(VALUE_KEY);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to getVale()");
        }

        if (needNotify) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    notifyObservers();
                }
            }).start();
        }


        return sendResponse(response);
    }


    private EntityHandlerResult handleDeleteRequest() {
        try {
            this.unregisterResource();
            return EntityHandlerResult.RESOURCE_DELETED;
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to unregister xdd switch resource");
            return EntityHandlerResult.ERROR;
        }
    }

    private void handleSlowResponse(OcResourceRequest request) {
        sleep(10);
        msg("Sending slow response...");
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        response.setResponseResult(EntityHandlerResult.OK);
        response.setResourceRepresentation(getOcRepresentation());
        sendResponse(response);
    }

    private List<Byte> mObservationIds; //IDs of observes

    private EntityHandlerResult handleObserver(final OcResourceRequest request) {
        ObservationInfo observationInfo = request.getObservationInfo();
        switch (observationInfo.getObserveAction()) {
            case REGISTER:
                if (null == mObservationIds) {
                    mObservationIds = new LinkedList<>();
                }
                mObservationIds.add(observationInfo.getOcObservationId());
                break;
            case UNREGISTER:
                if(mObservationIds!=null)
                    mObservationIds.remove((Byte)observationInfo.getOcObservationId());
                break;
        }
        // Observation happens on a different thread in notifyObservers method.
        // If we have not created the thread already, we will create one here.
        return EntityHandlerResult.OK;
    }


    public void notifyObservers() {
        msg(this.toString());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mIsListOfObservers) {
                        OcResourceResponse response = new OcResourceResponse();
                        response.setResourceRepresentation(getOcRepresentation());
                        OcPlatform.notifyListOfObservers(
                                mResourceHandle,
                                mObservationIds,
                                response);
                    } else {
                        OcPlatform.notifyAllObservers(mResourceHandle);
                    }
                } catch (OcException e) {
                    ErrorCode errorCode = e.getErrorCode();
                    if (ErrorCode.NO_OBSERVERS == errorCode) {
                        msg("No more observers, stopping notifications");
                    }
                    return;
                }
            }
        }).start();

    }


    private EntityHandlerResult sendResponse(OcResourceResponse response) {
        try {
            OcPlatform.sendResponse(response);
            return EntityHandlerResult.OK;
        } catch (OcException e) {
            Log.e(TAG, e.toString());

            e.printStackTrace();

            msg("Failed to send response");
            return EntityHandlerResult.ERROR;
        }
    }

    public synchronized void unregisterResource() throws OcException {
        if (null != mResourceHandle) {
            OcPlatform.unregisterResource(mResourceHandle);
        }
    }

    public boolean update(OcResourceRequest request) {
        setOcRepresentation(request.getResourceRepresentation());
        return true;
    }


    public void setOcRepresentation(OcRepresentation rep) {
        try {
            if (rep.hasAttribute(VALUE_KEY)) mValue = rep.getValue(VALUE_KEY);

        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to get representation values");
        }
    }



    public OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();
        try {
            rep.setValue(VALUE_KEY, mValue);
        } catch (OcException e) {
            Log.e(TAG, e.toString());
            msg("Failed to set representation values");
        }
        return rep;
    }

    //******************************************************************************
    // End of the OIC specific code
    //******************************************************************************

    public void setSlowResponse(boolean isSlowResponse) {
        mIsSlowResponse = isSlowResponse;
    }

    public void useListOfObservers(boolean isListOfObservers) {
        mIsListOfObservers = isListOfObservers;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public String toString() {
        return  "" + "----------------------------------------------------- "+
                "\n\t" + "URI" + ": " + mResourceUri + "," +
                "\t" +    VALUE_KEY + ": " + mValue+
                "\n" + "----------------F------------------------------------- ";
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    private void msg(String text) {
        if (null != mContext) {
            Intent intent = new Intent(MSG_ACTION);
            intent.putExtra("message", text);
            mContext.sendBroadcast(intent);
        }
    }

    private final static String TAG = Light.class.getSimpleName();
    private final static int SUCCESS = 200;
    private boolean mIsSlowResponse = false;
    private boolean mIsListOfObservers = false;
    private Thread mObserverNotifier;
    private Context mContext;
}
