package models;
//manche imports unnötig

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.PushNotifications;

import java.io.IOException;
import java.util.Map;

public class Fence {
    // used by Jongo to map JVM objects to database Objects

    @JsonProperty("_id")
    public String id;
    public int ID;

    public Double[] loc = new Double[]{11.5833, 48.15}; // Garching

    /**
     * **************
     * Object methods ------------- *************
     */
    public Fence() {
    }

    public Fence(int ID, Double longitude, Double latitude) {
        this.ID = ID;
        this.loc = new Double[]{longitude, latitude};
    }
    
     public String toString() {
    	return "Fence \tID: "+this.ID
    			+"\n\tlocation: "+this.loc[0]+","+this.loc[1];
    }
}
