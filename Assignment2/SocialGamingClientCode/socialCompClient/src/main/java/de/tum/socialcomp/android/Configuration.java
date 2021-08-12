package de.tum.socialcomp.android;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Simple class that holds all configuration specific information for the game.
 * 
 * @author Niklas Klügel
 *
 */

public class Configuration {

    // if the server runs on your PC use your IP with :9000 in the end
    // get your IP by executing ipconfig in the cmd. Look at IPv4 for your IP.
	// TODO: Please insert the IP of your server here (plus the port) hat Vadim gemacht
	public static final String ServerURL = getApplicationContext().getResources().getString(R.string.serv_ip);

	// This is the minimum distance the user should have moved to trigger a location update
	public static final float MinimumDistanceForLocationUpdates = 5f; 
	
	public static final double DefaultLongitude = 11.0;
	public static final double DefaultLatitude = 48.0;

}
