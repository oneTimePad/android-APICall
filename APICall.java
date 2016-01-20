package com.tagger.lie.tag_app;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by lie on 12/31/15.
 */
public class APICall {

    int status;
    private HttpURLConnection con;
    JSONObject request;
    Context ctx;
    String method;
    String call;
    String token = null;
    boolean once = false;

    final int TIMEOUT=10;
    final int TRYTIME=2000;



    public APICall(Context ctx,String method,String call,JSONObject request){
            this.ctx = ctx;
            this.method = method;
            this.call = call;
            this.request = request;
    }


    private class ConnectThread extends HandlerThread{

        Handler mHander;
        boolean Lock;
        int trys = 0;
        boolean connection_status;

        public ConnectThread(){
            super("ConnectThread");
            start();
            mHander = new Handler(getLooper());

        }

        public boolean isLocked(){
            return Lock;
        }


        public void connect(){
            mHander.post(new Runnable() {
                @Override
                public void run() {
                    if(trys==TIMEOUT){
                        connection_status=true;
                        Lock=false;
                        return;
                    }
                    try {
                       con = (HttpURLConnection)(new URL("http://192.168.205.125:2000"+call).openConnection());


                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                        if(token!=null){
                            con.setRequestProperty("Authorization",token);
                        }

                        con.setDoInput(true);
                        con.setDoOutput(true);
                        con.setUseCaches(false);

                        con.setRequestMethod(method);


                        con.connect();

                        OutputStream osC = con.getOutputStream();
                        OutputStreamWriter osW = new OutputStreamWriter(osC, "UTF-8");
                        osW.write(request.toString());
                        osW.flush();
                        osW.close();

                        status = con.getResponseCode();

                        Lock=false;
                        connection_status=true;


                    }
                    catch (ConnectException e){
                        connection_status = false;
                        Lock = false;
                        trys++;
                        if(!once) {
                            try {
                                Thread.sleep(TRYTIME);
                            } catch (InterruptedException v) {
                                Log.e("APICall", v.toString());
                            }
                        }

                    }
                    catch (IOException e){
                        Log.e("APICall ConnectThread", e.toString());
                    }
                }
            });
        }
    }





    public void connect()throws ConnectException{

        ConnectThread t = new ConnectThread();
        t.connection_status=false;
        while(!t.connection_status){
            t.Lock = true;
            t.connect();
            while(t.isLocked()) {


            }
            if(once){
                break;
            }

        }
        if(t.trys==TIMEOUT || once){
            throw new ConnectException();
        }






    }

    public void tryOnce(){
        once = true;
    }

    public int getStatus(){


            return status;

    }

    public void authenticate(String token,long expiration_date){


        this.token  ="JWT "+token;


    }



    private  class ResponseThread extends HandlerThread{
        Handler mHandler;
        JSONArray response;
        boolean lock= false;
        ResponseThread(){
            super("Response Thread");
            start();
            mHandler = new Handler(getLooper());

        }

        public boolean isLocked(){
            return lock;
        }

        public void getResponse(){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream in = con.getInputStream();
                        response = new JSONEncoder(in).encodeJSON();

                    }
                    catch(JSONException e){

                        Log.e("APICall",e.toString());
                    }
                    catch(IOException e){
                        Log.e("APICall",e.toString());
                    }
                    lock=false;

                }
            });


        }
    }


    public JSONArray getResponse(){

        ResponseThread r = new ResponseThread();
        r.lock = true;
        r.getResponse();

        while(r.isLocked()){

        }

        return r.response;

    }
}
