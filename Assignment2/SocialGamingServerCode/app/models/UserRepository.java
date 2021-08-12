package models;

import java.util.List;

import org.jongo.MongoCollection;

import javax.inject.*;

import uk.co.panaxiom.playjongo.*;

import com.google.common.collect.Lists;

import controllers.FacebookAppClient;
//4 testing
import play.Logger;

/**
 * This Repository classes provide the interface between the information stored
 * in the MongoDB and the Java objects. The loading and saving is done
 * automagically, but might break in weird ways. Stackoverflow usually helps
 * there ;)
 */
@Singleton
public class UserRepository {

    // In order to be able to access the database, Jongo has to be injected,
    // this handles all the automatic storing and loading from and to java objects.
    @Inject
    private PlayJongo jongo;

    public Boolean TestSpawning = false;

    // to avoid having to inject the repositories into the
    // game object, which then results in very weird cloning functions
    // this is a static reference to the repository.
    // This only works, as the UserRepository is a singleton
    // and must always be created when the home controller is created,
    // which ensures that the UserRepository is never null when used
    // from the HomeController
    private static UserRepository instance = null;

    public UserRepository() {
        instance = this;
    }

    public static UserRepository getInstance() {
        return instance;
    }

    /**
     * Looks up the user collection from the database
     *
     * @return
     */
    public MongoCollection users() {
        MongoCollection userCollection = jongo.getCollection("users");

        // make sure we use 2d indices on a sphere to use geospatial queries
        userCollection.ensureIndex("{loc: '2dsphere'}");
        return userCollection;
    }

    /**
     * User lookup by user/character name
     *
     * @param name
     * @return
     */
    public User findByName(String name) {
        return users().findOne("{name: #}", name).as(User.class);
    }

    /**
     * User lookup by Facebook ID
     *
     * @param fbID
     * @return
     */
    public User findByFacebookID(String fbID) {
        return users().findOne("{facebookID: #}", fbID).as(User.class);
    }

    /**
     * Adds a new user to the database
     *
     * @param name - User/character name
     * @param fbID - Facebook ID
     * @return
     */
    private User insertNewUserWithSimpleProfileData(String name, String fbID) {
        User newUser = new User(name, fbID);
        insert(newUser);
        return newUser;
    }

    private void insertTestUser(Double[] loc) {
        User newUser = new User("Test User 123", "TestUserFBID", loc, 500, 4, 1, 0);
        insert(newUser);
        System.out.println("New User: " + newUser.toString());
    }

    private void insert(User u) {
        users().save(u);
    }

    /**
     * Updates all fields in the DB for the object by making a copy and swapping
     * the original object in the database for it
     */
    public void update(User user) {
        // copy the user to be sure that database IDs will be taken care of
        users().update("{facebookID: #}", user.facebookID).with(copyUser(user));
    }

    /**
     * Alters the score of a user
     *
     * @param add
     */
    public void addToScoreAndUpdate(User user, double add) {
        double newScore = user.score + add;
        users().update("{facebookID: #}", user.facebookID).with("{$set: {score: #}}", (Object) new Double(newScore));
    }

    /**
     * Alters the 2d spherical position of a user
     *
     * @param longitude
     * @param latitude
     */
    public void updateLocation(User user, Double longitude, Double latitude) {
        users().update("{facebookID: #}", user.facebookID).with("{$set: {loc: #}}", (Object) new Double[]{longitude, latitude});
    }

    /**
     * This class method finds users that are nearby a location, it only returns
     * users that are also participating in the game, you can uncomment the
     * additional mongoDB parameters if you want
     *
     * @param loc - geolocations as 2D double vector: {longitude,lattitude}
     * @param maxDistance - maximum distance in meters where users should be
     * looked up
     * @param limit - a limit for the number of returned users
     * @return
     */
    public Iterable<User> findUsersNearby(Double[] loc, Double maxDistance, int limit) {
        // geoNear indicates a lookup based on spherical coordinates

        // this DB request also serves as an example for a slightly more complex mongoDB request making use
        // of the "native" mongoDB API
        List<User> results = Lists.newArrayList((Iterable) users().find("{loc: {$geoNear : {$geometry : {type: 'Point', "
                + "coordinates: # }, $maxDistance: # }}, participatesInGame: true}", loc, maxDistance).limit(limit) // 
                .as(User.class));

        return results;
    }

