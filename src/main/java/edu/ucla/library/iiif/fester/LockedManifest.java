
package edu.ucla.library.iiif.fester;

import info.freelibrary.iiif.presentation.Collection;
import info.freelibrary.iiif.presentation.Manifest;

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
     * @param aLock A Vertx lock
     */
    public LockedManifest(final JsonObject aManifest, final boolean aCollection, final Lock aLock) {
        myManifestIsCollection = aCollection;
        myManifest = aManifest;
        myLock = aLock;
    }

    /**
     * Gets whether this is a collection (or a work).
     *
     * @return True if the manifest is for a collection
     */
    public boolean isCollection() {
        return myManifestIsCollection;
    }

    /**
     * Gets the manifest content as a work manifest object.
     *
     * @return The manifest's contents
     */
    public Manifest getWork() {
        return Manifest.fromJSON(myManifest);
    }

    /**
     * Gets the manifest content as a Collection object.
     *
     * @return The manifest's contents
     */
    public Collection getCollection() {
        return Collection.fromJSON(myManifest);
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
