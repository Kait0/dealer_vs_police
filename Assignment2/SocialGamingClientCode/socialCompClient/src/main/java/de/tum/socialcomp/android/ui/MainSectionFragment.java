package de.tum.socialcomp.android.ui;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;

import de.tum.socialcomp.android.Configuration;
import de.tum.socialcomp.android.MainActivity;
import de.tum.socialcomp.android.R;
import de.tum.socialcomp.android.webservices.util.HttpGetter;
import de.tum.socialcomp.android.webservices.util.HttpPoster;

/**
 * This Fragment is used to start the game, it simply
 * shows one button that triggers a request at the
 * webservice to start new game.
 *
 * @author Niklas Klügel
 */
public class MainSectionFragment extends Fragment {

    View rootView;
    FragmentLoadListener listener;
    Button b;
    TextView a1;
    TextView a2;
    TextView a3;
    TextView a4;
    TextView a5;
    TextView a6;
    TextView a7;

    TextView t1;
    TextView t2;
    TextView t3;
    TextView t4;
    TextView t5;
    TextView t6;
    TextView t7;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        rootView = inflater.inflate(R.layout.fragment_section_launchpad, container, false);
        listener.onFragmentLoaded(rootView.findViewById(R.id.welcome));

        a1 = (TextView) rootView.findViewById(R.id.a1);
        a2 = (TextView) rootView.findViewById(R.id.a2);
        a3 = (TextView) rootView.findViewById(R.id.a3);
        a4 = (TextView) rootView.findViewById(R.id.a4);
        a5 = (TextView) rootView.findViewById(R.id.a5);
        a6 = (TextView) rootView.findViewById(R.id.a6);
        a7 = (TextView) rootView.findViewById(R.id.a7);

        t1 = (TextView) rootView.findViewById(R.id.t1);
        t2 = (TextView) rootView.findViewById(R.id.t2);
        t3 = (TextView) rootView.findViewById(R.id.t3);
        t4 = (TextView) rootView.findViewById(R.id.t4);
        t5 = (TextView) rootView.findViewById(R.id.t5);
        t6 = (TextView) rootView.findViewById(R.id.t6);
        t7 = (TextView) rootView.findViewById(R.id.t7);


        b = (Button) rootView.findViewById(R.id.showInventory);

        b.setOnClickListener(new View.OnClickListener() {

                                 @Override
                                 public void onClick(View v) {


                                     String jsonString = "";


                                     //get JSON Object-Inventory as String
                                     String faceBookID = ((MainActivity) getActivity()).getFacebookID();
                                     //Test, if connection exists

                                     boolean connectionSucceded = checkForConnection(4000);
                                     if (connectionSucceded) {
                                         HttpGetter getter = new HttpGetter();
                                         //String [] para = {"users", faceBookID, "getUserData"};
                                         String[] para = {"users", faceBookID, "getUserData"};
                                         getter.execute(para);
                                         String requestResult = "Empty Result";
                                         try {
                                             System.out.println("-------------------------------------" + requestResult);
                                             requestResult = getter.get();
                                             Log.d("d", requestResult);
                                             System.out.println("-------------------------------------" + requestResult);
                                             jsonString = requestResult;
                                         } catch (Exception e) {
                                             Log.e("Map Exception: ", e.getMessage());
                                             jsonString = requestResult;
                                         }
                                         System.out.println("-------------------------------------" + jsonString);
                                         String[] keys = {"name", "role", "Inventory1", "Inventory2", "Inventory3", "latitude", "longitude"};
                                         String[] bla = getJSONasText(jsonString, keys);
                                         //System.out.println("-------------------------------------" + bla[0]);
                                         anzeigen(bla);
                                         b.setText("Showing Inventory");
                                     } else {
                                         b.setText("No Connection to Server!");

                                     }


                                 }
                             }
        );


        return rootView;
    }


    public void setFragmentLoadListener(FragmentLoadListener listener) {
        this.listener = listener;
    }


    private String[] getJSONasText(String jsonString, String[] keys) {
        //parsing the JSON-String
        int length = keys.length;
        String[] results = new String[length];
        int i = 0;
        while (i < length) {
            results[i] = "Empty Result";
            i++;
        }
        if (jsonString != "Empty Result" || jsonString != "fail" || keys == null) {
            try {

                JSONObject wholeObject = new JSONObject(jsonString);

                i = 0;
                while (i < length) {
                    results[i] = wholeObject.getString(keys[i]);
                    i++;
                }
                return results;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {

            return results;
        }
        return results;
    }

    //Inventar anzeigen lassen
    private void anzeigen(String[] bla) {
        String DEALER = "0";
        String POLIZIST = "1";

        if (bla[1].equals(DEALER)) {
            //name
            t1.setText(bla[0]);
            //Role
            t2.setText("Dealer");
            //geld
            t3.setText(bla[2] + "$");
            a3.setText("Geld:");
            //drogenart
            a4.setText("Drogen Art: ");
            t4.setText("Standard Droge");
            //anzahl Päckchen
            a5.setText("Anzahl Päckchen: ");
            t5.setText(bla[4]);
            //lattitude
            t6.setText(bla[5]);

            //longitude
            t7.setText(bla[6]);
        } else if (bla[1].equals(POLIZIST)) {
            //name
            t1.setText(bla[0]);
            //Role
            t2.setText("Polizist");
            //Justice Points
            t3.setText(bla[2]);
            a3.setText("Justice Points:");
            //Anzahl gefangener Dealer
            a4.setText("Anzahl gefangener" + "\n" + "Dealer heute: ");
            t4.setText(bla[3]);
            //Anzahl gefangener Dealer gesamt
            a5.setText("Anzahl gefangener" + "\n" + "Dealer gesamt: ");
            t5.setText(bla[4]);
            //lattitude
            t6.setText(bla[5]);

            //longitude
            t7.setText(bla[6]);
        }


    }
    public static boolean checkForConnection(int warteZeitinMilisekunden){
        String ip = new Configuration().ServerURL;
        URL url = null;
        int port = -1;
        boolean connectionSucceded = false;
        String host = "";

        try {
            url = new URL(ip);
            port = url.getPort();

            System.out.println("port: " + port);

            host = url.getHost();
        } catch (Exception e) {
            System.out.println("Failed to translate ipadress");
        }
        Socket socket = null;
        try {
            System.out.println("Try Connecting...");
            socket = new Socket();
            SocketAddress sa = new InetSocketAddress(host, port);
            socket.connect(sa, warteZeitinMilisekunden);
            connectionSucceded = true;


        } catch (SocketTimeoutException e) {
            System.out.println("Can not connect");
            connectionSucceded = false;
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null && connectionSucceded == true) {
                System.out.println("Closing");
                socket.close();
                System.out.println("Closed");

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("ipAdresse: " + host);
        return connectionSucceded;
    }


}

