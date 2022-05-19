package com.yaaliuzhipeng.webserver;


import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ServerClient;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.util.UUID;

public class WebserverModule extends ReactContextBaseJavaModule {

    private ReactApplicationContext context;
    private final String ZIP_EVENT = "ZIPEVENT";
    private final String SERVER_EVENT = "SERVEREVENT";
    private ServerClient serverClient;

    WebserverModule(ReactApplicationContext context) {
        this.context = context;
    }

    @NonNull
    @Override
    public String getName() {
        return "WebServer";
    }

    public void emit(String eventName, Object data) {
        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Set up any upstream listeners or background tasks as necessary
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // stop background tasks
    }

    @ReactMethod
    public void startWithPort(String directoryPath, int port, String indexFileName, int cacheAge, Callback callback) {
        if(serverClient == null) {
            serverClient = new ServerClient();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    serverClient.launchWithPort(port,directoryPath,indexFileName);
                }
            }).start();
            callback.invoke(true);
        }else{
            callback.invoke(false);
        }
    }

    @ReactMethod
    public void unzip(String zip, String dest, Callback onError) {
        if (zip == null) {
            onError.invoke("invalid zip file path");
        }
        Log.i("TAG", "unzip: "+zip+"\n"+dest+"\n");
        UUID id = UUID.randomUUID();
        UnzipAsyncTask task = new UnzipAsyncTask(new File(zip), dest, new UnzipCallback() {
            @Override
            public void onStart() {
                WritableMap map = Arguments.createMap();
                map.putString("event", "onStart");
                map.putString("id", id.toString());
                emit(ZIP_EVENT, map);
            }

            @Override
            public void onCancelled() {
                WritableMap map = Arguments.createMap();
                map.putString("event", "onCancelled");
                map.putString("id", id.toString());
                emit(ZIP_EVENT, map);
            }

            @Override
            public void onSuccess() {
                WritableMap map = Arguments.createMap();
                map.putString("event", "onSuccess");
                map.putString("id", id.toString());
                emit(ZIP_EVENT, map);
            }

            @Override
            public void onError(String e) {
                WritableMap map = Arguments.createMap();
                map.putString("event", "onError");
                map.putString("id", id.toString());
                map.putString("message", e);
                emit(ZIP_EVENT, map);
            }
        });
        task.execute();
    }

    @ReactMethod
    public void stop(){
        if(serverClient != null){
            serverClient.stop();
        }
    }

    @ReactMethod
    public void isRunning(Callback callback){
        if(serverClient == null){
            callback.invoke(false);
        }else{
            callback.invoke(serverClient.isRunning());
        }
    }

}
