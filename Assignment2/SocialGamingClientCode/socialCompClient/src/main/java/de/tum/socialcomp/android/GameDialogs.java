package de.tum.socialcomp.android;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import de.tum.socialcomp.android.ui.GameSectionFragment;
import de.tum.socialcomp.android.webservices.util.HttpPoster;

/**
 * This class is used for convenience to create
 * the various Dialogs that are shown within the app; 
 * these are mostly triggered by external events.
 * 
 * @author Niklas Klügel
 *
 */

public class GameDialogs {
	
	/**
	 * This Dilog is shown once a request to play a game is received.
	 * Depending on the user actions it either sends a message to accept or
	 * abort the game.
	 * 
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createGameRequestDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog requestDialog = builder
		        .setMessage("Do you want to play?\n" + gameMessage.get("user1Name") + " vs. " + gameMessage.get("user2Name"))
		        .setNegativeButton("No", new DialogInterface.OnClickListener() {    

		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	try {
							String gameID = gameMessage.get("gameID");
							
			            	new HttpPoster().execute(new String[]{"games", 
			            			gameID,
									//Auskommentiert da getFacebookID keine Parameter erwartet und getBaseContext nirgens deklariert wurde
        							act.getFacebookID(/*act.getBaseContext()*/),
									"abort"});
			            	
						} catch (Exception e) {
							e.printStackTrace();
						}
		      

		            }
		        })

		        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {


		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	try {
							String gameID = gameMessage.get("gameID");
							
			            	new HttpPoster().execute(new String[]{"games", 
			            			gameID,
									//Auskommentiert da getFacebookID keine Parameter erwartet und getBaseContext nirgens deklariert wurde
        							act.getFacebookID(/*act.getBaseContext()*/),
									"accept"});
			            	
						} catch (Exception e) {
							e.printStackTrace();
						}

		            }
		        })
		        .create();
		
		return requestDialog;
	}

	/**
	 * Shows an informational Dialog that the game was aborted due to users request.
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createUserAbortDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("Game aborted: \n" + gameMessage.get("aborterName") + " gave up!")
		        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }
		        
		        })
		        .create();
		
		return abortDialog;
	}

	/**
	 * Dialog shown once the game is established; this is used to indicate that the game
	 * waits for short term social interaction.
	 * 
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createGameStartDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {

		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("Game established, go shake the phone!")
		        .create();
		
	
		return abortDialog;
	}

	/**
	 * Informational dialog that the user has won.
	 * 
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createUserWonDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("You won against " + gameMessage.get("opponent") + "! Your score is now: "+ Double.parseDouble(gameMessage.get("score")))
		        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }
		        
		        })
		        .create();
		
		return abortDialog;
	}
	
	/**
	 * Informational dialog that the user has lost.
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createUserLostDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("You lost against " + gameMessage.get("opponent") + "! Your score is now: "+ Double.parseDouble(gameMessage.get("score")))
		        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }
		        
		        })
		        .create();
		
		return abortDialog;
	}
	
	/**
	 * Informational Dialog that the the game was a draw.
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createUserDrawDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("Draw!")
		        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }
		        
		        })
		        .create();
		
		return abortDialog;
	}
	
	/**
	 * Dialog that allows to send a "poke" message to another user selected from the map fragment.
	 * @param act
	 * @param userFacebookID
	 * @param recipentName
	 * @param recipentFacebookID
	 * @return
	 */
	public static Dialog createUserPokeDialog(Activity act, final String userFacebookID, String recipentName, final String recipentFacebookID){
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog pokeDialog = builder
		        .setMessage("Do you want to poke " + recipentName +"?")
		        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }
		        
		        })
		        .setPositiveButton("Poke", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {

		            	new HttpPoster().execute(new String[]{"games", 
		            			userFacebookID,
    							recipentFacebookID,
								"poke"});
		            }
		        
		        })
		        .create();
		
		return pokeDialog;
	}
	
	/**
	 * Informational dialog that indicates that the user was poked by another user.
	 * @param gameMessage
	 * @param act
	 * @return
	 * @throws JSONException
	 */
	public static Dialog createUserWasPokedDialog(final Map<String,String> gameMessage, final MainActivity act) throws JSONException {

		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		Dialog abortDialog = builder
		        .setMessage("You were poked by "+gameMessage.get("senderName"))
		        .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface arg0, int arg1) {
		            	// do nothing for now
		            }

		        })
		        .create();

		return abortDialog;
	}


	public static Dialog createTeamDialog(MainActivity act, String team)
	{
		Log.d("dia", "created Dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(act);

		String teamname;
		if (team.equals("0")) teamname = "Dealers";
		else if (team.equals("1")) teamname = "Police";
		else teamname = "Undefined";

		Dialog dia = builder
				.setMessage("Your Team is now: "+teamname)
				.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// do nothing for now
					}

				})
				.create();

		return dia;
	}


	public static Dialog createPopup(MainActivity act, String text)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(act);

		Dialog dia = builder
				.setMessage(text)
				.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// do nothing
					}

				})
				.create();

		return dia;
	}


	public static Dialog createSellMeassage(MainActivity act, final String amt, String earnings, final String userFacebookID)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(act);

		String text = "Do you want to sell "+amt+" for "+earnings+"$?";
		Dialog dia = builder
				.setMessage(text)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						new HttpPoster().execute(new String[]{"games",
								userFacebookID,
								amt,
								"sellDrugs"});
                        GameSectionFragment.updateContents();
					}

				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// do nothing
					}

				})
				.create();

		return dia;
	}
}
