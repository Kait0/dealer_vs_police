package models;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.jongo.MongoCollection;

import play.Logger;
import uk.co.panaxiom.playjongo.PlayJongo;
import util.GameConfiguration;
import util.Util;

import javax.inject.*;


/**
 * This Repository classes provide the interface between the
 * information stored in the MongoDB and the Java objects.
 * The loading and saving is done automagically, but might
 * break in weird ways. Stackoverflow usually helps there ;)
 */
@Singleton
public class GameRepository {

	// In order to be able to access the database, Jongo has to be injected,
	// this handles all the automatic storing and loading from and to java objects.
    @Inject
    private PlayJongo jongo;


	// to avoid having to inject the repositories into the
	// game object, which then results in very weird cloning functions
	// this is a static reference to the repository.
	// This only works, as the GameRepository is a singleton
	// and must always be created when the home controller is created,
	// which ensures that the GameRepository is never null when used
	// from the HomeController
	private static GameRepository instance = null;

	public GameRepository()
	{
		instance = this;
	}

	public static GameRepository getInstance(){return instance;}

    private Random r = new Random();

    /**
     * Looks-up all games that are finished or transient, i.e. used for 
     * the game statistics page.
     * @return
     */
    
    public Iterable<Game> findAllGames() {    	
    	Iterable<Game> iterator = games().find().as(Game.class);

    	return iterator;
    }
    
    /**
     * Looks up an opponent for the user to play the game against.
     * 
     * @param user
     * @return
     */
    
    public User findOpponent(User user){
    	// we are looking up participating friends here, so you can start the application
    	// out of the box; preferable for such a game would be friend of friends etc
    	// 
    	// this method is costly but primitive
    	
    	User ret = null;
    	
    	//Get all friends that 1) participate and are 2) nearby and 3) logged in less that 60 mins ago  	
    	LinkedList<User> friends = new LinkedList<User>();
    	
    	for(String friendID: user.facebookFriendIDs) {
    		User friend = UserRepository.getInstance().findByFacebookID(friendID);
    		if(friend != null && friend.participatesInGame) {
    			
    			if(Util.geoLocToDistInMeters(user.loc[0], user.loc[1], friend.loc[0], friend.loc[1]) <= GameConfiguration.MaxDistanceOfUserForNearbyUsers) {
    		
    				long currentTimeInSeconds = System.currentTimeMillis() / 1000;
    				
    				if(currentTimeInSeconds - friend.lastLogin <= GameConfiguration.MaxTimeForLoginTimeOutInSeconds){

    					friends.add(friend);
    				} 				
    			}    			
    		}
    	}
    	 	
    	// choose one of the friends randomly, unless only one friend exists
    	if(friends.size() == 1) {
    		
    		ret = friends.get(0);
    		
    	} else if(friends.size() >= 1) {
    		
    		ret = friends.get( r.nextInt(friends.size()) );
    		
    	}
    	
    	Logger.info("opponent: "+ret+ "\nme "+user);
    	
    	
    	return ret;
    }

    public Game createAndStartNewGame(User user1, User user2) throws IOException {
    	//still to be done: lookup and test whether a game between both users already exists
    	 	
    	Game newGame = new Game(user1, user2, UserRepository.getInstance(), this);

    	games().insert(newGame);

        Logger.info("GameID: " + newGame.id);
    	Map<String,String> requestMessage = PushMessages.createGameRequestMessage(newGame);
    	
    	user1.sendMessage(requestMessage);
    	user2.sendMessage(requestMessage);
    	
    	return newGame;
    }
    
    public Game findByID(String id) { 	
    	return games().findOne("{_id: #}", id).as(Game.class);
    }
    
    public MongoCollection games() {
        MongoCollection gameCollection =  jongo.getCollection("games");
 
        return gameCollection;
    }


	/**
	 * Updates the whole object in the database using a deep copy with unassigned database/jongo ID.
	 */
	public void update(Game game) {
		games().update("{_id: #}",game.id).with( new Game(game));
	}

}
