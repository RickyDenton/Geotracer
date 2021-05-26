package com.geotracer.geotracer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.generics.OpStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class GeoScanner
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/

 /* =================== Constants =================== */
 private static final String TAG = "GeoScanner";                // TAG used for logging purposes

 // ------ Samples aggregation window parameters ------
 private static final int WINDOW_MIN_SAMPLES = 10;              // Minimum samples aggregation window for approximating the distance from a device
 private static final int WINDOW_MAX_SAMPLES = 50;              // Maximum samples aggregation window for approximating the distance from a device
 private static final long WINDOW_MAX_TIME = 10000;             // Maximum time interval for samples to be aggregated in the same window (ms)

 // ---------- Distance Estimation parameters ----------
 private static final int RSSI_REF = -81;                       // The supposed RSSI at 1 meter from the transmitter
 // to be used in the distance estimation formula
 private static final double N = 2.5;                           // The path-loss exponent to be used in the distance estimation formula

 // ------------ Proximity Alert parameters ------------
 private static final double SAFE_DISTANCE = 1.5;               // Safe distance below which proximity alerts should be raised to the user
 private static final long PROXIMITY_NOTIF_INTERVAL = 40000;    // Minimum time interval between two consecutive proximity alert notifications

 /* ==================== Objects ==================== */
 final private GeotracerService geotracerService;               // A reference to the main service object
 final private GeoLocator geoLocator;                           // GeoLocator support object
 final private BluetoothLeScanner bluetoothScanner;             // Bluetooth Advertiser object
 private Notification proximityNotification;                    // Warning notification raised to the user in case of social distancing violations
 private final Hashtable<String,AdvList> advTable;              // The Hashtable storing the lists of received advertisements before processing
 private Timer advParser;                                       // Timer used to parse received signatures with a fixed-delay execution

 /* ============ Service Status Variables ============ */
 private boolean isScanning;                                    // Whether the service is currently scanning for bluetooth advertisements
 private long lastProximityNotificationTime;                    // The last time a proximity notification was raised to the user
 private boolean isProximityNotificationEnabled;                // Whether proximity warning notifications are enabled or not

 /*=============================================================================================================================================*
 |                                                    PACKAGE-VISIBILITY METHODS                                                                |
 *=============================================================================================================================================*/

 // Constructor
 GeoScanner(GeotracerService geotracerService, BluetoothLeScanner bluetoothScanner, GeoLocator geoLocator)
  {
   this.geotracerService = geotracerService;
   this.bluetoothScanner = bluetoothScanner;
   this.geoLocator = geoLocator;
   isScanning = false;
   advTable = new Hashtable<>();
   lastProximityNotificationTime = 0;
   isProximityNotificationEnabled = true;

   // Initialize notification to be used for proximity warnings
   initProximityAlert();
  }

 // Starts the bluetooth advertisements scanning, returning the result of the operation
 boolean startScanning()
  {
   // Check the scanning not to be already started or starting
   if(isScanning)
    {
     Log.w(TAG,"Signature Scanning already started");
     return false;
    }

   // Assert the Bluetooth Scanning object to be present
   if(bluetoothScanner == null)
    {
     geotracerService.exitWithError(TAG,"The Bluetooth Scanner is unexpectedly NULL, aborting the GeoTracer service");
     return false;
    }
   else
    {
     // Initialize the scan filters to be applied (in this case, none, since the purpose is to receive ALL bluetooth advertisements)
     ArrayList<ScanFilter> filters = new ArrayList<>();
     ScanFilter filter = new ScanFilter.Builder().build();
     filters.add(filter);

     // Initialize the general scan settings
     ScanSettings settings = new ScanSettings.Builder()
             .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)    // Always report every advertisement associated to the (empty) filter
             .setMatchMode(ScanSettings.MATCH_MODE_STICKY)               // Return advertisements only if above a minimum RSSI and sightings
             .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)  // Always return ALL sensed advertisements
             .setReportDelay(0)                                          // Return all results immediately without kernel-level buffering
                                                                         // NOTE: In fact the kernel cheats if >0, keep it == 0
             .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)            // Maximum scanning frequency
             .build();

     // Start scanning for bluetooth advertisements (indefinitely)
     bluetoothScanner.startScan(filters,settings,scanningCallback);

     // Initialize and start the scanSweeper task with a fixed-delay execution
     advParser = new Timer();
     advParser.schedule(new AdvParserTask(),0,650);

     isScanning = true;
     Log.d(TAG,"Bluetooth Scanning Started");
     return true;
    }
  }

 // Stops the bluetooth advertisements scanning, returning the result of the operation
 boolean stopScanning()
  {
   // Check the scanning to be started
   if(!isScanning)
    {
     Log.w(TAG,"Signature Advertising is already stopped");
     return false;
    }

   // Assert the Bluetooth Scanning object to be present
   if(bluetoothScanner == null)
    {
     geotracerService.exitWithError(TAG,"The Bluetooth Scanner is unexpectedly NULL, aborting the GeoTracer service");
     return false;
    }
   else
    {
     // Stop the Bluetooth scan
     bluetoothScanner.stopScan(scanningCallback);

     // Stop the ScanSweeper Timer, if present
     if(advParser != null)
      advParser.cancel();

     // Reset the contents of the advTable
     advTable.clear();

     isScanning = false;
     Log.d(TAG,"Bluetooth Scanning Stopped");
     return true;
    }
  }

 // Returns true is the service is currently scanning for bluetooth advertisements
 boolean isScanning()
  { return isScanning; }

 // Returns true if proximity notifications are enabled
 boolean isProximityNotificationEnabled()
  { return isProximityNotificationEnabled; }

 // Enables proximity warnings, returning the result of the operation
 boolean enableProximityWarnings()
  {
   if(isProximityNotificationEnabled)
    {
     Log.w(TAG,"Proximity warning notifications are already enabled");
     return false;
    }
   else
    {
     isProximityNotificationEnabled = true;
     Log.d(TAG,"Proximity Warning Enabled");
     return true;
    }
  }

 // Disables proximity warnings, returning the result of the operation
 boolean disableProximityWarnings()
  {
   if(!isProximityNotificationEnabled)
    {
     Log.w(TAG,"Proximity warning notifications are not enabled");
     return false;
    }
   else
    {
     isProximityNotificationEnabled = false;
     Log.d(TAG,"Proximity Warning Disabled");
     return true;
    }
  }

 // Returns the current contents of the advTable serialized as a String
 String getAdvTable()
  {
   List<String> keysArray = new ArrayList<>(advTable.keySet());  // List of keys in the advTable
   StringBuilder sb = new StringBuilder();                       // Used to serialize the contents of the advTable

   // For each key in the scansResult table
   for(int k=0; k<keysArray.size(); k++)
    {
     String key = keysArray.get(k);
     AdvList advList = advTable.get(key);                          // Retrieve the DevRecord associated to the key

     // Serialize all the AdvLists in the advTable in a string
     if(advList != null)
      sb.append(advList).append("\n");
    }

   // Return the serialized table
   return sb.toString();
  }

 /*=============================================================================================================================================*
  |                                                         PRIVATE METHODS                                                                     |
  *=============================================================================================================================================*/

 // Callback Object used by the Android OS to report bluetooth received advertisements and status changes
 private final ScanCallback scanningCallback = new ScanCallback()
  {
   // A single advertisement was received
   @Override
   public void onScanResult(int callbackType,ScanResult scanResult)
    {
     super.onScanResult(callbackType,scanResult);

     // Injects the received advertisement in the advTable
     injectAdvertisement(scanResult);
    }

   // A bulk of advertisements was received (should NOT happen with .setReportDelay(0))
   @Override
   public void onBatchScanResults(List<ScanResult> scanResults)
    {
     super.onBatchScanResults(scanResults);

     // Injects all the received advertisements in the advTable
     for(ScanResult scanResult : scanResults)
      injectAdvertisement(scanResult);
    }

   // The Bluetooth scanning failed
   @Override
   public void onScanFailed(int errorCode)
    {
     // The SCAN_FAILED_ALREADY_STARTED error should NEVER occur
     if(errorCode != SCAN_FAILED_ALREADY_STARTED)
      Log.e(TAG,"Bluetooth Scanning Already Started! (error in the logic)");
     else
      {
       isScanning = false;

       // Stop the TimerTask for parsing the received advertisements and reset the scanTable
       if(advParser != null)
        advParser.cancel();

       Log.e(TAG,"Scanning Failed! code = "+errorCode);
       super.onScanFailed(errorCode);
      }
    }
  };

 // Injects a received bluetooth advertisement into the advTable
 private void injectAdvertisement(ScanResult scanResult)
  {
   ScanRecord scanRecord;  // The ScanRecord associated to the ScanResult returned by Android OS
   String key;             // The key used as index to insert the received advertisement in the advTable,
                           // consisting in the signature, if present, or in the transmitter's MAC address
   AdvType advType;        // The received type of advertisement
   AdvList advList;        // The AdvList in the advTable where the advertisement must be injected

   // Assert the advertisement not to be empty
   if(scanResult == null || scanResult.getDevice() == null || (scanRecord = scanResult.getScanRecord()) == null)
    {
     Log.w(TAG,"Received empty advertisement from the kernel");
     return;
    }

   // Get the advertisement received timestamp as milliseconds from Unix Epoch
   long timestamp = new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime() + scanResult.getTimestampNanos() / 1000000).getTime();

   // Get the advertisement RSSI
   int RSSI = scanResult.getRssi();

   // Attempt to retrieve a signature associated to the CONTACT_TRACING_SERVICE in the advertisement's payload, if any
   byte[] sigBytes = scanRecord.getServiceData(GeotracerService.CONTACT_TRACING_SERVICE);

   // If the signature was found the advertisement represents a signature advertisement, and such signature should be used as key in the advTable
   if(sigBytes != null)
    {
     key = GeotracerService.byteArrayToHex(sigBytes);
     advType = AdvType.ADV_TYPE_SIG;
    }

   // Otherwise if the signature was not found the advertisement represents a generic
   // bluetooth advertisement, and the sender's MAC address should be used as key
   else
    {
     key = scanResult.getDevice().getAddress();
     advType = AdvType.ADV_TYPE_MAC;
    }

   // Retrieve the AdvList associated to the advertisement in the AdvTable, if any
   advList = advTable.get(key);

   // If the AdvTable contains no such AdvList, create a new AdvList containing the received advertisement and append it into the AdvTable
   if(advList == null)
    {
     advList = new AdvList(advType);
     advList.samples.add(new AdvSample(RSSI,timestamp));
     advTable.put(key,advList);
    }

   // Otherwise if the AdvList was found, append the received advertisement by first acquiring a mutex for synchronization purposes with the
   // AdvParserTask TimerTask
   else
    {
     advList.mutex.lock();
       advList.samples.add(new AdvSample(RSSI,timestamp));
     advList.mutex.unlock();
    }

   /* Debugging purposes
   Log.d(TAG,"New " + advType + " received (key = " + key + "RSSI =" + RSSI + ", timestamp = " +
           new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault()).format(new Date(timestamp)) + ")");
   */
  }

 // Initializes the notification to be shown to the user should he come in
 // too close contact (< SAFE_DISTANCE) with a device broadcasting a signature
 private void initProximityAlert()
  {
   // Create the proximity alert notification, which launches the main GeoTracer activity when clicked
   PendingIntent launchMainActivity = PendingIntent.getActivity(geotracerService,2,
                                                                new Intent(geotracerService,MainActivity.class),0);
   // Create the notification
   proximityNotification = new Builder(geotracerService,geotracerService.getString(R.string.geotracer_notif_channel_id))
           .setSmallIcon(R.drawable.proximity_warning_icon)
           .setContentTitle(geotracerService.getString(R.string.geotracer_notif_proximity_title))
           .setContentText(geotracerService.getString(R.string.geotracer_notif_proximity_contents))
           .setPriority(NotificationCompat.PRIORITY_MAX)
           .setVibrate(new long[] { 500, 500, 500, })
           .setContentIntent(launchMainActivity)
           .setAutoCancel(true)
           .build();
  }

 /*=============================================================================================================================================*
  |                                                        AdvParserTask Runnable                                                               |
  *=============================================================================================================================================*/

  class AdvParserTask extends TimerTask
   {
    /* =================== Constants =================== */
    private static final String TAG = "AdvParserTask";              // TAG used for logging purposes

    // Filters the RSSIs of a distance estimation window by using their median value
    private int filterRSSIMedian(List<Integer> RSSIList)
     {
      int listSize = RSSIList.size();

      // Sort the RSSis in the list
      Collections.sort(RSSIList);

      // Return the median RSSI depending if the list has an even or odd number of elements
      if(listSize % 2 == 0)
       return (RSSIList.get(listSize/2) + RSSIList.get(listSize/2 -1))/2;
      else
       return RSSIList.get(listSize/2);
     }

    // Estimate the distance in meters from a device from the filtered RSSIs of its received advertisements
    double estimateDistance(int RSSI)
     {
      /*
      Distance Estimation
      ========================================================================================
      From the literature[1] the distance from a device broadcasting a bluetooth signal
      can be approximated via the following formula:

                                d = 10^((RSSI_REF-RSSI)/10n))

      Where:
        - RESSI_REF: The average RSSI measured at 1 meter from the transmitting device (dBm)
        - RSSI: The received signal strength (or more in general, its filtered value) (dBm)
        - n: The current path loss exponent

      Given the fact that the "RSSI_REF" and "n" variables cannot be known a priori (even
      if the former could be somewhat approximated by collecting the "RSSI_REF" readings of
      known bluetooth chipsets in a look-up table), their values have been tuned within the
      development environment using the available devices so to produce meaningful results as:

        - RSSI_REF = -81
        - n = 2.5

      References:
        [1] https://dl.acm.org/doi/10.1145/3356995.3364541
        [2] http://www.davidgyoungtech.com/2020/05/15/how-far-can-you-go */

      return Math.pow(10,((double)(RSSI_REF-RSSI)/(10*N)));
     }

    // Adds an external signature into the local KeyValue Database
    private void addOtherSignature(String key, double contactDistance)
     {
      OpStatus dbResult;     // Used to check the result of the database operation

      // Assert the KeyValue database service to be alive
      if(geotracerService.keyValueDB != null)
       {
        // Add the external signature into the local KeyValue Database
        dbResult = geotracerService.keyValueDB.beacons.insertBeacon(new ExtSignature(key,contactDistance));
        if(dbResult != OpStatus.OK)
         Log.e(TAG,"\"Error in adding an external signature into the keyValue Database: "+dbResult);
        else
         Log.d(TAG,"Added external signature into the keyValue database (" + key + ")");
       }
      else
       Log.e(TAG,"Cannot add external signature, the KeyValue database service is not alive!");
     }

    @Override
    public void run()
     {
      List<String> AdvListKeysArray = new ArrayList<>(advTable.keySet());   // The list of keys in the advTable as an array
      long executionTime = new Date().getTime();                            // The parsing starting execution time

      // Process individually each AdvList in the advTable
      for (int k=0; k<AdvListKeysArray.size(); k++)
       {
        /* ----- General AdvList Information ----- */
        String key = AdvListKeysArray.get(k);         // The key of AdvList to be parsed
        AdvList advList = advTable.get(key);          // The AdvList to be parsed
        ArrayList<AdvSample> samples;                 // The list of samples in the AdvList
        int samplesSize;                              // The number of samples in the AdvList

        /* ----- Processing Indexes ----- */
        int i, j;                                     // Support Indexes used during the parsing
        int estimatedWindowLimit = -1;                // If >0 represents the index of the upper limit of the estimation window
                                                      // that lead to a successful distance estimation
        int startingOutdatedIndex = -1;               // If >0 Represents the starting index in searching for outdated samples

        // Should never happen
        if(advList == null)
         {
          Log.e(TAG,"An AdvList whose key was fetched is NULL (error in the logic)");
          return;
         }

        // Lock the AdvList to prevent concurrent modifications represented by the injection of additional advertisements
        advList.mutex.lock();

        // Retrieve the list of samples in the AdvList and its size
        samples = advList.samples;
        samplesSize = samples.size();

        // Assert the list of samples in the AdvList to contain at least one sample
        if(samplesSize == 0)
         {
          Log.e(TAG,"Found an empty AdvList in the advTable, removing it");
          advTable.remove(key);
          continue;
         }

        // Unlock the AdvList, since concurrent insertion by new samples should cause no problem to the parsing
        advList.mutex.unlock();

        /* Debugging Purposes
        Log.d(TAG,"Start Parsing key \"" + key +"\" (sampleSize = " + samples.size() + ")");
        */

        // ---IF--- (and only if) the AdvList contains at least the MIN_SAMPLES required to be aggregated
        // in a distance estimation window, browse them from the first to the (last - MIN_SAMPLES))
        for(i=0; i<=(samplesSize-WINDOW_MIN_SAMPLES); i++)
         {
          ArrayList<Integer> currRSSIList = new ArrayList<>();          // The list of RSSIs belonging to the current estimation window
          long currWindowLimit = samples.get(i).time + WINDOW_MAX_TIME; // The upper time limit for samples to belong to the current estimation window

          // If this sample is too old to partecipate in a ---FUTURE--- estimation window, update the startingOutdatedIndex
          startingOutdatedIndex = i;

          // Attempt to create a distance estimation window by browsing samples from the current to the last
          for(j=i; j<samplesSize; j++)
           {
            // If the timestamp of the current sample is beyond the currWindowLimit, we have
            // reached the limit of samples that can fit in the current estimation window
            if(samples.get(j).time > currWindowLimit)
             break;

            // If maximum number of samples in the estimation window has been reached, stop adding samples
            if((j-i) == WINDOW_MAX_SAMPLES)
             break;

            // Append the RSSI of the current sample in the currRSSIList
            currRSSIList.add(samples.get(j).RSSI);
           }

          // If the samples window size (j-i) is large enough for estimating the distance
          if((j-i) >= WINDOW_MIN_SAMPLES)
           {
            // Filter the RSSI values in the window and use them for estimating the distance from the device broadcasting the associated signature
            double contactDistance = estimateDistance(filterRSSIMedian(currRSSIList));

            // Assert the estimated distance to fall within a validity range
            if((0 < contactDistance) && (contactDistance < 30))
             {
              // If the current advertisement is a signature, the safe distance is violated, proximity alerts are enabled, and at least
              // PROXIMITY_NOTIF_INTERVAL ms passed from the last proximity alert, raise a proximity alert to the user via a notification
              if((advList.type == AdvType.ADV_TYPE_SIG) && (contactDistance < SAFE_DISTANCE) && (isProximityNotificationEnabled)
                      && ((executionTime - lastProximityNotificationTime) > PROXIMITY_NOTIF_INTERVAL))
               {
                // Show the notification to the user
                NotificationManagerCompat.from(geotracerService).notify(2,proximityNotification);

                // Update the last proximity notification time
                lastProximityNotificationTime = executionTime;
               }

              // Compute the contact time with the device by averaging the timestamps of the first and last advertisements in the window
              long contactTime = (samples.get(i).time + samples.get(j-1).time)/2;

              /*
              QUESTO LOG NON VA TOCCATO!!!!!!
               */
              Log.i("ExperimentAnalysis","Estimated distance from device \"" + key + "\": " + contactDistance + " (time = " +
                      new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault()).format(new Date(contactTime)) + ")");

              // If this is a signature, insert it into the "Other Signatures" into the keyValue database (which takes care of redundancy checks),
              // and set the "addedToDB" flag in the AdvList
              if(advList.type == AdvType.ADV_TYPE_SIG)
               {
                addOtherSignature(key,contactDistance);
                advList.addedToDB = true;
               }

              // If the GeoLocator service is active, attempt to insert the position of the device in the local database
              if(geoLocator != null)
               geoLocator.injectDevicePosition(key,contactTime);
              else
               Log.w(TAG,"Position of device \"" + key + "\" is NOT forwarded to the database (the GeoLocator service is disabled");

              // Since the distance from the device broadcasting the associated advertising has been estimated, set the upper limit

              // Signal that the distance from the device broadcasting the associated advertisement has been estimated
              // by setting the index of the upper limit of the estimation window, and break to cleanup operations
              estimatedWindowLimit = j;
              break;
             }
           }
         }

        /* ============== AdvList Cleanup Operations ============== */
        int dropIndex;   // If >0 represents the upper limit of the sublist of samples to be dropped during cleanup

        // If the advertisement is a signature and was not added into the database during the main distance estimation cycle, add it in the database
        // as a "sighting" with an arbitrary great distance
        if(advList.type == AdvType.ADV_TYPE_SIG && !advList.addedToDB)
         {
          addOtherSignature(key,1000000);
          advList.addedToDB = true;
          Log.d(TAG,"Signature \""+key+"\" added into the database as a sighting");
         }

        // If at least an outdated sample was found during the main cycle, find the index of the first sample recent enough to
        // participate in a future distance estimation window, i.e. whose timestamp is no older than (executionTime - WINDOW_MAX_TIME
        if(startingOutdatedIndex > -1)
         for(i=0; i<samplesSize; i++)
          if(samples.get(i).time > (executionTime-WINDOW_MAX_TIME))
           break;

        // Set the ending index of the sublist of samples that should be dropped, either because they were used to successfully estimate the distance
        // or because they are outdated, to the maximum between the estimatedWindowLimit computed in the main cycle and the index of the first sample
        // recent enough to participate in a future window
        dropIndex = Math.max(estimatedWindowLimit,i);

        // If there are samples to drop (dropIndex > 0), reacquire the lock on the AdvList and do so
        if(dropIndex > 0)
         {
          advList.mutex.lock();

            samples.subList(0,dropIndex).clear();

            // If the resulting list of AdvSamples is empty, remove the AdvList from the AdvTable
            if(advList.samples.isEmpty())
              advTable.remove(key);
            else

          advList.mutex.unlock();
         }
       }
     }
   }
}