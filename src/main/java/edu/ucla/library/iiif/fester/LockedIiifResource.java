
package edu.ucla.library.iiif.fester;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;

/**
 * A IIIF resource that is locked and cannot be updated.
 */
public class LockedIiifResource {

    private final Lock myLock;

    private final JsonObject myResource;

    private final boolean myResourceIsCollection;

    /**
     * Creates a locked IIIF resource.
     *
     * @param aResource A manifest or collection in JSON form
     * @param aCollDoc If the resource is a collection
     * @param aLock A Vert.x lock
     */
    public LockedIiifResource(final JsonObject aResource, final boolean aCollDoc, final Lock aLock) {
        myResourceIsCollection = aCollDoc;
        myResource = aResource;
        myLock = aLock;
    }

    /**
     * Gets whether this is a collection.
     *
     * @return True if the resource is a collection
     */
    public boolean isCollection() {
        return myResourceIsCollection;
    }

    /**
     * Return the JSON encoded resource.
     *
     * @return The JSON representation of a IIIF presentation resource
     */
    public JsonObject toJSON() {
        return myResource;
    }

    /**
     * Gets whether this is a manifest.
     *
     * @return True if the resource is a manifest
     */
    public boolean isManifest() {
        return !myResourceIsCollection;
    }

    /**
     * Gets the resource's lock.
     *
     * @return The resource's lock
     */
    public Lock getLock() {
        return myLock;
    }

    /**
     * Release the lock on the resource.
     */
    public void release() {
        myLock.release();
    }
}
