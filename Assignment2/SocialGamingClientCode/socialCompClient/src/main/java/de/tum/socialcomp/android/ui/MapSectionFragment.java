package de.tum.socialcomp.android.ui;

import android.app.Dialog;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.tum.socialcomp.android.Configuration;
import de.tum.socialcomp.android.GameDialogs;
import de.tum.socialcomp.android.MainActivity;
import de.tum.socialcomp.android.R;
import de.tum.socialcomp.android.sensor.LocationChangeListener;
import de.tum.socialcomp.android.sensor.OnLocationChangeInterface;
import de.tum.socialcomp.android.webservices.FirebaseListener;
import de.tum.socialcomp.android.webservices.util.HttpGetter;
import de.tum.socialcomp.android.webservices.util.HttpPoster;

import android.content.BroadcastReceiver;

import static com.facebook.FacebookSdk.getApplicationContext;
import static de.tum.socialcomp.android.ui.GameSectionFragment.rootView;

/**
 * This Fragment shows the Map View for the game, it sports a refresh button
 * that requests all users that are nearby from the webservice and shows them on
 * the map.
 * <p>
 * The map used OSMDroid instead of Google Maps, so we dont have problems with
 * any Google specific Map Service restrictions such as request quota.
 * <p>
 * Most of the code is directly based on the examples given at:
 * https://code.google
 * .com/p/osmdroid/source/browse/#svn/trunk/OpenStreetMapViewer
 * <p>
 * There are two additional functions: - When another user is tapped on the map
 * the name is shown. - When performed a tap & hold on the other user he/she can
 * be poked (a dialog will appear).
 *
 * @author Niklas Klügel
 */

