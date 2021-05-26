package com.geotracer.geotracer.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.mainapp.MainActivity;


public class GeotracerService extends Service
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/

 /* =================== Constants =================== */
 private static final String TAG = "Geotracer Service";                // TAG used for logging purposes
 private static final int MAIN_NOTIFICATION_ID = 1;                    // The main notification ID used by the service
 public static final ParcelUuid CONTACT_TRACING_SERVICE = ParcelUuid   // The BLE Service UUID associated to the Contact Tracing Service (0XFD6F)
         .fromString("0000fd6f-0000-1000-8000-00805f9b34fb");

 /* ============== Sub-Services Objects ============== */
 private GeoLocator geoLocator;                                        // The GeoLocator service object
 private GeoAdvertiser geoAdvertiser;                                  // The GeoAdvertiser service object
 private GeoScanner geoScanner;                                        // The GeoScanner service object

 /* ================= Service Binder ================= */
 private IBinder geoBinder;

 /* =============== Database Services =============== */
 KeyValueManagement keyValueDB;                                        // Key-Value Database service object (local database)
 FirestoreManagement firestoreDB;                                      // Firestore Database service object (remote database)

 /* ============ Service Status Variables ============ */
 private boolean isServiceStarted = false;                             // Whether the service has successfully started

 /*=============================================================================================================================================*
 |                                                     SERVICE CALLBACK FUNCTIONS                                                               |
 *=============================================================================================================================================*/

 @Override
 public void onCreate()
  {
   // Set the service as a foreground service
   // NOTE: This must be called within 5 seconds of the service's creation, otherwise Android kills it
   setAsForeground();

   // Assert that the dynamic ACCESS_FINE_LOCATION permission has been granted to the application (otherwise the service cannot run)
   if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
     Log.e(TAG,"The Dynamic ACCESS_FINE_LOCATION permission is not granted to the application, the Geotracer Service cannot be started");
     stopSelf();
     return;
    }

   // Bind to the local and remote databases used by the service
   bindToDB();

   // Initialize the GeoLocator Service
   initLocation();

   // Initialize the GeoAdvertiser and GeoScanner Services
   initBluetooth();

   // Initialize the Binder object used to bind with the Geotracer testing activity
   geoBinder = new GeotracerBinder();

   // Start all sub-services
   geoLocator.startLocalization();
   geoAdvertiser.startAdvertising();
   geoScanner.startScanning();
  }

 // This callback function is called every time the service is started with the startService() function, even if calls beyond the first are of no use
 @Override
 public int onStartCommand(Intent startIntent, int startFlags, int startId)
  {
   // If this is the first call of the startService() function, mark the service as started
   if(!isServiceStarted)
    {
     isServiceStarted = true;
     Log.i(TAG,"----- Geotracer Service Started -----");
    }
   else
    Log.w(TAG,"Geotracer service already started");

   // Return a constant instructing the Android OS that, should it kill it, the service
   // should be recreated as soon as possible without re-delivering the previous Intent
   return Service.START_STICKY;
  }

 // Callback function for binding with other application components
 @Override
 public IBinder onBind(Intent intent)
  { return geoBinder; }

 // Binder Class used to create the binder to this service
 public class GeotracerBinder extends Binder
  {
   public GeotracerService getService()
    { return GeotracerService.this; }
  }

 // Callback function called when the service is destroyed by the Android OS
 @Override
 public void onDestroy()
  {
   // Stop all the enabled sub-services
   if(geoLocator != null)
    geoLocator.stopLocalization();
   if(geoAdvertiser != null)
    geoAdvertiser.stopAdvertising();
   if(geoScanner != null)
    geoScanner.stopScanning();

   // Unbind from the database services
   if(keyValueDB != null)
    unbindService(keyValueStoreServiceConnection);
   if(firestoreDB != null)
    unbindService(firestoreServiceConnection);

   Log.w(TAG,"----- Geotracer Service Stopped -----");
  }

  /*=============================================================================================================================================*
  |                                                  SERVICE INITIALIZATION FUNCTIONS                                                            |
  *=============================================================================================================================================*/

 // Sets the service as a foreground service, display an initialization notification
 @RequiresApi(api = Build.VERSION_CODES.O)
 private void setAsForeground()
  {
   /* ======= Foreground Notification Objects ======== */
   NotificationManager notificationManager;                 // The Notification Manager object used by the service
   NotificationChannel geotracer_channel;                   // The Notification Channel object used by the service
   Notification startedNotification;                        // The temporary notification to be shown while the service is initializing

   // If supported (and required), create a notification channel for the service (Android API 26+, Oreo)
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
    {
     // Initialize the notification channel's parameters
     String channelID = getString(R.string.geotracer_notif_channel_id);                      // Channel ID
     CharSequence channelName = getString(R.string.geotracer_notif_channel_name);            // Channel Name
     String channelDescription = getString(R.string.geotracer_notif_channel_description);    // Channel Description
     int channelImportance = NotificationManager.IMPORTANCE_HIGH;                            // Channel Importance (HIGH is required for sound)

     // Create the notification channel
     geotracer_channel = new NotificationChannel(channelID,channelName,channelImportance);
     geotracer_channel.setDescription(channelDescription);

     // Register the notification channel into the notification manager
     notificationManager = getSystemService(NotificationManager.class);
     notificationManager.createNotificationChannel(geotracer_channel);
    }

   // Initialize the foreground notification displayed by the service so to launch the GeoTracer main activity when clicked
   PendingIntent geoTracerActivity = PendingIntent.getActivity(this,0,
                                                               new Intent(this,MainActivity.class),0);

   startedNotification = new Notification.Builder(this,getString(R.string.geotracer_notif_channel_id))
           .setContentTitle(getText(R.string.geotracer_notif_channel_name))
           .setContentText(getText(R.string.geotracer_notif_channel_started))
           .setSmallIcon(R.drawable.geotracer_icon)
           .setContentIntent(geoTracerActivity)
           .build();

   // Set the service as a foreground service
   startForeground(MAIN_NOTIFICATION_ID, startedNotification);
  }

 // Initializes the GeoLocator service
 private void initLocation()
  {
   LocationManager locationManager;  // The Location Manager object to be used by the GeoLocator service

   // Attempt to retrieve the system location manager
   if((locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)) == null)
    Log.e(TAG,"Could not retrieve the Location Manager, the GeoLocator service will not work");
   else
    {
     // Attempt to retrieve the GPS location provider
     if((locationManager.getProvider(LocationManager.GPS_PROVIDER)) == null)
      Log.e(TAG,"Could not retrieve the GPS provider, the GeoLocator service will not work");
     else
      {
       // If the GPS is not enabled, ask the user to do so
       if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
         Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(gpsOptionsIntent);
         Toast.makeText(this,"Please enable GPS localization to fully utilize the Geotracer Service",Toast.LENGTH_LONG).show();
        }

       // Initialize the GeoLocator support object
       geoLocator = new GeoLocator(this,locationManager);
      }
    }
  }

 // Initializes the GeoAdvertiser and GeoScanner services
 private void initBluetooth()
  {
   BluetoothManager bluetoothManager;            // The BLE manager object required to initialize the other objects
   BluetoothAdapter bluetoothAdapter;            // The BLE adapter object required to initialize the BLE Advertiser and Scanner objects
   BluetoothLeAdvertiser bluetoothAdvertiser;    // The BLE Advertiser object used by the GeoAdvertiser service
   BluetoothLeScanner bluetoothScanner;          // The BLE Scanner object used by the GeoScanner service

   // Assert the Bluetooth Stack to be present on the device
   if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
    exitWithError(TAG,"No BLE stack was found, exiting");

   // Attempt to retrieve the Bluetooth Manager
   if((bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE)) == null)
    exitWithError(TAG,"Error in retrieving the Bluetooth Manager, exiting");

   // Attempt to retrieve the Bluetooth Adapter
   if((bluetoothAdapter = bluetoothManager.getAdapter()) == null)
    exitWithError(TAG,"Error in retrieving the Bluetooth Adapter, exiting");

   // If the bluetooth is not enable do so (without explicit user consent)
   if(!bluetoothAdapter.isEnabled())
    {
     Log.w(TAG,"Force-enabling the Bluetooth Adapter");
     bluetoothAdapter.enable();
    }

   // Attempt to retrieve the Bluetooth Advertiser
   if((bluetoothAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser()) == null)
    exitWithError(TAG,"Error in retrieving the Bluetooth Advertiser, exiting");

   // Attempt to retrieve the Bluetooth Scanner
   if((bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner()) == null)
    exitWithError(TAG, "Error in retrieving the Bluetooth Scanner, exiting");

   // Initialize the Bluetooth support objects
   geoAdvertiser = new GeoAdvertiser(this,bluetoothAdvertiser);
   geoScanner = new GeoScanner(this,bluetoothScanner,geoLocator);
  }

 // Bind the Geotracer to the local and remote database services
 private void bindToDB()
  {
   // Bind to the KeyValueStore database service (local)
   Intent keyValueStoreService = new Intent(this,KeyValueManagement.class);
   bindService(keyValueStoreService,keyValueStoreServiceConnection,Context.BIND_AUTO_CREATE);

   // Bind to the Firestorm database service (remote)
   Intent firestoreService = new Intent(this,FirestoreManagement.class);
   bindService(firestoreService,firestoreServiceConnection,Context.BIND_AUTO_CREATE);
  }

 // ServiceConnection callback object associated to the KeyValueStore Database (local)
 private final ServiceConnection keyValueStoreServiceConnection = new ServiceConnection()
  {
   // If the binding to the KeyValueStore database service was successful
   @Override
   public void onServiceConnected(ComponentName className,IBinder keyValueService)
    {
     KeyValueManagement.LocalBinder keyValueStoreBinder = (KeyValueManagement.LocalBinder) keyValueService;
     keyValueDB = keyValueStoreBinder.getService();
    }

   // If the binding failed or the service terminates during execution
   @Override
   public void onServiceDisconnected(ComponentName arg0)
    {
     Log.e(TAG,"KeyValue Database Service unexpectedly disconnected");
     keyValueDB = null;
    }
  };

 // ServiceConnection callback object associated to the Firebase database (remote)
 private final ServiceConnection firestoreServiceConnection = new ServiceConnection()
 {
  // If the binding to the Firestorm database service was successful
  @Override
  public void onServiceConnected(ComponentName className,IBinder firestormService)
   {
    FirestoreManagement.LocalBinder firestormBinder = (FirestoreManagement.LocalBinder) firestormService;
    firestoreDB = firestormBinder.getService();
   }

  // If the binding failed or the service terminates during execution
  @Override
  public void onServiceDisconnected(ComponentName arg0)
   {
    Log.e(TAG,"Firestore Database Service unexpectedly disconnected");
    firestoreDB = null;
   }
 };

 /*=============================================================================================================================================*
 |                                                 PACKAGE-VISIBILITY UTILITY FUNCTIONS                                                         |
 *=============================================================================================================================================*/

 // Stops the entire service if a fatal error occurs
 void exitWithError(String TAG,String error)
  {
   Log.e(TAG,error);
   stopSelf();
  }

 // Converts contents of a byte[] into a String in hexadecimal notation
 static String byteArrayToHex(byte[] byteArray)
  {
   StringBuilder sb = new StringBuilder(byteArray.length * 2);    // StringBuilder used to build the converted String

   // Convert each byte of the array into a string in hexadecimal notation
   for(byte b : byteArray)
    sb.append(String.format("%02x",b));

   // Return the StringBuilder object as a String
   return sb.toString();
  }

 /*==============================================================================================================================================*
  |                                                      BIND INTERFACE FUNCTIONS                                                                |
  *=============================================================================================================================================*/

 /* ============================== General Interface Functions ============================== */

 // Returns true if the GeoTracer service has started
 public boolean isServiceStarted()
  { return isServiceStarted; }

 /* ============================ GeoLocator Interface Functions ============================ */

 // Start the localization service by registering this object as a listener for GPS location updates, returning the result of the operation
 // NOTE: The actual rate and timing of GPS readings are performed by the Android OS according to its own policies, this is just a listener
 public boolean startLocalization()
  {
   if(geoLocator != null)
    return geoLocator.startLocalization();
   else
    return false;
  }

 // Stops the localization service, returning the result of the operation
 public boolean stopLocalization()
  {
   if(geoLocator != null)
    return geoLocator.stopLocalization();
   else
    return false;
  }

 // Returns the last known user location, if any
 // NOTE: May return NULL if no user position is known
 public Location getLastLocation()
  {
   if(geoLocator != null)
    return geoLocator.getLastLocation();
   else
    return null;
  }

 // Returns true if the localization service is enabled
 public boolean isLocalizing()
  {
   if(geoLocator != null)
    return geoLocator.isLocalizing();
   else
    return false;
  }

 // Returns true if the GPS location provider is enabled
 public boolean isGPSEnabled()
  {
   if(geoLocator != null)
    return geoLocator.isGPSEnabled();
   else
    return false;
  }

 /* =========================== GeoAdvertiser Interface Functions =========================== */

 // Starts the advertising using a new random signature, returning the result of the operation
 public boolean startAdvertising()
  { return geoAdvertiser.startAdvertising(); }

 // Stop advertising the signature, returning the result of the operation
 public boolean stopAdvertising()
  { return geoAdvertiser.stopAdvertising(); }

 // Changes the random signature being advertised, returning the result of the operation
 public boolean resetSignature()
  { return geoAdvertiser.resetSignature(); }

 // Returns the currently advertised user signature as a String in hexadecimal format
 // NOTE: Returns null if advertising is not started
 public String getSignature()
  { return geoAdvertiser.getSignature(); }

 // Returns true is the service is currently advertising the user's random signature
 public boolean isAdvertising()
  { return geoAdvertiser.isAdvertising(); }

 /* ============================= GeoScanner Interface Functions ============================= */

 // Starts the bluetooth advertisements scanning, returning the result of the operation
 public boolean startScanning()
  { return geoScanner.startScanning(); }

 // Stops the bluetooth advertisements scanning, returning the result of the operation
 public boolean stopScanning()
  { return geoScanner.stopScanning(); }

 // Returns true is the service is currently scanning for bluetooth advertisements
 public boolean isScanning()
  { return geoScanner.isScanning(); }

 // Returns true if proximity notifications are enabled
 public boolean isProximityNotificationEnabled()
  { return geoScanner.isProximityNotificationEnabled(); }

 // Enables proximity warnings, returning the result of the operation
 public boolean enableProximityWarnings()
  { return geoScanner.enableProximityWarnings(); }

 // Disables proximity warnings, returning the result of the operation
 public boolean disableProximityWarnings()
  { return geoScanner.disableProximityWarnings(); }

 // Returns the current contents of the advTable serialized as a String (should never return NULL)
 public String getAdvTable()
  { return geoScanner.getAdvTable(); }
}