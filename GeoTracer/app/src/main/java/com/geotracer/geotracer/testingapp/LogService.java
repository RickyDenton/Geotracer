package com.geotracer.geotracer.testingapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ScrollView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class LogService extends Service {

    public static final String ACTION_BROADCAST = LogService.class.getName();
   private static final String ADV_PARSER = "ExperimentAnalysis";

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    /*
    those parameters are for retrieving periodically the Log
     */
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 2000;

    public class LocalBinder extends Binder {
        LogService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LogService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(this.getClass().getName(), "Service Created");

        /*
        HANDLER USED TO CALL THE listenToLog FUNCTION EVERY delay SECONDS.
         */
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            listenToLog();
        }, delay);



    }


    public void onDestroy(){
        super.onDestroy();

        /*
        STOP THE listenToLog CALLS
         */
        handler.removeCallbacks(runnable);
    }


    @Override
    public IBinder onBind(Intent intent) {

        Log.d(this.getClass().getName(), "Service Bounded");
        return binder;
    }


    /*
    THIS FUNCTION TAKES THE SYSTEM LOGS AND PRINT THEM IN THE LOG WINDOW OF THE TESTING ACTIVITY

    TO ENABLE THE VISUALIZATION OF LOG OF A SPECIFIC TAG IT MUST BE ADDED TO THE cmd STRING AS FOLLOW:

            "TAG_NAME:D" OR I/W/E DEPENDING OF THE LEVEL YOU WANT.
     */

    public void listenToLog(){

        int i = 0;
        Process logcat;
        final StringBuilder log = new StringBuilder();

        try {

            //clean the previous log messages.
            Runtime.getRuntime().exec("logcat -c");


            //takes only the specified tag logs.

            /*
            ONLY THE ESTIMATED DISTANCE IS DISPLAYED IN THE LOG WINDOW
             */


            String cmd = "logcat -d " + ADV_PARSER + ":I" + " *:S";



            logcat = Runtime.getRuntime().exec(cmd);

            BufferedReader br = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
            String line;
            String separator = System.getProperty("line.separator");


            while ((line = br.readLine()) != null) {
                i++;
                log.append(line);
                log.append(separator);
            }

            /*
            if i == 1 it prints only a system message we don't care about
             */
            if(i > 1) {

                /*
                prepare and send back the intent to the Testing Activity
                 */
                Intent intent = new Intent(ACTION_BROADCAST);
                intent.putExtra("LogMessage", log.toString());
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}