public class MapSectionFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    Location location;
    LocationManager locationManager;
    private LocationChangeListener locationChangeListener;


    Context context;

    private final static float ZOOM = 15f;

    // This hash map will be used for long presses on a user on the map to
    // resolve the facebookID
    private HashMap<Marker, String> markerToFacebookIDMap = new HashMap<Marker, String>();
    private Object hashMapMutex = new Object();

    //Wird als erstes ausgefuehrt
    private void createMapView() {
        try {

            SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

            mapFrag.getMapAsync(this);

        } catch (NullPointerException exception) {
            Log.e("mapApp", exception.toString());
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //Zoom festsetzen um Zoomproblem zu vermeiden
        map.setMinZoomPreference(14.0f);
        map.setMaxZoomPreference(18.0f);

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                synchronized (hashMapMutex) {
                    String recipientName = marker.getTitle();
                    String recipientFacebookID = markerToFacebookIDMap
                            .get(marker);

                    if (recipientFacebookID != null && !recipientFacebookID.isEmpty()) {
                        // show the user Dialog, sending the messages to the server
                        // backend is done there as well
                        Dialog pokeDialog = GameDialogs.createUserPokeDialog(
                                getActivity(), ((MainActivity) getActivity()).getFacebookID(),
                                recipientName, recipientFacebookID);

                        pokeDialog.show();
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationChangeListener = new LocationChangeListener();

        // When the location has changed, then send an update to the webservice
        locationChangeListener.setOnLoctationChangedListener(new OnLocationChangeInterface() {
            @Override
            public void locationChanged(Location loc) {
                Log.d("MAP", "Location has changed");
                map.clear();
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                map.setMyLocationEnabled(true);

                String locationProvider = LocationManager.GPS_PROVIDER;

                location = locationManager.getLastKnownLocation(locationProvider);
                if (location != null) {
                    LatLng myLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLng(myLoc));
                    //Kreis rendern
                    // Instantiates a new CircleOptions object and defines the center and radius in meters and the appearance
                    CircleOptions circleOptions = new CircleOptions().center(myLoc).radius(30).strokeColor(Color.BLUE).fillColor(Color.argb(125, 0, 174, 255)).strokeWidth(2);
                    Circle circle = map.addCircle(circleOptions);
                }
                String facebookID = ((MainActivity) getActivity()).getFacebookID();
                HttpGetter posters = new HttpGetter();
                posters.execute(new String[]{"users", facebookID, "getNearbyFences"});
                try {
                    String requestResu = posters.get();
                    if (requestResu.isEmpty() || !requestResu.equals("{ }")) {
                        JSONObject json = new JSONObject(requestResu);

                        ArrayList<Marker> userMarker = new ArrayList<Marker>();

                        synchronized (hashMapMutex) {
                            //Polizisten in der Naehe anzeigen wenn man Dealer ist
                            if ((GameSectionFragment.currentTeam == 0 || GameSectionFragment.currentTeam == 2)) {
                                JSONArray jsonFences = json.getJSONArray("fences");
                                JSONArray jsonPolice = json.getJSONArray("police");
                                for (int idx = 0; idx < jsonPolice.length(); idx++) {
                                    JSONObject jsonUser = jsonPolice.getJSONObject(idx);
                                    String userName = jsonUser.getString("Name");
                                    Double longitude = jsonUser.getDouble("longitude");
                                    Double latitude = jsonUser.getDouble("latitude");
                                    String fbID = jsonUser.getString("facebookID");
                                    //Add new marker to map for every user nearby
                                    Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
                                    markerToFacebookIDMap.put(user, fbID);
                                }
                                for (int idx = 0; idx < jsonFences.length(); idx++) {
                                    JSONObject jsonUser = jsonFences.getJSONObject(idx);
                                    String userName = jsonUser.getString("fenceID");
                                    Double longitude = jsonUser.getDouble("longitude");
                                    Double latitude = jsonUser.getDouble("latitude");
                                    //Add new marker to map for every fence nearby
                                    Marker hehler = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Hehler " + userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_hehler)));
                                }
                                JSONArray jsonDealer = json.getJSONArray("dealer");
                                for (int idx = 0; idx < jsonDealer.length(); idx++) {
                                    JSONObject jsonUser = jsonDealer.getJSONObject(idx);
                                    String userName = jsonUser.getString("Name");
                                    Double longitude = jsonUser.getDouble("longitude");
                                    Double latitude = jsonUser.getDouble("latitude");
                                    String fbID = jsonUser.getString("facebookID");
                                    //Add new marker to map for every user nearby
                                    Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                                    markerToFacebookIDMap.put(user, fbID);
                                }
                            }
                            //Dealer in der Naehe anzeigen wenn man Polizist ist
                            if ((GameSectionFragment.currentTeam == 1 || GameSectionFragment.currentTeam == 2)) {
                                JSONArray jsonDealer = json.getJSONArray("dealer");
                                for (int idx = 0; idx < jsonDealer.length(); idx++) {
                                    JSONObject jsonUser = jsonDealer.getJSONObject(idx);
                                    String userName = jsonUser.getString("Name");
                                    Double longitude = jsonUser.getDouble("longitude");
                                    Double latitude = jsonUser.getDouble("latitude");
                                    String fbID = jsonUser.getString("facebookID");
                                    //Add new marker to map for every user nearby
                                    Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                                    markerToFacebookIDMap.put(user, fbID);
                                }
                                JSONArray jsonPolice = json.getJSONArray("police");
                                for (int idx = 0; idx < jsonPolice.length(); idx++) {
                                    JSONObject jsonUser = jsonPolice.getJSONObject(idx);
                                    String userName = jsonUser.getString("Name");
                                    Double longitude = jsonUser.getDouble("longitude");
                                    Double latitude = jsonUser.getDouble("latitude");
                                    String fbID = jsonUser.getString("facebookID");
                                    //Add new marker to map for every user nearby
                                    Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
                                    markerToFacebookIDMap.put(user, fbID);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Map Exception: ", e.getMessage());
                }
            }
        });
        // Use both, gps and network, on the cost of battery drain. But this way
        // we are likely to get some localization information in most cases.
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, Configuration.MinimumDistanceForLocationUpdates, locationChangeListener);
        } else {
            Log.e(this.getClass().getName(), "Pos: GPS not available!");
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, Configuration.MinimumDistanceForLocationUpdates, locationChangeListener);
        } else {
            Log.e(this.getClass().getName(), "Pos: Net. Provider not available!");
        }

        View rootView = inflater.inflate(R.layout.fragment_section_map, container, false);
        Log.d("team", Integer.toString(GameSectionFragment.currentTeam));
        rootView.findViewById(R.id.catchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((GameSectionFragment.currentTeam == 1 || GameSectionFragment.currentTeam == 2)) {
                    Log.d("Listen", "Aha du bist ein Polizist");
                    String facebookID = ((MainActivity) getActivity()).getFacebookID();
                    HttpPoster posters = new HttpPoster();
                    posters.execute(new String[]{"games", facebookID, "Catch"});
                    try {
                        String requestResu = posters.get();
                        Log.d("Catchthread", requestResu);
                    } catch (Exception e) {
                        Log.e("Map Exception: ", e.getMessage());
                    }
                }
            }
        });
        rootView.findViewById(R.id.refresh_map_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //
                        //Check for connection edited by Matija
                        boolean connectionSucceded = new MainSectionFragment().checkForConnection(2000);
                        if (connectionSucceded) {

                            map.clear();
                            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                            map.setMyLocationEnabled(true);

                            String locationProvider = LocationManager.GPS_PROVIDER;

                            location = locationManager.getLastKnownLocation(locationProvider);
                            if (location != null) {
                                LatLng myLoc = new LatLng(location.getLatitude(), location.getLongitude());
                                map.moveCamera(CameraUpdateFactory.newLatLng(myLoc));

                                CircleOptions circleOptions = new CircleOptions().center(myLoc).radius(30).strokeColor(Color.BLUE).fillColor(Color.argb(125, 0, 174, 255)).strokeWidth(2);
                                Circle circle = map.addCircle(circleOptions);
                            }
                            String facebookID = ((MainActivity) getActivity()).getFacebookID();
                            HttpGetter posters = new HttpGetter();
                            posters.execute(new String[]{"users", facebookID, "getNearbyFences"});
                            try {
                                String requestResu = posters.get();
                                if (requestResu.isEmpty() || !requestResu.equals("{ }")) {
                                    JSONObject json = new JSONObject(requestResu);

                                    ArrayList<Marker> userMarker = new ArrayList<Marker>();

                                    synchronized (hashMapMutex) {
                                        //Polizisten in der Naehe anzeigen wenn man Dealer ist
                                        if ((GameSectionFragment.currentTeam == 0 || GameSectionFragment.currentTeam == 2)) {
                                            JSONArray jsonFences = json.getJSONArray("fences");
                                            JSONArray jsonPolice = json.getJSONArray("police");
                                            for (int idx = 0; idx < jsonPolice.length(); idx++) {
                                                JSONObject jsonUser = jsonPolice.getJSONObject(idx);
                                                String userName = jsonUser.getString("Name");
                                                Double longitude = jsonUser.getDouble("longitude");
                                                Double latitude = jsonUser.getDouble("latitude");
                                                String fbID = jsonUser.getString("facebookID");
                                                //Add new marker to map for every user nearby
                                                Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
                                                markerToFacebookIDMap.put(user, fbID);
                                            }
                                            for (int idx = 0; idx < jsonFences.length(); idx++) {
                                                JSONObject jsonUser = jsonFences.getJSONObject(idx);
                                                String userName = jsonUser.getString("fenceID");
                                                Double longitude = jsonUser.getDouble("longitude");
                                                Double latitude = jsonUser.getDouble("latitude");
                                                //Add new marker to map for every fence nearby
                                                Marker hehler = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Hehler " + userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_hehler)));
                                            }
                                            JSONArray jsonDealer = json.getJSONArray("dealer");
                                            for (int idx = 0; idx < jsonDealer.length(); idx++) {
                                                JSONObject jsonUser = jsonDealer.getJSONObject(idx);
                                                String userName = jsonUser.getString("Name");
                                                Double longitude = jsonUser.getDouble("longitude");
                                                Double latitude = jsonUser.getDouble("latitude");
                                                String fbID = jsonUser.getString("facebookID");
                                                //Add new marker to map for every user nearby
                                                Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                                                markerToFacebookIDMap.put(user, fbID);
                                            }
                                        }
                                        //Dealer in der Naehe anzeigen wenn man Polizist ist
                                        if ((GameSectionFragment.currentTeam == 1 || GameSectionFragment.currentTeam == 2)) {
                                            JSONArray jsonDealer = json.getJSONArray("dealer");
                                            for (int idx = 0; idx < jsonDealer.length(); idx++) {
                                                JSONObject jsonUser = jsonDealer.getJSONObject(idx);
                                                String userName = jsonUser.getString("Name");
                                                Double longitude = jsonUser.getDouble("longitude");
                                                Double latitude = jsonUser.getDouble("latitude");
                                                String fbID = jsonUser.getString("facebookID");
                                                //Add new marker to map for every user nearby
                                                Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)));
                                                markerToFacebookIDMap.put(user, fbID);
                                            }
                                            JSONArray jsonPolice = json.getJSONArray("police");
                                            for (int idx = 0; idx < jsonPolice.length(); idx++) {
                                                JSONObject jsonUser = jsonPolice.getJSONObject(idx);
                                                String userName = jsonUser.getString("Name");
                                                Double longitude = jsonUser.getDouble("longitude");
                                                Double latitude = jsonUser.getDouble("latitude");
                                                String fbID = jsonUser.getString("facebookID");
                                                //Add new marker to map for every user nearby
                                                Marker user = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(userName).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
                                                markerToFacebookIDMap.put(user, fbID);
                                            }
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                Log.e("Map Exception: ", e.getMessage());
                            }
                        }
                    }
                });
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        //create the Map
        createMapView();
    }

    private void centerMapOnMyLocation() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        map.setMyLocationEnabled(true);

        String locationProvider = LocationManager.NETWORK_PROVIDER;

        location = locationManager.getLastKnownLocation(locationProvider);

        if (location != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), ZOOM));
        }

    }
}
