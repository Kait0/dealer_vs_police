package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.PushNotifications;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is for the database representation of users using Jungo
 *
 * ID is intended to be a unique ID in the System, one can for example use the
 * Facebook User-IDs, as we do here. The device-token is the Google Cloud
 * Message Service token received from the Android GCM API, which is device and,
 * ergo, user-specific.
 *
 *
 *
 * @author Niklas Klügel
 *
 */
public class User {
    // used by Jongo to map JVM objects to database Objects

    @JsonProperty("_id")
    public String id;

    // credentials for the services
    public String googleCloudDeviceId = "";
    public String facebookID = "";

    // a list of facebook IDs of the user's friends,
    // to simplifiy (circumvent inconsistencies when a user logs in and has a different list of friends), 
    // we will have to look up the users according to Facebook-ID
    // from the database
    public String[] facebookFriendIDs = new String[]{};

    public String name;

    public Double[] loc = new Double[]{11.5833, 48.15}; // Garching

    // In seconds in System time
    public Long lastLogin = -1L;

    // Indicates whether this profile is active in the game
    public Boolean participatesInGame = false;

    // Game score that the user achieved
    public Double score = 0.0;

    /**
     * Integer welche Rolle der Spieler hat 0: Dealer , 1:Polizist
     */
    public int role;

    //Die ID des Hehlers mit dem man zuletzt gehandelt hat, fuer Punkteberechnung
    public int lastFence = -1;

    //Da Admin unabhaengig von rolle sein soll
    public Boolean isAdmin = false;

    /**
     * der server setzt nutzer hier auf true wenn sie zu einem Hehler kommen
     * oder in die Naehe eines Dealers, auf false wenn sie sich aus jeweiligem
     * Bereich wieder entfernen Hilft dem Client einfach abzufragen ob jeweilige
     * Buttons sichtbar sein sollen
     */
    public Boolean inCatch=false;

    /**
     * Inventar mit Reihe 0 fur Dealer, Reihe 1 fur Polizist spalte 0: Geld
     * spalte 1: Drogenpackchen Art spalte 2: Drogenpackchen Anzahl Bei
     * Polizist: spalte 0 fuer Justice Points spalte 1 fuer heute gefangene
     * Dealer spalte 2 fuer insgesamt gefangene Dealer
     */
    public int inv[][] = new int[2][3];

    /**
     * **************
     * Object methods ------------- *************
     */
    public User() {
    }

    public User(String name, String facebookID) {
        this.name = name;
        this.facebookID = facebookID;

        //standard Rolle ist Admin und Dealer, da ein Button zur Teamzuteilung eingefuehrt wurde
        role = 0;
        isAdmin = true;
    }

    public User(String name, String facebookID, int role) {
        this.name = name;
        this.facebookID = facebookID;
        this.role = role;
    }

    public User(String name, String facebookID, Double[] loc, int money, int packs, int lastFence,int role) {
        this.name = name;
        this.facebookID = facebookID;
        this.loc = loc;
        this.inv[0][0] = money;
        this.inv[0][2] = packs;
        this.lastFence = lastFence;
        this.role=role;
    }

    public void setRole(int role) {
        this.role = role;
        UserRepository.getInstance().update(this);
    }

    //Aendere Admin Status, gib den jetzigen Status aus
    public Boolean toggleAdmin() {
        this.isAdmin = !this.isAdmin;
        //damit die Anderung direkt an die DB geht
        UserRepository.getInstance().update(this);
        return this.isAdmin;
    }

    //toggle einmal wenn aktion moeglich,nochmal wenn nichtmehr moeglich
    public Boolean toggleCatch() {
        this.inCatch = !this.inCatch;
        //damit die Anderung direkt an die DB geht (ist das ueberhaupt noetig?)
        UserRepository.getInstance().update(this);
        return this.inCatch;
    }

    public void setFence(int ID) {
        this.lastFence = ID;
        //damit die Anderung direkt an die DB geht
        UserRepository.getInstance().update(this);
    }

    /**
     * @return the Inventory of active role of this user
     */
    public int[] getInv() {
        return this.inv[role];
    }

    /**
     * Set the inventory of a user for his active role
     *
     * @param inv inventory thats being used
     */
    public void setInv(int[] inv) {
        this.inv[this.role] = inv;
        //damit die Anderung direkt an die DB geht
        UserRepository.getInstance().update(this);
    }

    /**
     * Set the nventory of a user for a certain role
     *
     * @param inv the inventory thats being used
     * @param role the role thats supposed to have that inventory
     */
    public void setInvWRole(int[] inv, int role) {
        this.inv[role] = inv;
        //damit die Anderung direkt an die DB geht
        UserRepository.getInstance().update(this);
    }

    /**
     * A primitve function that checks whether a user is another user's friend
     * on Facebook
     *
     * @param other
     * @return
     */
    public boolean isFriendOf(User other) {
        boolean ret = false;

        for (String facebookFriendID : this.facebookFriendIDs) {
            if (facebookFriendID.equals(other.facebookID)) {
                ret = true;

                break;
            }
        }

        return ret;
    }

    /**
     * Send a message to the User's device using Google Cloud Messaging
     *
     * @param message - message to be sent
     * @return
     * @throws IOException
     */
    public void sendMessage(Map<String, String> message) throws IOException {
        this.sendMessageCached(message, 0);
    }

    /**
     * Send a message to the User's device using Google Cloud Messaging, this
     * time the message is cached in the GCM system and delivered as soon as the
     * device is online
     *
     * @param message - Message to be sent
     * @param ttl - Time to live of the message in GCM
     * @return
     * @throws IOException
     */
    public void sendMessageCached(Map<String, String> message, int ttl) throws IOException {
        PushNotifications.sendMessage(this.googleCloudDeviceId, message, ttl);
    }

    public void getJustice(User dealer, int amount) {
        if (this.role != 1) {
            return;
        }
        Fence f = null;
        f = FenceRepository.getInstance().findByID(dealer.lastFence);
        if (f != null) {
            int add=((geoLocToDistInMeters(f.loc[0], f.loc[1], dealer.loc[0], dealer.loc[1])).intValue()) * amount;
            inv[1][0] += add;
            UserRepository.getInstance().update(this);
            Game.sendPopup(this,"You received "+add+" Justice Points!");
        }
    }

    public int getCaught() {
        if (this.role != 0) {
            return 0;
        }
        int ret = inv[0][2];
        inv[0][2] = 0;
        if (inv[0][0] == 0) {
            inv[0][0] = 100;
        }
        UserRepository.getInstance().update(this);
        Game.sendPopup(this,"You lost all your Drugs!");
        return ret;
    }

    public String toString() {
        String friendsString = "";

        for (String friend : facebookFriendIDs) {
            friendsString = friendsString + ", " + friend;
        }

        return "\tName: " + this.name
                + "\n\tRole: " + this.role
                + "\n\tLast Fence: " + this.lastFence
                + "\n\tlocation: " + this.loc[0] + "," + this.loc[1] //+"\n\tFacebook ID: "+this.facebookID
                //+"\n\tfriends ("+facebookFriendIDs.length+"): "+friendsString
                //+"\n\tGoogle Device ID: "+this.googleCloudDeviceId
                ;
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
}
