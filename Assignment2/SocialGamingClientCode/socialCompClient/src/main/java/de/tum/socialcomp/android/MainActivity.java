package de.tum.socialcomp.android;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import de.tum.socialcomp.android.sensor.LocationChangeListener;
import de.tum.socialcomp.android.sensor.OnLocationChangeInterface;
import de.tum.socialcomp.android.sensor.OnShakeListener;
import de.tum.socialcomp.android.sensor.ShakeListener;
import de.tum.socialcomp.android.ui.AppSectionsPagerAdapter;
import de.tum.socialcomp.android.ui.FragmentLoadListener;
import de.tum.socialcomp.android.ui.GameSectionFragment;
import de.tum.socialcomp.android.ui.MapSectionFragment;
import de.tum.socialcomp.android.webservices.FirebaseListener;
import de.tum.socialcomp.android.webservices.util.HttpGetter;
import de.tum.socialcomp.android.webservices.util.HttpPoster;


/**
 * This is class represents the main activity of the game,
 * it manages the UI but also the game logic including
 * setting up services such as the Facebook login data
 * and the Google Cloud Messaging. 
 * 
 * To maneuver through the code, it is best to start with the
 * 	void onCreate(Bundle savedInstanceState) - method.
 * 
 * 
 * 
 * @author Niklas Klügel
 *
 */

@SuppressLint({ "NewApi", "ValidFragment" })
public class MainActivity extends FragmentActivity implements ActionBar.TabListener, FragmentLoadListener{

	// Play Service specifics
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	// For SharedPreferences and GM & FacebookData
	private static final String PROPERTY_FACEBOOK_ID = "FacebookID";
	private static final String PROPERTY_FACEBOOK_NAME = "FacebookName";

	/**
	 * Tag used on log messages.
	 */
	private static final String GCM_TAG = "GCM";
	private static final String FB_TAG = "Facebook";
	private String logMessageQueue = "";

	/**
	 * Shared attributes
	 */
	// private GoogleCloudMessaging gcm;
	private Context context;

	private ShakeListener shakeListener;
	private LocationManager locationManager;
	private LocationChangeListener locationChangeListener;

