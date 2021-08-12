package models;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CatchThread extends Thread {

    private User dealer, police;
    //Time it needs to be caught
    private  long catchTime;
    //Distance the Dealer has to have fled to flee
    private  double maxDistance;
    //radius in which the police catches the dealer
    private  double catchDistance;
    //distance dealer fled from police
    private double runDistance;
    //time the hunt started
    private long startTime;
    //time the police caught the dealer up until now
    private long timeWhileCatching;
    /**
     * Status to let Methods that are using the Thread know whats going on -1:
     * idle 0: active 1: caught 2: fled
     */
    private int status;

    public CatchThread(User police, User dealer) {

        this.catchTime = 30000;
        this.maxDistance = 10;
        this.catchDistance = 30.0;
        this.runDistance = 0;
        this.startTime = 0;
        this.timeWhileCatching = 0;
        this.status = -1;
        this.police = police;
        this.dealer = dealer;
    }

    @Override
    public void run() {
        System.out.println("started thread...");
        status = 0;
        startTime = System.currentTimeMillis();
        if (dealer == null || police == null) {
            status = -1;
            System.out.println("not possible to start yet");
            return;
        }
        //mark them as in a catch
        police.toggleCatch();
        dealer.toggleCatch();
        while (runDistance < maxDistance) {
            //updating their positions
            dealer = UserRepository.getInstance().findByFacebookID(dealer.facebookID);
            police = UserRepository.getInstance().findByFacebookID(police.facebookID);
            double dist = getDist();
            System.out.println("\nDistance:"+ dist);
            if (dist > catchDistance) {
                runDistance += dist - catchDistance;
                if (runDistance >= maxDistance) {
                    status = 2;
                    Map<String, String> message = new HashMap<String, String>();
                    message.put("type", "catch");
                    message.put("subtype", "done");
                    message.put("caught", "0");
                    try {
                        police.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        dealer.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            } else {
                timeWhileCatching = System.currentTimeMillis() - startTime;
                if (timeWhileCatching > catchTime) {
                    status = 1;
                    police.getJustice(dealer, dealer.getCaught());
                    Map<String, String> message = new HashMap<String, String>();
                    message.put("type", "catch");
                    message.put("subtype", "done");
                    message.put("caught", "1");
                    try {
                        police.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        dealer.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            //tell the clients how far the catch is and to update location
            Map<String, String> message = new HashMap<String, String>();
            message.put("type", "catch");
            message.put("subtype", "process");
            message.put("caught", "" + this.getCatchPercentage());
            message.put("fled", "" + this.getFleeingPercentage());
            try {
                police.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                dealer.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Caught: " + getCatchPercentage() + "\n");
            //try to sleep the thread for a second
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CatchThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //unmark the Users as being in a Hunt
        police.toggleCatch();
        dealer.toggleCatch();
    }

    //get status of thread
    public int getStat() {
        return this.status;
    }

    //return the percentage of the catch
    public double getCatchPercentage() {
        //not sure if this is actually going to be a double
        return (double) this.timeWhileCatching / (double) this.catchTime;
    }

    //return the amount of distance you fled until you are free in %
    public double getFleeingPercentage() {
        return this.runDistance / this.maxDistance;
    }

    /* Auskommentiert zu Testzwecken
    public void Catch(User police, User dealer) {
        status = 0;
        this.police = police;
        this.dealer = dealer;
        //mark the Users as being in a Hunt
        police.toggleCatch();
        dealer.toggleCatch();
        //start() should start run() 
        start();
    }*/
    private double getDist() {
        return geoLocToDistInMeters(police.loc[0], police.loc[1], dealer.loc[0], dealer.loc[1]);
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

    public String toString() {
        return "\tPolice: " + this.police
                + "\n\tDealer: " + this.dealer
                + "\n\tPercentage Caught: "
                + this.getCatchPercentage()
                + "\n\tPercentage Fled: "
                + this.getFleeingPercentage();
    }
}