    public Iterable<User> findAllUsers(int limit) {
        List<User> results = Lists.newArrayList((Iterable) users().find().limit(limit).as(User.class));
        return results;
    }

    //Diese Ausgabe wird dafur genutzt, die Spieler auf der Karte anzuzeigen
    //TODO: Nur die Liste der Spieler der gleichen Seite ausgeben Anmerkung: nicht Liste sondern iterable
    //still TODO : Rolle muss einfluss haben
    public Iterable<User> findUsersNearbyByRole(Double[] loc, Double maxDistance, int limit, int role) {
        // geoNear indicates a lookup based on spherical coordinates

        // this DB request also serves as an example for a slightly more complex mongoDB request making use
        // of the "native" mongoDB API
        //Admins können sich alle anzeigen lassen:
        List<User> results;
        if (role != 2) {
            results = Lists.newArrayList((Iterable) users().find("{loc: {$geoNear : {$geometry : {type: 'Point', "
                    + "coordinates: # }, $maxDistance: # }}, role: #}", loc, maxDistance, role).limit(limit) // 
                    .as(User.class));
        } else {
            return findUsersNearby(loc, maxDistance, limit);
        }
        return results;
    }

    /**
     * Updates or inserts a new User This method has the overhead of requesting
     * most of the user's data and updating them in the database, it could be
     * performed in a more compact/less bandwidth demanding way, but for the
     * sake of being able to expand this method with additional
     * processing/information all data is being requested from facebook
     *
     * @param facebookAuthToken
     * @param googleCDSToken
     * @param longitude
     * @param latitude
     * @return
     */
    public User updateUserProfileFromLoginCredentials(String facebookAuthToken, String googleCDSToken, Double longitude, Double latitude) {
        User user = null;

        //Fuege beim einloggen von einem Nutzer Standard Hehler ein:
        Fence[] f = new Fence[5];
        f[0] = new Fence(0, 11.667068, 48.264326);
        f[1] = new Fence(1, 11.669462, 48.263338);
        f[2] = new Fence(2, 11.667143, 48.262709);
        f[3] = new Fence(3, 11.668559, 48.262416);
        f[4] = new Fence(4, 11.670694, 48.265576);
        for (int i = 0; i < 5; i++) {
            Fence fe = null;
            fe = FenceRepository.getInstance().findByID(i);
            if (fe == null) {
                FenceRepository.getInstance().addFence(f[i]);
            }
        }

        com.restfb.types.User facebookUserProfile = FacebookAppClient.getUser(facebookAuthToken);

        if (facebookUserProfile != null) {
            user = findByFacebookID(facebookUserProfile.getId());

            // if the user does not exist, create one
            if (user == null) {
                user = insertNewUserWithSimpleProfileData(facebookUserProfile.getName(), facebookUserProfile.getId());
            }

            // as the user logged in, we can set her/his profile as active    			
            user.participatesInGame = true;

            user.googleCloudDeviceId = googleCDSToken;
            user.loc = new Double[]{longitude, latitude};
            user.lastLogin = System.currentTimeMillis() / 1000;

            // Update the user profile's friends entries
            List<com.restfb.types.User> usersFacebookFriends = FacebookAppClient.getFriendsOfUser(facebookAuthToken);
            String[] fbFriendIDs = new String[usersFacebookFriends.size()];

            int idx = 0;
            for (com.restfb.types.User facebookFriend : usersFacebookFriends) {

                // check if the friend is already in the database, else add her/him
                if (findByFacebookID(facebookFriend.getId()) == null) {
                    insertNewUserWithSimpleProfileData(facebookFriend.getName(), facebookFriend.getId());
                }
                fbFriendIDs[idx] = facebookFriend.getId();

                idx++;
            }

            user.facebookFriendIDs = fbFriendIDs;

            update(copyUser(user));
            generateTestStuff(user.facebookID);
        }

        return user;
    }

