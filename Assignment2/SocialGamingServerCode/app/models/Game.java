package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import play.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import java.lang.Iterable;

/**
 * This class is for the database representation of the state of a Game 
 * AND most of the state-specific game logic
 * 
 * The game is a two-player game;
 * The user credentials such as Facebook ID and name are saved here in order
 * circumvent additional look-ups and to be able to show game statistics.
 * 
 *  
 * @author Niklas Klügel
 *
 */

public class Game {

	// used by Jongo to map JVM objects to database objects
	@JsonProperty("_id")
	public String id;
    
	
    // Credentials of the users
    public String firstUserFbID = "";
    public String secondUserFbID= "";
    
    private boolean firstUserAccepted = false;
    private boolean secondUserAccepted = false;
    
    public String firstUserName = "";
    public String secondUserName= "";
    
    public Long firstUserInteractionTimeStamp = -1L;
    public Long secondUserInteractionTimeStamp= -1L;
    
    public String winnerFbID = "";
    public String winnerName = "";
    
    public Date date = new Date();
    
    public static final String StateInitializing = "InInitializingState";
    public static final String StateProgress = "InProgressState";
    public static final String StateFinished = "InFinishedState";
    public static final String StateAborted = "InAbortedState";
    
    private boolean aborted = false;
    
    private String state = StateInitializing;


	//Spielerlisten: Ich denke, es ist einfacher, wenn das Spiel zumindest die Anzahl der Spieler und deren Namen kennt
    public List<User> police;
    public List<User> dealers;
	
	
	
	//Teamzuteilung
	public static void joinUserToTeam(User user) {
		//if (user.role != 2) return;
		
		//boolean result = flipCoin(0.5);
		Logger.info("Role: "+user.role);
		if (user.role == 1 || user.role == 2) {
			user.setRole(0);
			//zum Debuggen: User Money wird auf 1000 gesetzt
			int[] inv = user.getInv();
			inv[0] = 1000;
			inv[2] = 0;
			user.setInv(inv);
		}
		else user.setRole(1);
		
		Logger.info("User Team changed to: "+user.role);
		Logger.info("User Money: "+user.inv[0][0]);
		Logger.info(user.toString());
		
		//Send Message to User
		try{
			Map<String, String> message = new HashMap<>();
			message.put("type", "game");
			message.put("subtype", "team");
			message.put("teamnr", ""+user.role);
			user.sendMessage(message);
		} catch(IOException ex)
		{
			Logger.info("Could not send Message to User");
		}
	}
	
	public static void sendPopup(User user, String msg)
	{
		Logger.info("Sending message: "+msg);
		try{
			Map<String, String> message = new HashMap<>();
			message.put("type", "game");
			message.put("subtype", "popup");
			message.put("msg", msg);
			user.sendMessage(message);
		} catch(IOException ex)
		{
			Logger.info("Could not send Message to User");
		}
		
	}
	
    
    /****************
     * Object methods
     * -------------
     ***************/
    
    /// these methods involve game logic
    
    /**
     * To be called when a user accepted a game, 
     * establishes a game as soon as both user have accepted it.
     * Sends an established-message to both users.
     * 
     * @param facebookId
     * @throws IOException
     */
    
//TODO: Es muss nur ein Spieler akzeptieren, denn das Spiel "läuft" bereits. Spieler in die Liste einfügen
    public void accept(String facebookId) throws IOException{
    	Logger.info("accepted game");
    	
    	if(firstUserFbID.equals(facebookId)) {
    		firstUserAccepted = true;
    	}
    	
    	if(secondUserFbID.equals(facebookId)) {
    		secondUserAccepted = true;
    	}

		GameRepository.getInstance().update(this);
    	
    	if(this.isEstablished() && this.state.equals(StateInitializing)){
    		
    		this.state = StateProgress;
			GameRepository.getInstance().update(this);
    		
    		// if the game is just established by the new accept message, send
    		// users the message that they can start playing
    		
    		User user1 = UserRepository.getInstance().findByFacebookID(this.firstUserFbID);
        	User user2 = UserRepository.getInstance().findByFacebookID(this.secondUserFbID);
    		
        	Map<String,String> abortMessage = PushMessages.createEstablishedGameMessage(this);
        	
        	user1.sendMessage(abortMessage);
        	user2.sendMessage(abortMessage);
    	}
    }
    
    private static Random random = new Random();
    private static boolean flipCoin(double probability) {
    	return random.nextDouble() < probability;
    }
    
    public boolean isAborted(){
    	return this.aborted;
    }
    
    public boolean isEstablished(){
    	return firstUserAccepted && secondUserAccepted;
    }
    
    /**
     * Aborts the game.
     * Sends an abort-message to both users
     * 
     * @param abortingUserFacebookID
     * @throws IOException
     */
    
