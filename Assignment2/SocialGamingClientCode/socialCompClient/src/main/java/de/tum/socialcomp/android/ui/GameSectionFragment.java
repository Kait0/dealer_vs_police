package de.tum.socialcomp.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.NumberPicker;

import com.facebook.AccessToken;
import com.google.firebase.iid.FirebaseInstanceId;

import de.tum.socialcomp.android.Configuration;
import de.tum.socialcomp.android.MainActivity;
import de.tum.socialcomp.android.R;
import de.tum.socialcomp.android.webservices.util.HttpPoster;


/**
 * This Fragment is used to display the game statistics
 * from the webservice. Instead of using yet another
 * JSON based request we simply show a website
 * (the index site) hosted by our webservice in an
 * embedded WebView (Browser).
 *
 * @author Niklas Klügel
 *
 */
public class GameSectionFragment extends Fragment {
	public static View rootView;
    public static int currentTeam = -1; //no team
    public static Activity mainAct;

    public static String amount = "AMT";
    public static String money = "MONEY";
    public static String price = "PRICE";

    FragmentLoadListener listener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_section_game, container, false);
		TextView textView = (TextView)rootView.findViewById(R.id.gameTextView);
		textView.setText("Please join a Team!");


        rootView.findViewById(R.id.join_team_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity act = (MainActivity) getActivity();
                        if(act != null){
                            new HttpPoster().execute("users",
                                    act.getFacebookID(),
                                    "team");
                        }
                    }
                });


        return rootView;
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainAct = activity;
    }


    /*
	public static void updateGameText()
    {
        TextView textView = (TextView) rootView.findViewById(R.id.gameTextView);
        if (currentTeam == -1)
        {
            textView.setText("Please join a Team!");
        }
        else if (currentTeam == 0)
        {
            textView.setText("Money: 100000000 $\nDrugs: 10x Coffee");
        }
        else if (currentTeam == 1)
        {
            textView.setText("CHAT");
        }
        else if (currentTeam == 2)
        {
            textView.setText("YOU ARE GOD");
        }
    }
    */

    public static void updateDealMenu()
    {
        View view = rootView.findViewById(R.id.gameContentLayout);
        ViewGroup parent = (ViewGroup) view.getParent();
        int index = parent.indexOfChild(view);
        parent.removeView(view);
        view = mainAct.getLayoutInflater().inflate(R.layout.layout_deal, parent, false);
        parent.addView(view, index);
        TextView textView;
        //TextView textView = (TextView)view.findViewById(R.id.dealName);
        //textView.setText("NAME");
        textView = (TextView)view.findViewById(R.id.dealAmount);
        textView.setText("Amount owned: "+amount);
        textView = (TextView)view.findViewById(R.id.dealPrice);
        textView.setText("Price: "+ price+"$");
        textView = (TextView)view.findViewById(R.id.dealCurrentMoney);
        textView.setText("Money: "+ money+"$");
        final NumberPicker amtPick = (NumberPicker)view.findViewById(R.id.amountPicker);
        amtPick.setMinValue(1);
        amtPick.setMaxValue(5);
        amtPick.setWrapSelectorWheel(false);
        rootView.findViewById(R.id.buyButton)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity act = (MainActivity) mainAct;
                        int amount = amtPick.getValue();
                        if(act != null){
                            new HttpPoster().execute("games",
                                    act.getFacebookID(),
                                    amount+"",
                                    "buyDrugs");
                        }
                        GameSectionFragment.updateContents();
                    }
                });

        rootView.findViewById(R.id.sellButton)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity act = (MainActivity) mainAct;
                        int amount = amtPick.getValue();
                        if(act != null){
                            new HttpPoster().execute("games",
                                    act.getFacebookID(),
                                    amount+"",
                                    "sellInformation");
                        }
                    }
                });
    }

    public static void updateContents()
    {
        if (currentTeam == 0)
        {
            updateDealMenu();
            MainActivity act = (MainActivity) mainAct;
            new HttpPoster().execute("games", "deal", "data", act.getFacebookID());
            return;
        }

        View view = rootView.findViewById(R.id.gameContentLayout);
        ViewGroup parent = (ViewGroup) view.getParent();
        int index = parent.indexOfChild(view);
        parent.removeView(view);

        if (currentTeam == -1)
        {
            view = mainAct.getLayoutInflater().inflate(R.layout.layout_chat, parent, false);
            parent.addView(view, index);
            TextView textView = (TextView)view.findViewById(R.id.gameTextView);
            textView.setText("Please join a Team!");
        }
        else if (currentTeam == 1)
        {
            view = mainAct.getLayoutInflater().inflate(R.layout.layout_chat, parent, false);
            parent.addView(view, index);
            View textView = view.findViewById(R.id.chatText);
            ((TextView)textView).setText("CHAT - not implemented yet");
        }
        else if (currentTeam == 2)
        {
            view = mainAct.getLayoutInflater().inflate(R.layout.layout_chat, parent, false);
            parent.addView(view, index);
            View textView = view.findViewById(R.id.chatText);
            ((TextView)textView).setText("YOU ARE GOD");
        }
    }

    public void setFragmentLoadListener(FragmentLoadListener listener) {
        this.listener = listener;
    }
}