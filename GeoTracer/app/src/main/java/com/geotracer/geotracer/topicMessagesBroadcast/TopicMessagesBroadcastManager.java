package com.geotracer.geotracer.topicMessagesBroadcast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TopicMessagesBroadcastManager extends FirebaseMessagingService {

    //the topic
    public static final String RULES_CIRCULARS_ORDINANCES_TOPIC = "rules_circulars_ordinance";
    public static final String NUMBER_OF_NEW_AND_DEATHS_CASES_TOPIC = "new_cases_and_deaths";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //open main activity on click on notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        String channelId = "CovidChannel";

        NotificationCompat.Builder builder = new  NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.geotracer_icon)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody()).setAutoCancel(true).setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());
    }


    public static Task<Void> subscribeUserToTopic(String nameOfTopic){

        return FirebaseMessaging.getInstance().subscribeToTopic(nameOfTopic);

    }

    public static Task<Void> unsubscribeUserToTopic(String nameOfTopic){

        return FirebaseMessaging.getInstance().unsubscribeFromTopic(nameOfTopic);

    }
}