    public void abort(String abortingUserFacebookID) throws IOException {
    	this.aborted = true;
    	this.state = StateAborted;
    	this.winnerName ="aborted";
		GameRepository.getInstance().update(this);
    	   	
    	User user1 = UserRepository.getInstance().findByFacebookID(this.firstUserFbID);
    	User user2 = UserRepository.getInstance().findByFacebookID(this.secondUserFbID);
    	
    	User abortingUser = null;
    	
		// subtract a point for giving up
    	if(user1.facebookID.equals(abortingUserFacebookID)){
			UserRepository.getInstance().addToScoreAndUpdate(user1,-1);
    		abortingUser = user1;
    		
    	} else {
			UserRepository.getInstance().addToScoreAndUpdate(user2,-1);
    		abortingUser = user2;
    	}

		Map<String,String> abortMessage = PushMessages.createAbortGameMessage(this, abortingUser);
    	
    	user1.sendMessage(abortMessage);
    	user2.sendMessage(abortMessage);
    }
    
    /**
     * If a social interaction happened, this method evaluates it.
     * If both users interacted within a narrow time-frame of 60 seconds, then
     * the game is "finished" and the winner is randomly selected.
     * 
     * @param facebookID
     * @throws IOException
     */
    
    public void socialInteraction(String facebookID) throws IOException{
    	Long time = System.currentTimeMillis();
    	
    	if(this.state.equals(StateProgress)){
    		
    		Logger.info("offset " + java.lang.Math.abs(this.firstUserInteractionTimeStamp - this.secondUserInteractionTimeStamp)); 
    		
    		if(this.firstUserFbID.equals(facebookID) && this.firstUserInteractionTimeStamp < 0L) {
    			this.firstUserInteractionTimeStamp = time;
				GameRepository.getInstance().update(this);
    		}
    		
    		if(this.secondUserFbID.equals(facebookID) && this.secondUserInteractionTimeStamp < 0L){
    			this.secondUserInteractionTimeStamp = time;
				GameRepository.getInstance().update(this);
    		}
    		
    		// if both timestamps have been set, then the game is finished
    		if(this.firstUserInteractionTimeStamp > 0 && this.secondUserInteractionTimeStamp > 0) {
    			
    			User user1 = UserRepository.getInstance().findByFacebookID(this.firstUserFbID);
            	User user2 = UserRepository.getInstance().findByFacebookID(this.secondUserFbID);
    			
    			// see if social interaction took place in a similar time frame (one minute)
            	// else we have a draw
            	if(java.lang.Math.abs(this.firstUserInteractionTimeStamp - this.secondUserInteractionTimeStamp) < 60000) {
            	
	            	boolean user1Result = flipCoin(0.5); 
	    			boolean user2Result = flipCoin(0.5);
	    			
	    			if(user1Result && !user2Result){
	    				// user1 won
						UserRepository.getInstance().addToScoreAndUpdate(user1,5);
	    				this.winnerFbID = user1.facebookID;
	    				this.winnerName = user1.name;
	    				
	    		    	user1.sendMessage(PushMessages.createWonGameMessage(user1, user2));
	    		    	user2.sendMessage(PushMessages.createLostGameMessage(user2, user1));
	    				
	    			} else if(!user1Result && user2Result){
	    				// user2 won
						UserRepository.getInstance().addToScoreAndUpdate(user2,5);
	    				this.winnerFbID = user2.facebookID;
	    				this.winnerName = user2.name;
	    				
	    				user2.sendMessage(PushMessages.createWonGameMessage(user2, user1));
	    		    	user1.sendMessage(PushMessages.createLostGameMessage(user1, user2));
	    				
	    			} else {
	    				// draw
	    				user1.sendMessage(PushMessages.createDrawGameMessage(user2));
	    				user2.sendMessage(PushMessages.createDrawGameMessage(user1));
	    				
	    				this.winnerName = "draw";
	    			} 			
	    			
            	} else {
       				// draw
    				user1.sendMessage(PushMessages.createDrawGameMessage(user2));
    				user2.sendMessage(PushMessages.createDrawGameMessage(user1));
    				
    				this.winnerName = "draw";
            	}
            	this.state = StateFinished;

				GameRepository.getInstance().update(this);
    		}
    	}
    }


	
    
    /**
     * Administrative methods
     * 
     */

	
	public Game(User user1, User user2, UserRepository userRepo, GameRepository gameRepo){
		this.firstUserFbID = user1.facebookID;
		this.secondUserFbID= user2.facebookID;
		
		this.firstUserName = user1.name;
		this.secondUserName= user2.name;

	}

	/**
	 * creates a deep copy of this object
	 */
	public Game(Game g) {
		this.firstUserAccepted = g.firstUserAccepted;
		this.firstUserFbID = g.firstUserFbID;
		this.firstUserName = g.firstUserName;
		this.firstUserInteractionTimeStamp = g.firstUserInteractionTimeStamp;
		this.secondUserAccepted=g.secondUserAccepted;
		this.secondUserFbID= g.secondUserFbID;
		this.secondUserName=g.secondUserName;
		this.secondUserInteractionTimeStamp = g.secondUserInteractionTimeStamp;
		this.aborted = g.aborted;
		this.winnerFbID = g.winnerFbID;
		this.winnerName = g.winnerName;
		this.date = g.date;
		this.state = g.state;
	}

	public String getState() {return state;}

	// "Why this empty constructor?"
	// Jongo won't be able to create a Java object otherwise
	// and will happily throw errors in your face
	public Game(){};

}
