
package edu.ucla.library.iiif.fester;

/**
 * Constants related to status.
 */
public final class Status {

    /** Free memory property. */
    public static final String FREE_MEMORY = "free_mem";

    /** Total memory property. */
    public static final String TOTAL_MEMORY = "total_mem";

    /** Used memory property. */
    public static final String USED_MEMORY = "used_mem";

    /** Percentage memory ((used/total)*100) property. */
    public static final String PERCENT_MEMORY = "%_used_mem";

    /** Memory category for property values. */
    public static final String MEMORY = "memory";

    /** The main status property. */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    public static final String STATUS = "status";

    /** The main endpoints status property. */
    public static final String ENDPOINTS = "endpoints";

    /** The okay status response. */
    public static final String OK = "ok";

    /** The warn status response. */
    public static final String WARN = "warn";

    /** The error status response. */
    public static final String ERROR = "error";

    /** The PUT response HTTP code. */
    public static final String PUT_RESPONSE = "put.response";

    /** The GET response HTTP code. */
    public static final String GET_RESPONSE = "get.response";

    /** The DELETE response HTTP code. */
    public static final String DELETE_RESPONSE = "delete.response";

    /**
     * Creates a new status object.
     */
    private Status() {
        // This is intentionally left empty
    }

}
