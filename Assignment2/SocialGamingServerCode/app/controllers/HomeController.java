package controllers;
//Test

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.*;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import util.GameConfiguration;
import views.html.index;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */
public class HomeController extends Controller {

    // If the HomeController wants to be able to access Users and/or
    // Games from the database, it has to "Inject" the Repository Objects
    // to access them
    // More information here:
    // https://www.playframework.com/documentation/2.5.x/ScalaDependencyInjection
    //
    // TL;DR: When a new HomeController Object is created by Play it
    // automatically creates the UserRepository and GameRepository and
    // saves it into those attributes.
    @Inject
    private UserRepository users;

    @Inject
    private GameRepository games;

    @Inject
    private FenceRepository fences;

    public List<CatchThread> catches = new ArrayList<>();

    /**
     * Index route: generates a HTML site with the game statistics. This will be
     * also shown in a widget in the Android application.
     *
     * @return
     */
    public Result index() {

        // for displaying at first get all the games
        // as they have to be in array form, consume the iterator
        // to create an array
        List<Game> gamesList = new ArrayList<>();
        //Auskommentiert, da es in einer Endlosschleife resultiert. Scheint mir noch von der Beispielimplementierung zu kommen, falls wir es doch brauchen wieder entkommentieren und fixen.
        /*for (Game g : games.findAllGames()) {
            gamesList.add(g);
        }
         */
        // finally render...

        String ret = "";
        Iterable<User> user = users.findAllUsers(20);

        if (user != null) {
            for (User u : user) {
                ret += "\n" + u.toString();
            }
        }
        ret += "\n\tThreads:\n";
        for (CatchThread c : catches) {
            ret += "\n" + c.toString();
        }

        return ok(index.render("Social Computing\n" + ret, gamesList));
    }

    /**
     * On login, this will either update or insert a new user in the database
     *
     * @param facebookAuthToken - Facebook auth token to request the user
     * information from facebook on behalf of user
     * @param googleCDSToken - Google Cloud Messaging device token used to
     * communicate via push service to the device
     * @param longitude - user position
     * @param latitude - user position
     * @return
     */
    public Result loginUser(String facebookAuthToken, String googleCDSToken, Double longitude, Double latitude) {
        Result ret;

        Logger.info("looking up user...");

        User user = users.updateUserProfileFromLoginCredentials(facebookAuthToken, googleCDSToken, longitude, latitude);

        if (user != null) {

            Map<String, String> message = new HashMap<String, String>();
            message.put("type", "server");
            message.put("subtype", "login");

            try {
                user.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }

            ret = ok("logged in user" + user.name);
        } else {
            ret = badRequest("Error logging user in!");
        }

        Logger.info("User is:\n " + user.toString());

        return ret;
    }

    public Result joinUser(String facebookID) {

        User user = UserRepository.getInstance().findByFacebookID(facebookID);
        if (user == null) {
            return badRequest("Could not find User with Facebook ID " + facebookID);
        }
        Game.joinUserToTeam(user);

        //Result
        ObjectNode searchResult = Json.newObject();
        //TODO add information to the result
        //searchResult.put("user", user);
        return ok(searchResult);

    }
	
	public Result getTeam(String facebookID) {
		
		User user = UserRepository.getInstance().findByFacebookID(facebookID);
        if (user == null) {
            return badRequest("Could not find User with Facebook ID " + facebookID);
        }
		
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
		
		return ok();
	}

    public Result getUserData(String fID) {
        User user = UserRepository.getInstance().findByFacebookID(fID);
        if (user == null) {
            return badRequest("Could not find User with Facebook ID " + fID);
        }
        //Result
        //TODO testen ob arrays richtig übergeben werden
        ObjectNode searchResult = Json.newObject();
        searchResult.put("name", user.name);
        searchResult.put("role", user.role);
        searchResult.put("Inventory1", user.getInv()[0]);
        searchResult.put("Inventory2", user.getInv()[1]);
        searchResult.put("Inventory3", user.getInv()[2]);
        searchResult.put("latitude", user.loc[0]);
        searchResult.put("longitude", user.loc[1]);

        return ok(searchResult);
    }

