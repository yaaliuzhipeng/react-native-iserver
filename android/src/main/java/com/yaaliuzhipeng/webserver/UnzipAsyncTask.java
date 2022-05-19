package com.yaaliuzhipeng.webserver;

import android.os.AsyncTask;
import android.util.Log;

import com.example.ServerClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

interface UnzipCallback {
    public abstract void onStart();
    public abstract void onCancelled();
    public abstract void onSuccess();
    public abstract void onError(String e);
}
class UnzipAsyncTask extends AsyncTask<String, Double, String> {
    File zip;
    String dest;
    UnzipCallback callback;
    UnzipAsyncTask(File zip,String dest,UnzipCallback callback) {
        this.zip = zip;
        this.dest = dest;
        this.callback = callback;
    }
    UnzipAsyncTask(String zip,String dest,UnzipCallback callback) {
        this.zip = new File(zip);
        this.dest = dest;
        this.callback = callback;
    }
    UnzipAsyncTask(String zip,UnzipCallback callback) {
        this.zip = new File(zip);
        this.callback = callback;
    }
    UnzipAsyncTask(File zip,UnzipCallback callback) {
        this.zip = zip;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        callback.onStart();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            if(!zip.isFile()){
                throw new Exception("invalid zip file");
            }
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
            ZipEntry entry = zipInputStream.getNextEntry();
            if(dest != null) {
                File destDir = new File(dest);
                if(!destDir.isDirectory()){
                    destDir.mkdirs();
                }
            }
            while (true) {
                if (entry == null) {
                    break;
                }
                File file = new File(ServerClient.Companion.joinPath(dest,entry.getName()));
                if(entry.isDirectory()){
                    file.mkdirs();
                    //Log.i(TAG, "在处理文件夹 => "+file.getAbsolutePath());
                }else{
                    if(entry.getName().contains("/")){
                        String name = entry.getName();
                        int ind = name.lastIndexOf("/");
                        String dirPath = name.substring(0,ind);
                        File dir = new File(dirPath);
                        if(dir.isDirectory()){
                            //Log.i(TAG, "doInBackground: 创建文件夹, "+dirPath);
                            dir.mkdirs();
                        }
                    }
                    file.createNewFile();
                    //Log.i(TAG, "在处理文件 => "+file.getAbsolutePath());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    int count;
                    byte[] buffer = new byte[1024000];
                    while((count = zipInputStream.read(buffer)) != -1) {
                        bufferedOutputStream.write(buffer,0,count);
                    }
                    bufferedOutputStream.close();
                }
                entry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
        } catch (Exception e) {
            Log.i("TAG", "doInBackground 解压异常、"+e.getLocalizedMessage());
            return e.getLocalizedMessage();
        }
        return "success";
    }
    @Override
    protected void onCancelled(String s) {
        callback.onCancelled();
    }
    @Override
    protected void onProgressUpdate(Double... values) {
    }
    @Override
    protected void onPostExecute(String s) {
        if(s.equals("success")){
            callback.onSuccess();
        }else{
            callback.onError(s);
        }
    }
}