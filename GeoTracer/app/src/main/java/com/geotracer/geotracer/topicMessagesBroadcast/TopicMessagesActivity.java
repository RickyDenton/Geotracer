package com.geotracer.geotracer.topicMessagesBroadcast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.geotracer.geotracer.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

public class TopicMessagesActivity extends AppCompatActivity {

    private SharedPreferences sharedpreferences;
    private SharedPreferences.Editor editor;
    public static final String TOPICS = "TOPICS";
    public static final String FIRST_TOPIC = "FIRST_TOPIC";
    public static final String SECOND_TOPIC = "SECOND_TOPIC";

    private boolean isSubscribedToTheFirstTopic = false;
    private boolean isSubscribedToTheSecondTopic = false;

    //buttons for subscribe to the first and second topic
    private Button subscribeButton1;
    private Button subscribeButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_messages);

        //take the references of button in UI
        subscribeButton1 = findViewById(R.id.subscribeButton1);
        subscribeButton2 = findViewById(R.id.subscribeButton2);

        //recover preferences
        sharedpreferences = getSharedPreferences(TOPICS, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        isSubscribedToTheFirstTopic = sharedpreferences.getBoolean(FIRST_TOPIC,false);
        isSubscribedToTheSecondTopic = sharedpreferences.getBoolean(SECOND_TOPIC,false);

        Log.d("TopicMessageActvity",isSubscribedToTheFirstTopic+","+isSubscribedToTheSecondTopic);

        //init UI from last preferences
        if(isSubscribedToTheFirstTopic){
            subscribeButton1.setText("unsubscribe");
            subscribeButton1.setBackgroundColor(Color.parseColor("#00C320"));
        }else {
            subscribeButton1.setText("subscribe");
            subscribeButton1.setBackgroundColor(Color.parseColor("#E64A19"));
        }

        if(isSubscribedToTheSecondTopic){
            subscribeButton2.setText("unsubscribe");
            subscribeButton2.setBackgroundColor(Color.parseColor("#00C320"));
        }else {
            subscribeButton2.setText("subscribe");
            subscribeButton2.setBackgroundColor(Color.parseColor("#E64A19"));
        }


        //calls the subscribe / unsubscribe function
        subscribeButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if he is not subscribed to the topic
                if(subscribeButton1.getText()=="subscribe") {

                    //subscribe user to the first topic
                    TopicMessagesBroadcastManager.subscribeUserToTopic(TopicMessagesBroadcastManager.RULES_CIRCULARS_ORDINANCES_TOPIC)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //save preferences
                                    editor.putBoolean(FIRST_TOPIC,true);
                                    editor.commit();

                                    //topic added
                                    subscribeButton1.setText("unsubscribe");
                                    subscribeButton1.setBackgroundColor(Color.parseColor("#00C320"));
                                    //notify user
                                    Toast.makeText(getApplicationContext(),"You subscribed to the topic",Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //notify user
                            Toast.makeText(getApplicationContext(),"There was a problem. Try later.",Toast.LENGTH_SHORT).show();
                        }
                    });

                    //if he is subscribed to the topic
                }else {

                    //unsubscribe...
                    TopicMessagesBroadcastManager.unsubscribeUserToTopic(TopicMessagesBroadcastManager.RULES_CIRCULARS_ORDINANCES_TOPIC)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //save preferences
                                    editor.putBoolean(FIRST_TOPIC,false);
                                    editor.commit();

                                    subscribeButton1.setText("subscribe");
                                    subscribeButton1.setBackgroundColor(Color.parseColor("#E64A19"));
                                    //notify user
                                    Toast.makeText(getApplicationContext(),"You unsubscribed successfully",Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //notify user
                            Toast.makeText(getApplicationContext(),"There was a problem. Try later.",Toast.LENGTH_SHORT).show();
                            Log.d("TopicMessageActvity",e.getMessage());
                        }
                    });

                }
            }
        });


    //calls the subscribe / unsubscribe function
        subscribeButton2.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //if he is not subscribed to the topic
            if(subscribeButton2.getText()=="subscribe") {

                //subscribe user to the first topic
                TopicMessagesBroadcastManager.subscribeUserToTopic(TopicMessagesBroadcastManager.NUMBER_OF_NEW_AND_DEATHS_CASES_TOPIC)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //save preferences
                                editor.putBoolean(SECOND_TOPIC,true);
                                editor.commit();

                                //topic added
                                subscribeButton2.setText("unsubscribe");
                                subscribeButton2.setBackgroundColor(Color.parseColor("#00C320"));
                                //notify user
                                Toast.makeText(getApplicationContext(),"You subscribed to the topic",Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //notify user
                        Toast.makeText(getApplicationContext(),"There was a problem. Try later.",Toast.LENGTH_SHORT).show();
                    }
                });

                //if he is subscribed to the topic
            }else {

                //unsubscribe...
                TopicMessagesBroadcastManager.unsubscribeUserToTopic(TopicMessagesBroadcastManager.NUMBER_OF_NEW_AND_DEATHS_CASES_TOPIC)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //save preferences
                                editor.putBoolean(SECOND_TOPIC,false);
                                editor.commit();

                                subscribeButton2.setText("subscribe");
                                subscribeButton2.setBackgroundColor(Color.parseColor("#E64A19"));
                                //notify user
                                Toast.makeText(getApplicationContext(),"You unsubscribed successfully",Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //notify user
                        Toast.makeText(getApplicationContext(),"There was a problem. Try later.",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    });
}



}