    public void generateTestStuff(String fID) {
        if (TestSpawning) {
            User user = findByFacebookID(fID);
            Double newloc[] = user.loc;
            Double nloc[] = user.loc;
            newloc[0] += 0.0001;
            newloc[1] -= 0.0001;
            nloc[0] -= 0.0001;
            nloc[1] -= 0.0001;
            System.out.println("Trying to create Test User...");
            User u = null;
            u = findByFacebookID("TestUserFBID");
            if (u == null) {
                System.out.println("Test User is being created newly.");
                insertTestUser(newloc);
            } else {
                System.out.println("Test User exists: " + u.toString());
                users().remove("{facebookID: #}", "TestUserFBID");
                System.out.println("Test User is being created newly.");
                insertTestUser(newloc);
            }
            Fence g = FenceRepository.getInstance().findByID(20);
            if (g == null) {
                System.out.println("Test Fence is being created newly.");
                g = new Fence(20, nloc[0], nloc[1]);
                FenceRepository.getInstance().addFence(g);
            } else {
                System.out.println("Test Fence is being updated.");
                g.loc[0] = nloc[0];
                g.loc[1] = nloc[1];
                FenceRepository.getInstance().update(g);
            }
        } else {
            User u = null;
            u = findByFacebookID("TestUserFBID");
            if (u != null) {
                users().remove("{facebookID: #}", "TestUserFBID");
            }
            Fence g = FenceRepository.getInstance().findByID(20);
            if (g != null) {
                FenceRepository.getInstance().fences().remove("{ID: #}", 20);
            }
        }
    }

    public String getNearestDealer(String facebookID) {
        //hardcoded distance fences can be at
        Double maxDist = 30.0;
        String ret;
        User user = findByFacebookID(facebookID);
        Iterable<User> dealersNearby = findUsersNearbyByRole(user.loc, maxDist, 10, 0);
        if (dealersNearby != null) {
            User de = new User();
            de.loc = new Double[]{0.0, 0.0};
            for (User d : dealersNearby) {
                if (/*Util.*/geoLocToDistInMeters(d.loc[0], d.loc[1], user.loc[0], user.loc[1]) </*Util.*/ geoLocToDistInMeters(de.loc[0], de.loc[1], user.loc[0], user.loc[1])) {
                    de = d;
                }
            }
            ret = de.facebookID;

        } else {
            ret = "-1";
        }
        return ret;
    }

    public int getNearestFence(String facebookID) {
        //hardcoded distance fences can be at
        Double maxDist = 30.0;
        int ret;
        User user = findByFacebookID(facebookID);
        Iterable<Fence> fencesNearby = FenceRepository.getInstance().findFencesNearby(user.loc, maxDist, 10);
        if (fencesNearby != null) {
            Fence fe = new Fence(999, 0.0, 0.0);
            for (Fence f : fencesNearby) {
                if (geoLocToDistInMeters(f.loc[0], f.loc[1], user.loc[0], user.loc[1]) < geoLocToDistInMeters(fe.loc[0], fe.loc[1], user.loc[0], user.loc[1])) {
                    fe = f;
                }
            }
            ret = fe.ID;

        } else {
            ret = -1;
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

    /**
     * Performs a deep copy of the user's data
     *
     * @return
     */
    private static User copyUser(User user) {
        User copy = new User();

        copy.name = user.name;
        copy.googleCloudDeviceId = user.googleCloudDeviceId;
        copy.facebookID = user.facebookID;
        copy.facebookFriendIDs = user.facebookFriendIDs.clone();
        copy.loc = user.loc;
        copy.participatesInGame = user.participatesInGame;
        copy.lastLogin = user.lastLogin;
        copy.score = user.score;
        copy.role = user.role;
        copy.lastFence = user.lastFence;
        copy.isAdmin = user.isAdmin;
        copy.inv = user.inv;
        copy.inCatch = user.inCatch;

        return copy;
    }
}