	private ViewPager viewPager;
	private AppSectionsPagerAdapter appSectionsPagerAdapter;

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra(FirebaseListener.DATA)) {
				receivedGameMessage(((RemoteMessage) intent.getExtras().get(FirebaseListener.DATA)).getData());
			}
		}
	};

	TextView debugView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// basic setup for application context
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		context = getApplicationContext();

		/**********************************
		 *** The important parts of the initialization start here
		 ***
		 **********************************/

		/*****
		 * 
		 * initializing the UserInterface ...
		 * 
		 *****/
		this.initPagination();
		this.initLogView();

		/*****
		 * 
		 * initializing the sensors ...
		 * 
		 *****/
		shakeListener = new ShakeListener(this);
		this.initLocationServices();

		registerReceiver(broadcastReceiver, new IntentFilter(FirebaseListener.INTENT_FILTER));


		/*****
		 *
		 * initializing Firebase and Facebook
		 *
		 *****/
		Log.d("INFORMATION","Firebase Instance Id is " + FirebaseInstanceId.getInstance().getToken());
		Log.d("INFORMATION","Facebook access token is " + AccessToken.getCurrentAccessToken());

		Profile fbProfile = Profile.getCurrentProfile();

		if( fbProfile != null )
		{
			Log.d("INFORMATION","Facebook profile is " + Profile.getCurrentProfile());

			// get last known location
			Location lastLocation = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			if (lastLocation != null) {
				// if the phone has never been located the last location
				// can be 0, use a default location
				new HttpPoster().execute("users",
						AccessToken.getCurrentAccessToken().getToken(),
						FirebaseInstanceId.getInstance().getToken(),
						"" + lastLocation.getLongitude(),
						"" + lastLocation.getLatitude(), "login");
			} else {
				new HttpPoster().execute("users",
						AccessToken.getCurrentAccessToken().getToken(),
						FirebaseInstanceId.getInstance().getToken(),
						"" + Configuration.DefaultLongitude,
						"" + Configuration.DefaultLatitude, "login");
			}

			//showLogMessage("Sent login data to GameServer");
			storeFacebookIDAndName(fbProfile.getId(),fbProfile.getName());
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkPlayServices();

		shakeListener.resume();
	}

	@Override
	public void onPause() {
		shakeListener.pause();
		super.onPause();
	}

	@Override
	public void onFragmentLoaded(View loaded) {
		debugView = (TextView) loaded;
		debugView.setText(logMessageQueue);
		logMessageQueue = "";
	}

	/**
	 * Initializes the tabs and adapter to show the tabs so the user can navigate between
	 * the three Fragments: MainSectionFragment, MapSectionFragment and GameSectionFragment
	 * 
	 */
	
	private void initPagination() {
		appSectionsPagerAdapter = new AppSectionsPagerAdapter(
				getSupportFragmentManager());

		appSectionsPagerAdapter.setFragmentLoadListener(this);
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();

		// because nobody needs this
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);

		// Specify that the Home/Up button should not be enabled, since there is
		// no hierarchical
		// parent.
		actionBar.setHomeButtonEnabled(false);

		// Specify that we will be displaying tabs in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Set up the ViewPager, attaching the adapter and setting up a listener
		// for when the
		// user swipes between sections.
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(appSectionsPagerAdapter);
		viewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						// When swiping between different app sections, select
						// the corresponding tab.
						// We can also use ActionBar.Tab#select() to do this if
						// we have a reference to the
						// Tab.
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < appSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(appSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}



	// this is used to show a timestamp
	private long startTime = 0L;
	public void initLogView() {
		startTime = System.currentTimeMillis();
	}


	@Override
	protected void onDestroy() {
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	// https://stackoverflow.com/questions/27077189/android-action-bar-disable-options-menu
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
		return false;
	}
	
	/**
	 * Shows a simple log message on the MainSectionFragment.
	 * This is used to show that the application logged in.
	 * 
	 * @param logMessage
	 */
	public void showLogMessage(final String logMessage) {
		final String message = (System.currentTimeMillis() - MainActivity.this.startTime)
				/ 1000 + ": " + logMessage;

		if (debugView == null){
			logMessageQueue = logMessageQueue.length() > 0 ? message.concat("\n").concat(logMessageQueue) : message;
			return;
		}

		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String append = debugView.getLineCount() > 0 ? "\n".concat(message) : message;
				debugView.append(append);
			}
		});
	}

	
	/**
	 * Initializes the location based services; when the location has changed
	 * a heartbeat signal is sent to the webservice updating the user's location
	 * in the database.
	 * 
	 */
	private void initLocationServices()
	{

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationChangeListener = new LocationChangeListener();

		// When the location has changed, then send an update to the webservice
		locationChangeListener.setOnLoctationChangedListener(new OnLocationChangeInterface()
		{
			@Override
			public void locationChanged(Location loc)
			{

				Log.d("INFORMATION","location has changed");

				String facebookID = MainActivity.this.getFacebookID();

				if (!facebookID.isEmpty())
				{
					new HttpPoster().execute(new String[]{"positions", facebookID, loc.getLongitude() + "", loc.getLatitude() + "", "update" });
				}

			}
		});

		// Use both, gps and network, on the cost of battery drain. But this way
		// we are likely to get some localization information in most cases.
		if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) )
		{
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, Configuration.MinimumDistanceForLocationUpdates, locationChangeListener);
		}
		else
		{
			Log.e(this.getClass().getName(), "Pos: GPS not available!");
		}
		
		if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) )
		{
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, Configuration.MinimumDistanceForLocationUpdates, locationChangeListener);
		}
		else
		{
			Log.e(this.getClass().getName(),"Pos: Net. Provider not available!");
		}
	}


	
	
	/**************************************************
	 * The next methods implement most of the client's game logic
	 **************************************************
	 **************************************************/
	
	
	/**
	 * Since the game messages are sent via the Google Cloud Messaging and therefore
	 * received at the client in another thread, we have to switch into the UI
	 * thread in order to display any dialogs or perform 
	 * other UI state modifying functions. 
	 * 
	 * In this way this method works as a dispatch for the received JSON objects onto
	 * the game logic that is handled in the UI thread (since it alters the UI).
	 * 
	 * @param gameMessage
	 */
	public void receivedGameMessage(final Map<String,String> gameMessage) {
		// Important: performing the processing in the UI thread is
		// necessary for the instantiation of ALL AsyncTasks (such as the http
		// request)

		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				processGameMessageOnUIThread(gameMessage);

			}
		});
	}

	/**
	 * Main game logic - it is a simple message dispatch showing dialogs
	 * for the respective messages received.
	 * 
	 * @param gameMessage
	 */
	public void processGameMessageOnUIThread(final Map<String,String> gameMessage) {

		try {
			String subtype = gameMessage.get("subtype");

			if (subtype.equals("request")) {

				// on request allow the user to join or abort the game,
				// this dialog will also take care of sending the respective
				// messages to
				// the game server
				showDialog(GameDialogs.createGameRequestDialog(gameMessage,
						this));

			} else if (subtype.equals("aborted")) {

				// if one user aborted, show who gave up
				showDialog(GameDialogs.createUserAbortDialog(gameMessage, this));

			} else if (subtype.equals("established")) {
				final String gameID = gameMessage.get("gameID");

				// if all users accepted, start the game
				final Dialog gameEstablishedDialog = GameDialogs
						.createGameStartDialog(gameMessage, this);
				OnShakeListener onShakeListener = new OnShakeListener() {

					@Override
					public void onShake() {

						// close the dialog and send a message to the server
						gameEstablishedDialog.dismiss();

						new HttpPoster().execute(new String[] {
								"games",
								gameID,
								MainActivity.this
										.getFacebookID(),
								"interaction" });

						// the listener will be automatically removed

					}
				};

				this.shakeListener.addOnShakeListenerOneShot(onShakeListener);

				showDialog(gameEstablishedDialog);

			} else if (subtype.equals("won")) {
				// the user won
				showDialog(GameDialogs.createUserWonDialog(gameMessage, this));

			} else if (subtype.equals("lost")) {
				// the user lost
				showDialog(GameDialogs.createUserLostDialog(gameMessage, this));

			} else if (subtype.equals("draw")) {
				// the game was a draw
				showDialog(GameDialogs.createUserDrawDialog(gameMessage, this));

			} else if (subtype.equals("poke")) {
				// the user was poked
				showDialog(GameDialogs.createUserWasPokedDialog(gameMessage,
						this));
			}

			else if (subtype.equals("dealData")) {
				GameSectionFragment.amount = gameMessage.get("amount");
				GameSectionFragment.money = gameMessage.get("money");
				GameSectionFragment.price = gameMessage.get("price");
				GameSectionFragment.updateDealMenu();
			}

			//Teamzuteilung
			else if (subtype.equals("team")) {
				String teamNr = gameMessage.get("teamnr");
				if (!teamNr.equals(GameSectionFragment.currentTeam+""))
					showDialog(GameDialogs.createTeamDialog(this, teamNr));
				updateGameTab(teamNr);
			}

			else if (subtype.equals("popup")) {
				String msg = gameMessage.get("msg");

				showDialog(GameDialogs.createPopup(this, msg));
			}

			else if (subtype.equals("sellData")) {
				String amt = gameMessage.get("amount");
				String earnings = gameMessage.get("earnings");
				showDialog(GameDialogs.createSellMeassage(this, amt, earnings, getFacebookID()));
			}
			else if(subtype.equals("process"))
			{
				String caugth = gameMessage.get("caught");
				//Runde die Werte weil sie sonst zu gross fuer die Anzeige sind
				float c = Float.parseFloat(caugth);
				c = Math.round(10000.f * c) / 100.f;
				//Dealer benachrichtigen das er gefangen wird
				if(c < 1.0 && GameSectionFragment.currentTeam == 0)
				{
					showDialog(GameDialogs.createPopup(this, "Achtun du wirst gefangen!"));
				}
				caugth = Float.toString(c);
				String fled = gameMessage.get("fled");
				c = Float.parseFloat(fled);
				c = Math.round(10000.f * c)/100.f;
				fled = Float.toString(c);
				TextView t = (TextView)findViewById(R.id.catchProcess);
				String helper = "Catch Process: " + caugth + "%";
				t.setText(helper);
				TextView t2 = (TextView)findViewById(R.id.fledProcess);
				helper = "Fleeing Process: " + fled + "%";
				t2.setText(helper);
			}
			else if(subtype.equals("done"))
			{
				String caugth = gameMessage.get("caught");
				float c = Float.parseFloat(caugth);
				if(c == 1)
				{
					TextView t = (TextView)findViewById(R.id.catchProcess);
					String helper = "Catch Process: 100.0%";
					t.setText(helper);
				}
				if(c == 0)
				{
					TextView t2 = (TextView)findViewById(R.id.fledProcess);
					String helper = "Fleeing Process: 100%";
					t2.setText(helper);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}



	// Teamzuteilung

	private void updateGameTab(String teamNr)
	{
		//initPagination();
		TextView textView = (TextView) GameSectionFragment.rootView.findViewById(R.id.gameTextView);
		if (teamNr.equals("0")) {
			appSectionsPagerAdapter.changePageTitle(2, "Deal");
			GameSectionFragment.currentTeam = 0;
		}
		else if (teamNr.equals("1")) {
			appSectionsPagerAdapter.changePageTitle(2, "Chat");
			GameSectionFragment.currentTeam = 1;
		}
		else if (teamNr.equals("2")) {
			appSectionsPagerAdapter.changePageTitle(2, "Admin");
			GameSectionFragment.currentTeam = 2;
		}
		getActionBar().getTabAt(2).setText(appSectionsPagerAdapter.getPageTitle(2));
		GameSectionFragment.updateContents();
	}


	/**************************************************
	 * The following methods are simply helper methods
	 * necessary to store/retrieve preferences or 
	 * to support the registration of the Google Cloud Messaging
	 * services or to alter the UI.
	 **************************************************
	 **************************************************/
	
	void showDialog(final Dialog dia) {
		dia.show();
	}
	

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			android.app.FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			android.app.FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		viewPager.setCurrentItem(tab.getPosition());
		if (tab.getPosition() == 2)
		{
			new HttpPoster().execute("users",
					this.getFacebookID(),
					"teamget");
			//if (GameSectionFragment.currentTeam != -1)
			//	((GameSectionFragment)appSectionsPagerAdapter.getItem(2)).updateContents();
		}
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			android.app.FragmentTransaction fragmentTransaction) {
	}


	private SharedPreferences getSharedPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences,
		return getSharedPreferences(MainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}



	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}


	private void storeFacebookIDAndName(String facebookID, String name) {
		final SharedPreferences prefs = getSharedPreferences(context);
		int appVersion = getAppVersion(context);

		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_FACEBOOK_ID, facebookID);
		editor.putString(PROPERTY_FACEBOOK_NAME, name);
		editor.apply();

	}

	public String getFacebookID() {
		String ret = "";

		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		String facebookID = prefs.getString(PROPERTY_FACEBOOK_ID, "");
		if (facebookID.isEmpty()) {
			Log.i(FB_TAG, "Facebook ID not found.");
			return "";
		} else {
			ret = facebookID;
		}

		return ret;
	}

	public String getFacebookName(Context context) {
		String ret = "";

		final SharedPreferences prefs = getSharedPreferences(context);
		String facebookName = prefs.getString(PROPERTY_FACEBOOK_NAME, "");
		if (facebookName.isEmpty()) {
			Log.i(FB_TAG, "Facebook ID not found.");
			return "";
		} else {
			ret = facebookName;
		}

		return ret;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(GCM_TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}


}
