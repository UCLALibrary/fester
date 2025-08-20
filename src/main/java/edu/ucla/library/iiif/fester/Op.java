
package edu.ucla.library.iiif.fester;

/**
 * Constants representing API operations.
 */
public final class Op {

    /**
     * Get the status of Fester.
     */
    public static final String GET_STATUS = "getStatus";

    /**
     * Get a manifest from Fester.
     */
    public static final String GET_MANIFEST = "getManifest";

    /**
     * Put a manifest into Fester.
     */
    public static final String PUT_MANIFEST = "putManifest";

    /**
     * Delete a manifest from Fester.
     */
    public static final String DELETE_MANIFEST = "deleteManifest";

    /**
     * Get a collection from Fester.
     */
    public static final String GET_COLLECTION = "getCollection";

    /**
     * Put a collection into Fester.
     */
    public static final String PUT_COLLECTION = "putCollection";

    /**
     * Delete a collection from Fester.
     */
    public static final String DELETE_COLLECTION = "deleteCollection";

    /**
     * Post a CSV file to Fester for processing of its contents.
     */
    public static final String POST_CSV = "postCSV";

    /**
     * Post a CSV file to Fester for addition of thumbnail value.
     */
    public static final String POST_THUMB = "postThumb";

    /**
     * Post a CSV file to Fester for the update of existing work manifests.
     */
    public static final String POST_UPDATE_CSV = "postUpdateCSV";

    /**
     * Get S3 endpoints status.
     */
    public static final String CHECK_ENDPOINTS = "checkEndpoints";

    /**
     * A successful operation.
     */
    public static final String SUCCESS = "success";

    /**
     * An operation that needs to be retried.
     */
    public static final String RETRY = "retry";

    /**
     * A failed operation.
     */
    public static final String FAILURE = "failure";

    private Op() {
    }

}
