package de.tum.socialcomp.android.webservices;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import de.tum.socialcomp.android.MainActivity;

/**
 * Created by simonschlegl on 01.05.17.
 *
 * This Service listens for new messages coming in through Firebase.
 * The messages are then forwarded to the receivedGameMessage() Method
 */
public class FirebaseListener extends FirebaseMessagingService {

    public static final String INTENT_FILTER = "INTENT_FILTER";
    public static final String DATA = "DATA";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d("INFORMATION", "Got Firebase message From: " + remoteMessage.getFrom());

        for( String key : remoteMessage.getData().keySet())
        {
            Log.d("INFORMATION","KEY: " + key + " -- VALUE: " + remoteMessage.getData().get(key));
        }

        Intent intent = new Intent(INTENT_FILTER);
        intent.putExtra(FirebaseListener.DATA, remoteMessage);
        sendBroadcast(intent);
    }
}
