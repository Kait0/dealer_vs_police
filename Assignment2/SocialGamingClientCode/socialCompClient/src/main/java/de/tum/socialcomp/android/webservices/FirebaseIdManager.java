package de.tum.socialcomp.android.webservices;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by simonschlegl on 03.05.17.
 *
 * Sometimes the Firebase Device ID can change.
 * You can handle this here.
 */

public class FirebaseIdManager extends FirebaseInstanceIdService
{
    @Override
    public void onTokenRefresh() {

        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("INFORMATION", "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // sendRegistrationToServer(refreshedToken);
    }
}
