package models;

import java.util.List;

import org.jongo.MongoCollection;

import javax.inject.*;

import uk.co.panaxiom.playjongo.*;

import com.google.common.collect.Lists;

/**
 * This Repository classes provide the interface between the information stored
 * in the MongoDB and the Java objects. The loading and saving is done
 * automagically, but might break in weird ways. Stackoverflow usually helps
 * there ;)
 */
@Singleton
public class FenceRepository {

    // In order to be able to access the database, Jongo has to be injected,
    // this handles all the automatic storing and loading from and to java objects.
    @Inject
    private PlayJongo jongo;

    // to avoid having to inject the repositories into the
    // game object, which then results in very weird cloning functions
    // this is a static reference to the repository.
    // This only works, as the FenceRepository is a singleton
    // and must always be created when the home controller is created,
    // which ensures that the UserRepository is never null when used
    // from the HomeController
    private static FenceRepository instance = null;

    public FenceRepository() {
        instance = this;
    }

    public static FenceRepository getInstance() {
        return instance;
    }

    /**
     * Looks up the fence collection from the database
     *
     * @return
     */
    public MongoCollection fences() {
        MongoCollection fenceCollection = jongo.getCollection("fences");

        // make sure we use 2d indices on a sphere to use geospatial queries
        fenceCollection.ensureIndex("{loc: '2dsphere'}");
        return fenceCollection;
    }

    /**
     * Alters the 2d spherical position of a fence
     *
     * @param longitude
     * @param latitude
     */
    public void updateLocation(Fence fence, Double longitude, Double latitude) {
        fences().update("{ID: #}", fence.ID).with("{$set: {loc: #}}", (Object) new Double[]{longitude, latitude});
    }

    /**
     * Fence lookup by ID
     *
     * @param ID
     * @return
     */
    public Fence findByID(int ID) {
        return fences().findOne("{ID: #}", ID).as(Fence.class);
    }

    public Iterable<Fence> findFencesNearby(Double[] loc, Double maxDistance, int limit) {
        // geoNear indicates a lookup based on spherical coordinates

        // this DB request also serves as an example for a slightly more complex mongoDB request making use
        // of the "native" mongoDB API
        List<Fence> results = Lists.newArrayList((Iterable) fences().find("{loc: {$geoNear : {$geometry : {type: 'Point', "
                + "coordinates: # }, $maxDistance: # }}}", loc, maxDistance).limit(limit) // 
                .as(Fence.class));
        return results;
    }

    private void insert(Fence f) {
        fences().save(f);
    }

    //for usage in other classes
    public void addFence(Fence f) {
        insert(f);
    }

    /**
     * Updates all fields in the DB for the object by making a copy and swapping
     * the original object in the database for it
     */
    public void update(Fence fence) {
        // copy the user to be sure that database IDs will be taken care of
        fences().update("{ID: #}", fence.ID).with(copyFence(fence));

    }

    public void initiateFences(Fence[] f, int anzahl) {
        for (int i = 0; i < anzahl; i++) {
            insert(f[i]);
        }
    }
    
    public Fence copyFence(Fence f) {
        Fence ret = new Fence();
        ret.ID = f.ID;
        ret.loc = f.loc;
        return ret;
    }
}
