package com.geotracer.geotracer.db.local;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.annotation.RequiresApi;
import java.util.concurrent.TimeUnit;
import androidx.work.Constraints;
import androidx.work.WorkManager;
import android.content.Intent;
import android.app.Service;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Binder;
import android.os.Build;
import android.util.Log;

import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.services.GeocoderManager;
import com.google.firebase.firestore.GeoPoint;

import io.paperdb.Paper;

//   KEY-VALUE MANAGEMENT
//   Class to manage the access to a local key-value database. The service is composed by several
//   subclasses to better improve usability of the class giving a more logical separation between
//   the function provided by the module
//          - .signatures: functions to operate on user signatures
//          - .positions: functions to operate on user's positions
//          - .beacons: functions to operate on other application's signatures
//          - .buckets: functions to add/remove bucket from which receive notifications


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class KeyValueManagement extends Service {

    //  class used to pass the key-value service by a Binder
    public class LocalBinder extends Binder {
        public KeyValueManagement getService() {
            return KeyValueManagement.this;
        }
    }

    private final IBinder classBinder = new LocalBinder();  // maintains a reference to a global service class
    public SignatureUtility signatures = null;   //  functions to operate on signatures
    public PositionUtility positions = null;     //  functions to operate on user positions
    public BeaconUtility beacons = null;         //  functions to operate on beacons
    public BucketUtility buckets = null;         //  functions to operate on buckets

    @Override
    public void onCreate(){
        super.onCreate();

        //  initialization of Paper key-value database
        Paper.init(getBaseContext());

        signatures = new SignatureUtility(Paper.book("signatures"));   //  functions to operate on signatures
        positions = new PositionUtility(Paper.book("positions"), this);      //  functions to operate on user positions
        beacons = new BeaconUtility(Paper.book("beacons"));            //  functions to operate on beacons
        buckets = new BucketUtility(Paper.book("buckets"));            //  functions to operate on buckets
        //  launch the DatabaseConsolidator worker
        WorkManager
                .getInstance(this.getBaseContext())
                .enqueueUniquePeriodicWork(                 //  only one worker which will be called periodically
                        "consolidator",       //  name assigned to worker
                          ExistingPeriodicWorkPolicy.KEEP,  //  if a worker is already present discharge the new worker
                          new PeriodicWorkRequest.Builder(     //  worker will be called 1 time per hour
                                  DatabaseConsolidator.class,
                                  1, TimeUnit.HOURS
                          )
                .setConstraints(         //  worker will be called only when the device is idle
                        new Constraints
                                .Builder()
                                .setRequiresDeviceIdle(true)
                                .build()
                ).build());

    }

    @Override
    public IBinder onBind(Intent intent) {

        //  initialization of Paper key-value database
        Paper.init(getBaseContext());
        return classBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        //  initialization of Paper key-value database
        return true;
    }

    //  general function to remove all the elements from the local database
    public boolean cleanLocalStore(){

        if(signatures.removeAllSignatures() != OpStatus.OK)
            return false;
        if(positions.dropAllPositions() != OpStatus.OK)
            return false;
        if( beacons.dropAllBeacons() != OpStatus.OK)
            return false;
        buckets.dropAllBuckets();
        return true;
    }

    public String geoCode( GeoPoint loc ){

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(loc.getLatitude());
        location.setLongitude(loc.getLongitude());
        Log.d("TEST_GEOCODE","Test geocoding conversation...");
        String city = GeocoderManager.convertLocationToPlace(location,getApplicationContext());
        Log.d("TEST_GEOCODE",city);
        return city;

    }
}

