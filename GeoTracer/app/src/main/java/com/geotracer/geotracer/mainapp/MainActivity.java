package com.geotracer.geotracer.mainapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;

import com.geotracer.geotracer.UserStatus;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.heatMap.HeatMap;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.service.GeotracerService;
import com.geotracer.geotracer.testingapp.TestingActivity;
import com.geotracer.geotracer.topicMessagesBroadcast.TopicMessagesActivity;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;




public class MainActivity extends AppCompatActivity {

    NotificationSender notificationSender;
    BroadcastReceiver notificationReceiver;

    public static final String MAIN_ACTIVITY_LOG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //FCM
        FirebaseMessaging.getInstance().subscribeToTopic("weather")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Done";
                        if (!task.isSuccessful()) {
                            msg = "Failed";
                        }
                        Log.d("TopicMessagesBroadcastManager", msg);
                    }
                });


        //---------

        /*
        Broadcast listener for contact notifications
         */

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                notificationReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(MAIN_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");

                        TextView tv = new TextView(MainActivity.this);
                        showPopupWindow(tv, toLog);

                        FrameLayout frameLayout = findViewById(R.id.contact_frame);
                        frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        TextView contact_text = findViewById(R.id.contact_text);
                        contact_text.setText(getResources().getString(R.string.contacts));
                        ((UserStatus) MainActivity.this.getApplication()).setContacts(true);

                        //notify user with a notification
                        sendNotificationToUser();

                    }
                },new IntentFilter(NotificationSender.ACTION_BROADCAST)

        );

        //initialize heat map fragment
        Fragment heatMapFragment = new HeatMap();

        //start heat map fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_heat_map_area, heatMapFragment) //insert heatMapFragment inside the area
                .commit();

        initBottomMenu();
    }

    private void sendNotificationToUser(){

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        String channelId = "InfectionChannel";

        NotificationCompat.Builder builder = new  NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.geotracer_icon)
                .setContentTitle(getResources().getString(R.string.titleInfectionAlarm))
                .setContentText(getResources().getString(R.string.contacts))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());

    }

    private void initBottomMenu() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i;
                switch(item.getItemId()){
                    case R.id.from_main_to_testing:
                        i = new Intent(getApplicationContext(), TestingActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.from_main_to_info:
                        i = new Intent(getApplicationContext(), InfoActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.from_info_to_topic:
                        i = new Intent(getApplicationContext(), TopicMessagesActivity.class);
                        startActivity(i);
                        return true;

                    default:
                        return MainActivity.super.onOptionsItemSelected(item);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, NotificationSender.class);
        bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onResume() {
        super.onResume();

        /* Questo è richiesto altrimenti il main service non può partire */
        // Dynamic ACCESS_FINE_LOCATION permission check (required for API 23+)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                Toast.makeText(this,"Permission to access the device's location is required for using the service",Toast.LENGTH_SHORT).show();
            else  //Compatibility purposes
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        /* FIXME: Questo si assicura che il main service giri (non è un problema se gira già) */
        Intent geoTracerService = new Intent(this,GeotracerService.class);
        startForegroundService(geoTracerService);


        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, iff);
        if(((UserStatus) this.getApplication()).getContacts()) {
            FrameLayout frameLayout = findViewById(R.id.contact_frame);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
            TextView tv = findViewById(R.id.contact_text);
            tv.setText(getResources().getString(R.string.contacts));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(notificationService != null)
            unbindService(notificationService);
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) s;
            notificationSender = binder.getService();

            Log.d(MAIN_ACTIVITY_LOG, "AM I INFECTED: " + String.valueOf(notificationSender.canIbeInfected()));
            if(notificationSender.canIbeInfected() == OpStatus.INFECTED){
                FrameLayout frameLayout = findViewById(R.id.contact_frame);
                frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                TextView contact_text = findViewById(R.id.contact_text);
                contact_text.setText(getResources().getString(R.string.contacts));
                ((UserStatus) MainActivity.this.getApplication()).setContacts(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            notificationService = null;

        }
    };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

    }

    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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
        closePopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });



    }


}