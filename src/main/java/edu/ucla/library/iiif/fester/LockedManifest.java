
package edu.ucla.library.iiif.fester;

import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;

/**
 * A manifest that is locked and cannot be updated.
 */
public class LockedManifest {

    private final Lock myLock;

    private final JsonObject myManifest;

    private final boolean myManifestIsCollection;

    /**
     * Creates a locked manifest.
     *
     * @param aManifest A manifest in JSON form
     * @param aCollection A collection document
     * @param aLock A Vert.x lock
     */
    public LockedManifest(final JsonObject aManifest, final boolean aCollection, final Lock aLock) {
        myManifestIsCollection = aCollection;
        myManifest = aManifest;
        myLock = aLock;
    }

    /**
     * Gets whether this is a collection document.
     *
     * @return True if the manifest is for a collection
     */
    public boolean isCollection() {
        return myManifestIsCollection;
    }

    /**
     * Return the JSON encoded manifest.
     *
     * @return The JSON representation of a IIIF presentation manifest
     */
    public JsonObject toJSON() {
        return myManifest;
    }

    /**
     * Gets whether this is a work manifest.
     *
     * @return True if the manifest is for a work
     */
    public boolean isWork() {
        return !myManifestIsCollection;
    }

    /**
     * Gets the manifest's lock.
     *
     * @return The manifest's lock
     */
    public Lock getLock() {
        return myLock;
    }

    /**
     * Release the lock on the manifest.
     */
    public void release() {
        myLock.release();
    }
}
