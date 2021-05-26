package com.geotracer.geotracer.testingapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.UserStatus;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.service.GeotracerService;
import com.geotracer.geotracer.topicMessagesBroadcast.TopicMessagesActivity;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import java.util.List;

public class TestingActivity extends AppCompatActivity {


    LogService logService;
    BroadcastReceiver notificationReceiver;
    BroadcastReceiver logServiceReceiver;
    NotificationSender notificationSender;
    private FirestoreManagement firestore;
    private KeyValueManagement keyValueManagement;
    private GeotracerService geotracerMainService;

    public static final String TESTING_ACTIVITY_LOG = "TestingActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);


        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");

        /*
        SET THE BUTTON STATUS
         */



        /*
        BROADCAST RECEIVER FOR THE LOG SERVICE
         */
        LocalBroadcastManager.getInstance(TestingActivity.this).registerReceiver(
                logServiceReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        //Log.d(TESTING_ACTIVITY_LOG, "LogService BROADCAST LISTENER");
                        String toLog = intent.getStringExtra("LogMessage");

                        tv.append(toLog);
                        ScrollView sv = (ScrollView) findViewById(R.id.scrollview);
                        sv.fullScroll(ScrollView.FOCUS_DOWN);
                        Log.d(TESTING_ACTIVITY_LOG, toLog);



                    }

                },new IntentFilter(LogService.ACTION_BROADCAST)

        );

        /*
        BROADCAST RECEIVER FOR NOTIFICATIONS COMING FROM THE DB
         */
        LocalBroadcastManager.getInstance(TestingActivity.this).registerReceiver(
                notificationReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(TESTING_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");

                        TextView tv = new TextView(TestingActivity.this);
                        showPopupWindow(tv, toLog);


                        FrameLayout frameLayout = findViewById(R.id.contact_frame);
                        frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        TextView contact_text = findViewById(R.id.contact_text);
                        contact_text.setText(getResources().getString(R.string.contacts));
                        ((UserStatus) TestingActivity.this.getApplication()).setContacts(true);
                    }
                },new IntentFilter(NotificationSender.ACTION_BROADCAST)

        );


        /*
        ENABLE/DISABLE MAIN SERVICE
         */

        Switch main_service = (Switch) findViewById(R.id.mainServiceToggle);
        main_service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    /* FIXME */
                    if(geotracerMainService == null)
                    {
                        Intent i = new Intent(TestingActivity.this, GeotracerService.class);
                        startService(i);
                        bindService(i, geotracerService, Context.BIND_AUTO_CREATE);

                        String s = "Geotracer service started";


                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();


                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is already started!");

                }else{
                    if(geotracerMainService != null)
                    {
                        unbindService(geotracerService);
                        Intent i = new Intent(TestingActivity.this, GeotracerService.class);
                        stopService(i);

                        geotracerMainService = null;
                        String s = "Geotracer service stopped";
                        Log.d(TESTING_ACTIVITY_LOG, s);


                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();


                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service already stopped");

                }
            }
        });

        /*
        ENABLE/DISABLE SIGNATURE DISSEMINATION
         */

        Switch signature_dissemination = (Switch) findViewById(R.id.signaturesDisseminationToggle);
        signature_dissemination.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.startAdvertising();
                        if(result)
                        {
                            String s = "Signature dissemination started";


                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();



                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Signature Dissemination Already Started");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }
                else{
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.stopAdvertising();
                        if(result)
                        {


                            String s = "Signature Dissemination stopped";


                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();


                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Signature Dissemination Already Stopped");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                    }


            }
        });

        /*
        ENABLE/DISABLE SIGNATURE COLLECTION
         */

        Switch signature_collection = (Switch) findViewById(R.id.signaturesCollectionToggle);
        signature_collection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.startScanning();
                        if(result)
                        {
                            String s = "Signature Collection Started";

                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();

                            Log.d(TESTING_ACTIVITY_LOG, s);
                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Signature Collection Already Started");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }
                else{
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.stopScanning();
                        if(result)
                        {
                            String s = "Signature Collection Stopped";
                            Log.d(TESTING_ACTIVITY_LOG, s);

                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();

                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Signature Collection Already Stopped");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }


            }
        });

        /*
        ENABLE/DISABLE GEOLOCALIZATION

         */

        Switch user_localization = (Switch) findViewById(R.id.userLocalizationToggle);
        user_localization.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.startLocalization();
                        if(result)
                        {
                            String s = "Localization Started";
                            Log.d(TESTING_ACTIVITY_LOG, s);


                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();


                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Localization Already Started");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }
                else{
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.stopLocalization();
                        if(result)
                        {
                            String s = "Localization stopped";
                            Log.d(TESTING_ACTIVITY_LOG, s);


                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();

                        }
                        else
                        {
                            Log.d(TESTING_ACTIVITY_LOG,"Localization Already Stopped");
                        }
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }


            }
        });

        /*

        USER INFECTION NOTIFICATION

         */

        Switch user_infected = (Switch) findViewById(R.id.userInfectedToggle);
        user_infected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    RetStatus<List<BaseLocation>> userPositions = keyValueManagement.positions.getAllPositions();
                    if(userPositions.getStatus() == OpStatus.OK){
                        firestore.insertInfectedLocations(userPositions.getValue());
                        notificationSender.infectionAlert();

                        String s = "Positivity Report Activated";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();

                    }


                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(TESTING_ACTIVITY_LOG, "Positivity Report Disabled");
                }


            }
        });

        /*
        ENABLE/DISABLE PROXIMITY NOTIFICATION
         */

        Switch proximity_notifications = (Switch) findViewById(R.id.proximityNotificationToggle);
        proximity_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE


                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.enableProximityWarnings();
                        if(result) {
                            String s = "Proximity Warning Notifications Enabled";
                            Log.d(TESTING_ACTIVITY_LOG, s);

                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();

                        }
                        else
                            Log.d(TESTING_ACTIVITY_LOG, "Proximity Warning Notifications Enabled Already Enabled");
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");


                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE


                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.disableProximityWarnings();
                        if(result) {

                            String s = "Proximity Warning Notifications Disabled";
                            Log.d(TESTING_ACTIVITY_LOG, s);


                            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM, 0, 0);
                            toast.show();

                        }else
                            Log.d(TESTING_ACTIVITY_LOG, "Proximity Warning Notifications Already Disabled");
                    }
                    else
                        Log.w(TESTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                }
            }
        });
        initBottomMenu();

    }

    /*
    MENU ICON MANAGEMENT
     */
    private void initBottomMenu() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.testing_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i;
                switch (item.getItemId()) {

                    case R.id.from_testing_to_main:
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.from_testing_to_info:
                        i = new Intent(getApplicationContext(), InfoActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.from_testing_to_topic:
                        i = new Intent(getApplicationContext(), TopicMessagesActivity.class);
                        startActivity(i);
                        return true;
                    default:
                        return TestingActivity.super.onOptionsItemSelected(item);
                }
            }
        });
    }


    /*
    CONNECTION WITH THE NOTIFICATION SERVICE
     */
    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) service;
            notificationSender = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            notificationSender = null;

        }
    };

    /*
    CONNECTION WITH THE LOG SERVICE
     */
    private ServiceConnection logServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LogService.LocalBinder binder = (LogService.LocalBinder) s;
            logService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            logService = null;
        }
    };

    /*
    CONNECTION WITH THE FIRESTORE MANAGER SERVICE
     */
    private final ServiceConnection firestoreService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            FirestoreManagement.LocalBinder binder = (FirestoreManagement.LocalBinder) service;
            firestore = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            firestore = null;

        }
    };

    /*
    CONNECTION WITH THE KEYVALUE MANAGER SERVICE
     */
    private final ServiceConnection keyValueService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            KeyValueManagement.LocalBinder binder = (KeyValueManagement.LocalBinder) service;
            keyValueManagement = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            keyValueManagement = null;

        }
    };

    /*
    CONNECTION WITH THE MAIN APPLICATION SERVICE
      */
    private final ServiceConnection geotracerService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            GeotracerService.GeotracerBinder binder = (GeotracerService.GeotracerBinder) service;
            geotracerMainService = binder.getService();

            /*
            DEVO SETTARE LO STATO THE TOGGLES
             */

            Switch main_service = (findViewById(R.id.mainServiceToggle));
            Switch dissemination = (findViewById(R.id.signaturesDisseminationToggle));
            Switch collection = (findViewById(R.id.signaturesCollectionToggle));
            Switch localization = (findViewById(R.id.userLocalizationToggle));
            Switch proximity = findViewById(R.id.proximityNotificationToggle);


            if(geotracerMainService.isServiceStarted()){
                main_service.setChecked(true);
            }else
                main_service.setChecked(false);

            if(geotracerMainService.isAdvertising())
                dissemination.setChecked(true);
            else
                dissemination.setChecked(false);
            if(geotracerMainService.isScanning())
                collection.setChecked(true);
            else
                collection.setChecked(false);

            if(geotracerMainService.isLocalizing())
                localization.setChecked(true);
            else
                localization.setChecked(false);

            if(geotracerMainService.isProximityNotificationEnabled())
                proximity.setChecked(true);
            else
                proximity.setChecked(false);


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            geotracerMainService = null;

        }
    };

    protected void onResume() {
        super.onResume();
        /*
        RESTART ALL THE BROADCAST RECEIVERS
        */

        /*  Questo è richiesto altrimenti il main service non può partire */
        // Dynamic ACCESS_FINE_LOCATION permission check (required for API 23+)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                Toast.makeText(this,"Permission to access the device's location is required for using the service",Toast.LENGTH_SHORT).show();
            else  //Compatibility purposes
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        //NOTIFICATION RECEIVER
        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, iff);

        //FIRESTORE MANAGEMENT
        Intent intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

        //KEYVALUE MANAGEMENT
        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);

        //LOG SERVICE
        iff= new IntentFilter(LogService.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(logServiceReceiver, iff);


        //MAIN APPLICATION SERVICE
        intent = new Intent(this, GeotracerService.class);
        bindService(intent, geotracerService, Context.BIND_AUTO_CREATE);

        //SET THE VARIABLE FOR CONTACT NOTIFICATION STATUS
        if(((UserStatus) this.getApplication()).getContacts()) {
            FrameLayout frameLayout = findViewById(R.id.contact_frame);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
            TextView tv = findViewById(R.id.contact_text);
            tv.setText(getResources().getString(R.string.contacts));
        }

    }

    @Override
    protected void onPause(){
        super.onPause();

        /*
        STOP THE BROADCAST RECEIVER AND UNBIND THE OTHER SERVICES.
         */

        //NOTIFICATION
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);

        //FIRESTORE
        if(firestoreService != null)
            unbindService(firestoreService);

        //KEYVALUE
        if(keyValueService != null)
            unbindService(keyValueService);


        //MAIN SERVICE
        if(geotracerMainService != null)
            unbindService(geotracerService);

        //LOGSERVICE
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logServiceReceiver);


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, LogService.class);
        bindService(intent, logServiceConnection, Context.BIND_AUTO_CREATE);

        //BIND NOTIFICATION SERVICE
        Intent intent2 = new Intent(this, NotificationSender.class);
        bindService(intent2, notificationService, Context.BIND_AUTO_CREATE);

        //BIND FIRESTORE SERVICE
        intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

        //BIND KEYVALUE SERVICE
        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);


        //BIND MAIN APPLICATION SERVICE
        intent = new Intent(this, GeotracerService.class);
        bindService(intent, geotracerService, Context.BIND_AUTO_CREATE);

        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(logServiceConnection != null)
            unbindService(logServiceConnection);
        if(notificationService != null)
            unbindService(notificationService);
    }




    /*
    IT DELETES THE OLD SIGNATURES FROM THE DB
     */

    public void delete(View view) {

        /*

        PERFORM THE ACTIONS NECESSARY TO THE DELETE FROM THE DB THE DATA OLDER THAN 14 DAYS.
        IF THE TESTING INTERFACE IS AVAILABLE TO EVERYONE I SUGGEST TO DELETE ONLY ITS DATA.
        A POPUP WINDOW WITH THE RESULT OF THE OPERATION WILL BE SHOWN
         */

        if(firestore.dropExpiredLocations()== OpStatus.OK) {
            String s = "Old data has been deleted from the database";
            Log.d(TESTING_ACTIVITY_LOG, s);

            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();

        }else{
            String s = "Error! Data not deleted";
            Log.d(TESTING_ACTIVITY_LOG, s);

            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }


    }

    /*
    IT CLEANS THE LOG WINDOW
     */

    public void emptyLog(View view) {
        /*

        THIS FUNCTION DELETE THE CONTENT FROM THE LOG WINDOW

         */
        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");

        try {
            Runtime.getRuntime().exec("logcat -b all -c");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
    IT SHOWS THE POPUP WINDOW
     */

    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) TestingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //it is used to take the resources from the popup.xml file
        View customView = layoutInflater.inflate(R.layout.popup,null);

        Button closePopupBtn = (Button) customView.findViewById(R.id.closePopupBtn);

        //instantiate popup window
        PopupWindow popupWindow = new PopupWindow(customView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        //display the popup window
        popupWindow.showAtLocation(location, Gravity.CENTER, 0, 0);


        TextView popup_view = (TextView) customView.findViewById(R.id.popup_text);
        popup_view.setText(message);

        //close the popup window on button click
        closePopupBtn.setOnClickListener(v -> popupWindow.dismiss());



    }






    /*
            SAVE STATE BEFORE CHANGING ACTIVITY

     */



    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*

    It manages delete menu. It discriminates between the choices and call the corrisponding drop function.


     */

    public void manageDelete(View view) {

        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(TestingActivity.this, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.delete_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(item -> {

            TextView tv = new TextView(TestingActivity.this);
            switch(item.getItemId()){
                case R.id.delete_my_positions:
                    //DELETE MY POSITIONS
                    if(keyValueManagement.positions.dropAllPositions() != OpStatus.OK) {
                        String s = "Error in removing positions";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();

                    }else {
                        String s = "All positions removed";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }
                    return true;
                case R.id.delete_my_signatures:
                    //DELETE MY SIGNATURES
                    if(keyValueManagement.signatures.removeAllSignatures() != OpStatus.OK) {
                        String s = "Error in removing signatures";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();

                    }else {
                        String s = "All signatures removed";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }
                    return true;
                case R.id.delete_rec_beacons:
                    //DELETE RECEIVED BEACONS
                    if(keyValueManagement.beacons.dropAllBeacons() != OpStatus.OK){
                        String s = "Error in removing beacons";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }

                    else {
                        String s = "All beacons removed";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();

                    }
                    return true;
                case R.id.delete_all:
                    //DELETE EVERYTHING
                    if(!keyValueManagement.cleanLocalStore()) {
                        String s = "Error in cleaning the local database";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }
                    else {
                        if(notificationSender.forceNotInfected()==OpStatus.OK) {
                            FrameLayout frameLayout = findViewById(R.id.contact_frame);
                            frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                            TextView contact_text = findViewById(R.id.contact_text);
                            contact_text.setText(getResources().getString(R.string.no_contacts));
                            ((UserStatus) TestingActivity.this.getApplication()).setContacts(false);
                            showPopupWindow(tv, "Local database cleaned");
                        }
                    }
                    return true;
                case R.id.delete_bucket:
                    if(notificationSender.removeAllBuckets() != OpStatus.OK){
                        String s = "Error in deleting buckets";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }

                    else{
                        String s = "All buckets removed";
                        Log.d(TESTING_ACTIVITY_LOG, s);

                        Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM, 0, 0);
                        toast.show();
                    }
                default:
                    return false;
            }

        });

        popup.show();//showing popup menu

    }



    public void getSignature(View view) {
        String signature = geotracerMainService.getSignature();
        Toast toast = Toast.makeText(getApplicationContext(), "Your signature is: " + signature, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    public void changeSignature(View view) {
        if(geotracerMainService.resetSignature()){
            String s = "Signature changed successfully";
            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }

    }

    public void getPosition(View view) {
        Location location = geotracerMainService.getLastLocation();
        if(location != null)
            {
                String lat = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                Toast toast = Toast.makeText(getApplicationContext(),"Latitude: "+lat+", Longitude: "+longitude,Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM,0,0);
                toast.show();
            }
        else
            {
                Toast toast = Toast.makeText(getApplicationContext(),"Unknown",Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM,0,0);
                toast.show();
            }
    }

    public void gpsStatus(View view) {
        if(geotracerMainService.isGPSEnabled()){
            String s = "GPS is Enabled";
            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }else{
            String s = "GPS is OFF";
            Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 0);
            toast.show();
        }
    }
}
