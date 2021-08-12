package models;


import java.util.HashMap;
import java.util.Map;

/**
* This class is used for convenience to generate the specific messages used to 
* communicate to the client application.
*
* The pattern is always the same:
* type - for identifying the message type (event)
* subtype - for indicating the nature of the event
* and additional payload
* 
* The messages are always encoded into a JSON that is sent to the client application
*
*
*/

public class PushMessages {

	public static Map<String,String> createGameRequestMessage(Game game){
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "request");
		message.put("gameID", game.id);
		message.put("user1ID", game.firstUserFbID);
		message.put("user2ID", game.secondUserFbID);
		message.put("user1Name", game.firstUserName);
		message.put("user2Name", game.secondUserName);

		return message;
	}

	public static Map<String,String> createAbortGameMessage(Game game, User abortingUser) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "aborted");
		message.put("gameID", game.id);
		message.put("aborterID", abortingUser.facebookID);
		message.put("aborterName", abortingUser.name);

		return message;
	}

	public static Map<String,String> createEstablishedGameMessage(Game game) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "established");
		message.put("gameID", game.id);

		return message;
	}

	public static Map<String,String> createWonGameMessage(User winner, User loser) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "won");
		message.put("score", ""+winner.score);
		message.put("opponent", loser.name);

		return message;
	}


	public static Map<String,String> createLostGameMessage(User loser, User winner) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "lost");
		message.put("score", ""+loser.score);
		message.put("opponent", winner.name);

		return message;
	}

	public static Map<String,String> createDrawGameMessage(User opponent) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "draw");
		message.put("opponent", opponent.name);

		return message;
	}

	public static Map<String,String> createPokeMessage(User sender) {
		Map<String,String> message = new HashMap<>();

		message.put("type", "game");
		message.put("subtype", "poke");
		message.put("senderName", sender.name);
		message.put("senderID", sender.facebookID);

		return message;
	}

}
