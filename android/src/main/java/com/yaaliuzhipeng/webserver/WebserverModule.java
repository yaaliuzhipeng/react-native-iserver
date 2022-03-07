package com.yaaliuzhipeng.webserver;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import fi.iki.elonen.SimpleWebServer;

class StaticServer extends SimpleWebServer {
    public StaticServer(File rootDirectory, int port) throws IOException {
        super("localhost", port, rootDirectory, false);
//        mimeTypes().put();
    }
}

public class WebserverModule extends ReactContextBaseJavaModule {

    private StaticServer server;
    private final String TAG = "s1000";

    public WebserverModule(ReactApplicationContext reactApplicationContext) {

    }

    @Override
    public String getName() {
        return "WebServer";
    }

    //String zipPath, String destinationPath, Callback successCallback,Callback failCallback
    @ReactMethod
    public void unzip(String zipPath, String destinationPath, Callback successCallback, Callback failCallback) {

        File df = new File(destinationPath);
        if (!df.isDirectory() && !df.mkdirs()) {
            failCallback.invoke("unexpected error while create destination dir");
            return;
        }
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipPath)));
            ZipEntry ze;
            ze = zis.getNextEntry();
            while(ze != null) {
                File file = new File(destinationPath + File.separator + ze.getName());
                if(ze.isDirectory()){
                    Log.i(TAG, "在处理文件夹 => "+ze.getName());
                    file.mkdirs();
                }else{
                    Log.i(TAG, "在处理文件 => "+ze.getName());
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fos);
                    int count;
                    byte[] buffer = new byte[10240000];
                    while((count = zis.read(buffer)) != -1) {
                        bufferedOutputStream.write(buffer,0,count);
                    }
                    bufferedOutputStream.close();
                    fos.close();
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        successCallback.invoke(true);
//            failCallback.invoke(e.getMessage());
    }

    //String directoryPath,int port,String indexFileName,int cacheAge, Callback callback
    @ReactMethod
    public void startWithPort(String directoryPath, int port, String indexFileName, int cacheAge, Callback callback) {
        File dirRoot = new File(directoryPath);
        try {
            if (server == null) {
                server = new StaticServer(dirRoot, port);
            }
            server.start();
            callback.invoke(true);
        } catch (IOException e) {
            Log.i("TAG", "startWithPort failed ");
            callback.invoke(false);
        }
    }

}











