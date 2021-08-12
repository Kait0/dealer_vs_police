package de.tum.socialcomp.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.app.Activity;
import android.util.Base64;
import android.util.Log;
import android.view.Window;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * This LoginScreen is really just for Facebook.
 * It displays the Facebook Login button and starts the next
 * screen if login was successful.
 * If you want to know what's happening in depth, please consult
 * the facebook documentation.
 */

public class LoginScreen extends Activity {

    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login_screen);

        // This logs the Keyhash used by Facebook for App development
        // (convenience)
        this.logKeyhash();

        callbackManager = CallbackManager.Factory.create();

        final LoginScreen loginscreen = this;

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("INFORMATION","successfully logged into Facebook");

                        Intent mainActivityIntent = new Intent(loginscreen,MainActivity.class);
                        startActivity(mainActivityIntent);
                    }

                    @Override
                    public void onCancel() {
                        Log.d("INFORMATION","canceled Facebook login");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d("INFORMATION","error while loggin into facebook");
                    }
                });

        Profile fbProfile = Profile.getCurrentProfile();
        if (fbProfile != null){
            Intent mainActivityIntent = new Intent(loginscreen,MainActivity.class);
            startActivity(mainActivityIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * This is simply used to show the key hash that is used by
     * Facebook to identify the developer's devices.
     * Otherwise the application *WILL NOT* log in since the
     * Facebook application is not verified to run on this device.
     *
     * This is a convenience function; the keyhash can also be acquired
     * as shown in the Facebook introductory tutorial:
     * https://developers.facebook.com/docs/android/getting-started/
     *
     */
    private void logKeyhash() {
        // log the key hash so it can be pasted to the facebook developer
        // console
        try {

            PackageInfo info = getPackageManager().getPackageInfo(
                    "de.tum.socialcomp.android", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYHASH",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

}
