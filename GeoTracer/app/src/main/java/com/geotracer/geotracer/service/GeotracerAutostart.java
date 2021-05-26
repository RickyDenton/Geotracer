package com.geotracer.geotracer.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
This class is used to automatically start the GeoTracer service after:

1) The device has finished booting (android.intent.action.BOOT_COMPLETED)
2) The app has been reinstalled
3) The app has been updated
*/
public class GeotracerAutostart extends BroadcastReceiver
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/

 /* =================== Constants =================== */
 private static final String TAG = "GeoTracerAutostart";   // TAG used for logging purposes

 /*=============================================================================================================================================*
 |                                             BROADCASTRECEIVER INTERFACE CALLBACK FUNCTIONS                                                   |
 *=============================================================================================================================================*/
 public void onReceive(Context context,Intent osIntent)
  {
   // Intent used to start the Geotracer Service
   Intent geoTracerService = new Intent(context,GeotracerService.class);

   switch(osIntent.getAction())
    {
     case Intent.ACTION_BOOT_COMPLETED:
      Log.i(TAG,"Boot Completed, starting the Geotracer Service");
      context.startForegroundService(geoTracerService);
      break;

     case Intent.ACTION_PACKAGE_ADDED:
     case Intent.ACTION_PACKAGE_CHANGED:
      // Ensure that packages of our application have changed
      if(osIntent.getData().getSchemeSpecificPart().equals((context.getPackageName())))
       {
        Log.i(TAG,"Package Changes, starting the Geotracer Service");
        context.startForegroundService(geoTracerService);
       }
      break;

     default:
      Log.e(TAG,"Unknown Intent received from the Android OS");
    }
  }
}