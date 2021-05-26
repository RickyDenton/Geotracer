package com.geotracer.geotracer.notifications;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.google.firebase.firestore.ListenerRegistration;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentChange;
import android.content.ServiceConnection;
import androidx.work.OneTimeWorkRequest;
import com.esotericsoftware.minlog.Log;
import android.content.ComponentName;
import androidx.work.WorkManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import java.util.ArrayList;
import android.os.IBinder;
import java.util.Calendar;
import java.util.HashMap;
import android.os.Binder;
import java.util.Objects;
import java.util.Locale;
import io.paperdb.Paper;
import java.util.List;
import java.util.Date;


@SuppressWarnings("all")
public class NotificationSender extends Service {

    private static final String TAG = "NotificationSender";

    public static final String ACTION_BROADCAST = NotificationSender.class.getName();
    public class LocalBinder extends Binder {

        public NotificationSender getService() {
            return NotificationSender.this;
        }

    }

    private final ServiceConnection keyValueService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            KeyValueManagement.LocalBinder binder = (KeyValueManagement.LocalBinder) service;
            keyValueStore = binder.getService();

            //  getting from the local database all the user's buckets
            RetStatus<List<String>> result = keyValueStore.buckets.getBuckets();

            try {
                //  saving all the registered buckets
                if (result.getStatus() == OpStatus.OK)
                    observedLocations.addAll(result.getValue());

                //  for each bucket we add a listener to receive updates asynchronously
                observedLocations.forEach(bucket -> listeners
                        .put(bucket,firestore.collection(bucket)
                                .addSnapshotListener((value, error) -> {

                                    assert value != null;
                                    if(value.getMetadata().isFromCache()) return;

                                    for (DocumentChange dc : value.getDocumentChanges())
                                        if( dc.getType() == DocumentChange.Type.ADDED &&
                                                keyValueStore.beacons.beaconPresent(
                                                        (String)dc.getDocument().getData().get("signature")) == OpStatus.PRESENT){
                                            infectionReaction();
                                            break;
                                        }
                                })
                        ));

            }catch(RuntimeException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            keyValueStore = null;

        }
    };

    private final IBinder classBinder = new NotificationSender.LocalBinder();
    private final HashMap<String, ListenerRegistration> listeners = new HashMap<>(); //  bucket listeners
    private final List<String> observedLocations = new ArrayList<>();                //  buckets listened
    private FirebaseFirestore firestore;
    private KeyValueManagement keyValueStore;

    @Override
    public void onCreate() {

        super.onCreate();
        try {
            //  creating the connection to Firestore
            firestore = FirebaseFirestore.getInstance();

            //  creating connection to service KeyValueManagement
            Intent service = new Intent(getBaseContext(), KeyValueManagement.class);
            bindService(service, keyValueService, Context.BIND_AUTO_CREATE);

        }catch(RuntimeException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        try {
            unbindService(keyValueService);
            firestore.terminate();
        }catch(RuntimeException e){
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        if( intent == null )
            return null;

        return classBinder;

    }

    //  the function starts a new Worker which will collect all the user signatures stored inside
    //  the local database and send them on all the registered buckets
    public void infectionAlert(){

        try {
            WorkManager
                    .getInstance(this.getBaseContext())
                    .enqueue(
                            new OneTimeWorkRequest.Builder(InfectionAlarm.class).build()
                    );
        }catch(RuntimeException e){
            e.printStackTrace();
        }
    }

    //  adds a new bucket to the key-value store and allocate a new listener to receive updates
    //  from the new location
    //  Returns:
    //           - OpStatus.ILLEGAL_ARGUMENT: invalid arguments provided to the class
    //           - OpStatus.OK: bucket correctly inserted locally and remotely
    //           - OpStatus.ERROR: an error has occurred during the request management
    public OpStatus addBucket(String bucket){

        if( bucket == null || bucket.length() == 0 )
            return OpStatus.ILLEGAL_ARGUMENT;

        try {
            OpStatus status = keyValueStore.buckets.insertBucket(bucket);
            if (status != OpStatus.OK)
                return status;

            listeners
                    .put(bucket, firestore.collection(bucket)
                            .addSnapshotListener((value, error) -> {

                                assert value != null;
                                if (value.getMetadata().isFromCache()) return;

                                for (DocumentChange dc : value.getDocumentChanges())
                                    if (dc.getType() == DocumentChange.Type.ADDED &&
                                            keyValueStore.beacons.beaconPresent(
                                                    (String) dc.getDocument().getData().get("signature")) == OpStatus.PRESENT) {
                                        infectionReaction();
                                        break;
                                    }
                            })
                    );
            Log.debug(TAG, "Added " + bucket + " to the listened channels");
            return OpStatus.OK;
        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  removes all the buckets from the local database and their associated listener
    //  Returns:
    //           - OpStatus.ILLEGAL_ARGUMENT: invalid arguments provided to the class
    //           - OpStatus.OK: all the bucket correctly removed locally
    //           - OpStatus.ERROR: an error has occurred during the request management
    public OpStatus removeAllBuckets(){
        try {
            RetStatus<List<String>> buckets = keyValueStore.buckets.getBuckets();
            if (buckets.getStatus() != OpStatus.OK)
                return buckets.getStatus();

            buckets.getValue().forEach(this::removeBucket);
            return OpStatus.OK;
        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  removes a single bucket and its listener
    //  Returns:
    //           - OpStatus.ILLEGAL_ARGUMENT: invalid arguments provided to the class
    //           - OpStatus.OK: bucket correctly removed locally
    //           - OpStatus.ERROR: an error has occurred during the request management
    public OpStatus removeBucket(String bucket){

        try {
            if (bucket == null || bucket.length() == 0)
                return OpStatus.ILLEGAL_ARGUMENT;

            OpStatus result = keyValueStore.buckets.removeBucket(bucket);
            if (result == OpStatus.OK)
                Objects.requireNonNull(listeners.get(bucket)).remove();

            if (listeners.containsKey(bucket)) {
                Objects.requireNonNull(listeners.get(bucket)).remove();
                listeners.remove(bucket);
            }
            return result;
        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  verification of the user status[Infected/Not Infected]
    //  Returns:
    //           - OpStatus.INFECTED: the user is classified as infected
    //           - OpStatus.NOT_INFECTED: the user is not classified as infected
    //           - OpStatus.ERROR: an error has occurred during the request management
    public OpStatus canIbeInfected(){

        try {
            Paper.init(getBaseContext());
        //  we verify the presence of an infection alert stored on the local database
            if(Paper.book("notification_state").contains("application_state"))

                //  if present we verify if it is expired and in the case, we remove it
                if(Objects
                        .requireNonNull(new SimpleDateFormat("EEE MMM dd hh:mm:ss zzzz yyyy", Locale.ENGLISH)
                                .parse(Paper.book("notification_state")
                                        .read("application_state"))).after(new Date()))
                    return OpStatus.INFECTED;
                else
                    Paper.book("notification_state").delete("application_state");
        } catch (ParseException e) {
                e.printStackTrace();
                return OpStatus.ERROR;
        }

        return OpStatus.NOT_INFECTED;
    }

    //  function for testing purpouse, removes the eventual infected status stored inside the local database
    //           - OpStatus.OK: function correctly executed
    //           - OpStatus.ERROR: an error has occurred during the request management
    public OpStatus forceNotInfected(){

        try{
            Paper.init(getBaseContext());
            //  if a flag of infection is stored we remove it
            if(Paper.book("notification_state").contains("application_state"))
                    Paper.book("notification_state").delete("application_state");
            return OpStatus.OK;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return OpStatus.ERROR;
        }


    }

    //  function called after the identification of an infection. Sends a broadcast to update all
    //  the service components to the infection layout to inform the user that could be infected
    public void infectionReaction(){

        try {
            //  notifications expire time 14 days
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, 14);
            Paper.init(getBaseContext());
            //  if the user has stored information we update them in order to increase the expire
            if (Paper.book("notification_state").contains("application_state"))
                Paper.book("notification_state").delete("application_state");
            else {
                //  otherwise we advise the GUI to show a notification and store the expiring
                String info = "WARNING!!\n You could have been in contact with an infected person\n";
                Intent intent = new Intent(ACTION_BROADCAST);
                intent.putExtra("Contact", info);
                if (LocalBroadcastManager.getInstance(this).sendBroadcast(intent))
                    Log.info(this.getClass().getName(), "Message sent");
            }
            Paper.book("notification_state").write("application_state", calendar.getTime().toString());
        }catch(RuntimeException e){
            e.printStackTrace();
        }

    }

}