    /**
     * Returns nearby users of a user, given his/her Faceook ID. The maximum
     * distance for this search is set in GameConfiguration.
     *
     * @param facebookID
     * @return
     */
    public Result getNearbyUsers(String facebookID) {
        // sanity check: only allow a participant user to do a request
        // limit number of returned results

        Result ret;

        Logger.info("Requesting users...");

        User user = users.findByFacebookID(facebookID);

        if (user != null && user.participatesInGame) {
            Iterable<User> usersNearby = users.findUsersNearbyByRole(user.loc, GameConfiguration.MaxDistanceOfUserForNearbyUsers, GameConfiguration.MaxNumberOfReturnedUsers, user.role);

            if (usersNearby != null) {
                ObjectNode searchResult = Json.newObject();
                ArrayNode userArray = searchResult.arrayNode();

                for (User u : usersNearby) {
                    if (!u.facebookID.equals(user.facebookID)) {
                        ObjectNode userNode = Json.newObject();

                        userNode.put("user", u.name);
                        userNode.put("facebookID", u.facebookID);
                        userNode.put("longitude", u.loc[0]);
                        userNode.put("latitude", u.loc[1]);

                        userArray.add(userNode);
                    }
                }

                // just add an array, if we actually have found users, otherwise just send back an empty json
                if (userArray.size() > 0) {
                    searchResult.put("users", userArray);
                }

                ret = ok(searchResult);
            } else {

                ret = badRequest("Could not find other users");
            }

        } else {
            ret = badRequest("User not recognized");
        }

        return ret;
    }

    public Result getDistanceToUser(String PfacebookID, String DfacebookID) {
        Result ret;

        Logger.info("Requesting nearest user...");

        User police = users.findByFacebookID(PfacebookID);
        User dealer = users.findByFacebookID(DfacebookID);
        if (police != null && dealer != null) {
            ObjectNode searchResult = Json.newObject();
            Double dist =/*Util.*/ geoLocToDistInMeters(police.loc[0], police.loc[1], dealer.loc[0], dealer.loc[1]);
            searchResult.put("distance", dist);
            ret = ok(searchResult);
        } else {
            ret = badRequest("Could not find distance");
        }
        return ret;
    }
    //sorry didnt know how to use the class Util here so this is a temporary method copied here

    public static Double geoLocToDistInMeters(Double loc, Double loc2, Double loc3, Double loc4) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(loc4 - loc2);
        double dLng = Math.toRadians(loc3 - loc);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(loc2)) * Math.cos(Math.toRadians(loc4))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        int meterConversion = 1609;

        return Math.abs((dist * meterConversion));
    }

    //Returns nearby Fences,Dealer and Police
    public Result getNearbyFences(String facebookID) {
        double nearbyD = 50000.0;
        double nearbyP = 50000.0;
        Result ret;
        User user = null;
        user = users.findByFacebookID(facebookID);
        if (user == null) {
            return badRequest("User not existant");
        }
        if (!user.isAdmin) {
            if (user.role == 0) {
                nearbyP = 300.0;
            } else {
                nearbyD = 300.0;
            }
        }
        Iterable<Fence> fencesNearby = fences.findFencesNearby(user.loc, 500000.0, 10);
        Iterable<User> dealerNearby = users.findUsersNearbyByRole(user.loc, nearbyD, 30, 0);
        Iterable<User> policeNearby = users.findUsersNearbyByRole(user.loc, nearbyP, 30, 1);
        //TODO also police and dealers
        ObjectNode searchResult = Json.newObject();
        ArrayNode fenceArray = searchResult.arrayNode();
        ArrayNode dealerArray = searchResult.arrayNode();
        ArrayNode policeArray = searchResult.arrayNode();
        if (fencesNearby != null && (user.role == 0 || user.isAdmin)) {
            for (Fence f : fencesNearby) {
                ObjectNode fenceNode = Json.newObject();
                fenceNode.put("fenceID", f.ID);
                fenceNode.put("longitude", f.loc[0]);
                fenceNode.put("latitude", f.loc[1]);
                fenceArray.add(fenceNode);
            }
            if (fenceArray.size() > 0) {
                searchResult.put("fences", fenceArray);
            } else {
                searchResult.put("fences", searchResult.arrayNode());
            }
        } else {
            searchResult.put("fences", searchResult.arrayNode());
        }
        if (dealerNearby != null) {
            for (User d : dealerNearby) {
                ObjectNode dealerNode = Json.newObject();
                dealerNode.put("Name", d.name);
                dealerNode.put("longitude", d.loc[0]);
                dealerNode.put("latitude", d.loc[1]);
                dealerNode.put("facebookID", d.facebookID);
                dealerArray.add(dealerNode);
            }
            if (dealerArray.size() > 0) {
                searchResult.put("dealer", dealerArray);
            } else {
                searchResult.put("dealer", searchResult.arrayNode());
            }
        } else {
            searchResult.put("dealer", searchResult.arrayNode());
        }

        if (policeNearby != null) {
            for (User p : policeNearby) {
                ObjectNode policeNode = Json.newObject();
                policeNode.put("Name", p.name);
                policeNode.put("longitude", p.loc[0]);
                policeNode.put("latitude", p.loc[1]);
                policeNode.put("facebookID", p.facebookID);
                policeArray.add(policeNode);
            }
            if (policeArray.size() > 0) {
                searchResult.put("police", policeArray);
            } else {
                searchResult.put("police", searchResult.arrayNode());
            }
        } else {
            searchResult.put("police", searchResult.arrayNode());
        }

        return ok(searchResult);
    }

    /**
     * Updates the database entry of that user with the new location.
     *
     * @param facebookID
     * @param longitude
     * @param latitude
     * @return
     */
    public Result updateUserLocation(String facebookID, Double longitude, Double latitude) {
        Result ret;

        User user = users.findByFacebookID(facebookID);
        if (user != null) {
            users.updateLocation(user, longitude, latitude);
            Logger.info("Updated user position: " + facebookID + " > " + longitude + " , " + latitude);
            if(users.TestSpawning){
            users.generateTestStuff(facebookID);
            }

            ret = ok();
        } else {
            ret = badRequest("User does not exist!");
        }

        return ret;
    }

    /**
     * This is called once a user requests a new game and, after game object
     * creation, messages both users to accept or abort a game.
     *
     * Game objects also reside in the database, we are using the mongo-db
     * object ID as simple ID for a game directly = gameID.
     *
     * The message sending logic is part of the game object.
     *
     * @param facebookID
     * @return
     */
    public Result requestNewGame(String facebookID) {
        Result ret;

        Logger.info("requesting new game for facebook id " + facebookID);

        User user = users.findByFacebookID(facebookID);

        if (user != null) {
            Logger.info("User requested new game: " + facebookID);

            User opponent = games.findOpponent(user);

            if (opponent != null) {
                // this will not only add a new Game object to the database but also communicate
                // the request via Google Cloud Messaging to the opponents

                try {
                    Game newGame = games.createAndStartNewGame(user, opponent);
                    ret = ok();

                } catch (IOException e) {
                    ret = badRequest("Some error occured");
                    e.printStackTrace();
                }

            } else {
                ret = badRequest("No opponent found!");
            }

        } else {
            ret = badRequest("User does not exist!");
        }

        return ret;
    }

    /**
     * This is called once a user decides NOT to play upon request.
     *
     * This action sends messages.
     *
     * @param gameID
     * @param facebookID
     * @return
     */
    public Result abortGame(String gameID, String facebookID) {
        Result ret;

        Game game = games.findByID(gameID);

        if (game != null) {

            if (!game.isAborted()) {
                try {
                    game.abort(facebookID);
                    ret = ok();

                } catch (IOException e) {
                    ret = badRequest("Some error occured");
                    e.printStackTrace();
                }
            }

            ret = ok();
        } else {
            ret = badRequest("Game does not exist!");
        }

        return ret;
    }

    /**
     * Called to accept a game, the game object changes state to "established".
     *
     * This action sends messages.
     *
     * @param gameID
     * @param facebookID
     * @return
     */
    public Result acceptGame(String gameID, String facebookID) {
        Result ret;

        Game game = games.findByID(gameID);

        if (game != null) {

            if (!game.isAborted()) {
                try {
                    game.accept(facebookID);
                    ret = ok();

                } catch (IOException e) {
                    ret = badRequest("Some error occured");
                    e.printStackTrace();
                }
            }

            ret = ok();
        } else {
            ret = badRequest("Game does not exist!");
        }

        return ret;
    }

    /**
     * Called when a user did the short time social interaction. If both users
     * did this for the same game (-ID), then the winner will be determined and
     * the game is finished.
     *
     * This action sends messages.
     *
     * @param gameID
     * @param facebookID
     * @return
     */
    public Result interactionInGame(String gameID, String facebookID) {
        Result ret;

        Game game = games.findByID(gameID);

        if (game != null) {

            if (!game.isAborted()) {
                try {
                    game.socialInteraction(facebookID);
                    ret = ok();

                } catch (IOException e) {
                    ret = badRequest("Some error occured");
                    e.printStackTrace();
                }
            }

            ret = ok();
        } else {
            ret = badRequest("Game does not exist!");
        }

        return ret;
    }

    public Result requestDealData(String facebookID) {
        Result ret;
        User user = users.findByFacebookID(facebookID);

        if (user != null) {
            int money = user.getInv()[0];
            int amount = user.getInv()[2];
            int price = 100; //TODO Preis abfragen
            Logger.info("Money: " + money);
            Logger.info("Amount: " + amount);

            Map<String, String> message = new HashMap<String, String>();
            message.put("type", "game");
            message.put("subtype", "dealData");
            message.put("money", "" + money);
            message.put("amount", "" + amount);
            message.put("price", "" + price);

            try {
                user.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }

            ret = ok("Data sent");
        } else {
            ret = badRequest("Error sending data!");
        }

        return ret;
    }

    //Verarbeitung eines Kaufrequests
    public Result buyDrugs(String facebookID, int amount) {
        Result ret;
        int cost = 100;
        User user = users.findByFacebookID(facebookID);
        if (user.role == 0) {
            //Es reicht ein Dealer in der Naehe
            Fence nearest = fences.findByID(users.getNearestFence(facebookID));
            if (nearest == null) {
                ret = badRequest("No fences nearby");
                Game.sendPopup(user, "Could not buy. No fences nearby.");
                return ret;
            }
            //Look up the money the dealer has
            int money = user.getInv()[0];
            if (money >= amount * cost) {
                //Update user in DB, send him confirmation message
                money -= amount * cost;
                int[] userInventory = user.getInv();
                userInventory[0] = money;
                userInventory[2] += amount;
                //userInventory[2] = nicht sicher, ob/was hier veraendert werden soll
                user.setInv(userInventory);
                user.setFence(nearest.ID);
                //Hier sollte noch der Hehler der grade am naehesten ist im User gespeichert werden
                Logger.info("Purchase sucessful");
                Game.sendPopup(user, "Purchase sucessful!");
                ret = ok();
            } else {
                Game.sendPopup(user, "Could not buy. Not enough money.");
                ret = badRequest("Not enough money");
            }
        } else {
            Game.sendPopup(user, "Could not buy. You are no dealer.");
            ret = badRequest("Not a dealer");
        }
        return ret;
    }

    //Gibt dem Client die Informationen wie das verkaufen vonstatten gehen wuerde
    public Result sellInformation(String facebookID, int amount) {
        Result ret = null;
        User user = users.findByFacebookID(facebookID);
        Fence f = fences.findByID(users.getNearestFence(facebookID));
        Fence lastFence = fences.findByID(user.lastFence);
        if (user == null) {
            return badRequest("User not found.");
        }
        if (f == null) {
            return badRequest("No Fence nearby.");
        }
        if (lastFence == null) {
            return badRequest("No fences visited previously.");
        }
        if (user.getInv()[2] >= amount) {
            int earnings = ((int) (geoLocToDistInMeters(lastFence.loc[0], lastFence.loc[1], f.loc[0], f.loc[1]) * amount) + amount * 100) - 30;

            Map<String, String> message = new HashMap<String, String>();
            message.put("type", "game");
            message.put("subtype", "sellData");
            message.put("amount", "" + amount);
            message.put("earnings", "" + earnings);
            message.put("fenceID", "" + f.ID);

            try {
                user.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }

            ret = ok();
        } else {
            Game.sendPopup(user, "Not enough stuff in inventory.");
            ret = badRequest("Not so many Drugs!");
        }
        return ret;
    }

    //Verkaufen von Drogen an Hehler
    public Result sellDrugs(String facebookID, int amount) {
        //= null hinzugefuegt um not initialized error zu verhindern. Wahrscheinlich fehlt ein else Statement wie in der Funktion drüber
        Result ret = null;
        User user = users.findByFacebookID(facebookID);
        Fence buyer = fences.findByID(users.getNearestFence(facebookID));
        if (user == null) {
            return badRequest("User not found.");
        }
        if (user.getInv()[2] >= amount) {
            //Distanzberechnung zwischen Hehlern fuer den Preis:
            Fence last = null;
            if (user.lastFence > -1) {
                last = fences.findByID(user.lastFence);
            } else {
                ret = badRequest("Never bought anything");
            }
            if (buyer == null) {
                return badRequest("No Fence nearby.");
            }
            int earnings = ((int) (geoLocToDistInMeters(buyer.loc[0], buyer.loc[1], last.loc[0], last.loc[1]) * amount)) - 30;

            int[] userInventory = new int[3];
            userInventory[0] = user.getInv()[0] + earnings + amount * 100;
            userInventory[2] = user.getInv()[2] - amount;
            if (userInventory[2] == 0) {
                userInventory[1] = 0;
            } else {
                userInventory[1] = user.getInv()[1];
            }
            user.setInv(userInventory);
            ret = ok();
        } else {
            Game.sendPopup(user, "Not enough stuff in inventory.");
            ret = badRequest("Not so many Drugs!");
        }
        return ret;
    }

    //Starten des Fangprozesses, nimmt dabei naehesten Dealer
    public Result Catch(String PFacebookID) {
        String diD = users.getNearestDealer(PFacebookID);
        User police = users.findByFacebookID(PFacebookID);
        if (diD.equals("-1")) {
            System.out.println("No Dealer near");
            Game.sendPopup(police, "No Dealer near");
            return badRequest("No Dealer Near");
        }
        if (police.inCatch) {
            Game.sendPopup(police, "Youre already catching lol");
            return badRequest("Youre already catching lol");
        }
        User dealer = users.findByFacebookID(diD);
        Result ret = null;

        CatchThread catcher = new CatchThread(police, dealer);
        catches.add(catcher);
        catches.get(catches.size() - 1).start();
        //catcher.start();
        //sollte der Code hier schon enden, und der client von selbst weiter den status abfragen? 
        //Da er wahrscheinlich auf das result wartet bis er was tut, und das dauert halt das ganze fangen lang sonst
        ret = ok();
        return ret;
    }

    /**
     * This is used as an axaple to send messages from client to client using
     * the server.
     *
     * This action sends messages.
     *
     * @param senderFacebookID
     * @param recipentFacebookID
     * @return
     */
    public Result poke(String senderFacebookID, String recipentFacebookID) {
        Result ret;

        User sender = users.findByFacebookID(senderFacebookID);
        User recipent = users.findByFacebookID(recipentFacebookID);

        if (sender != null && recipent != null && recipent.participatesInGame) {
            // we can do some other logic here, for now, we just send a
            // poke message without further testing (test if nearby, is friend, etc)
            // the message will be kept alive for one hour in the GCM System
            // and therefore delivered as soon as the recipent is online (within this hour)

            Logger.info("poke > " + sender.name + " -> " + recipent.name);

            try {
                recipent.sendMessageCached(PushMessages.createPokeMessage(sender), 3600);

                ret = ok();

            } catch (IOException e) {
                e.printStackTrace();

                ret = badRequest();

            }

        } else {
            ret = badRequest("User does not exist!");
        }

        return ret;
    }

}
