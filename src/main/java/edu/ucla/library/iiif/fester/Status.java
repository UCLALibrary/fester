
package edu.ucla.library.iiif.fester;

/**
 * Constants related to status.
 */
public final class Status {

    /* Free memory property */
    public static final String FREE_MEMORY = "free_mem";

    /* Total memory property */
    public static final String TOTAL_MEMORY = "total_mem";

    /* Used memory property */
    public static final String USED_MEMORY = "used_mem";

    /* Percentage memory ((used/total)*100) property */
    public static final String PERCENT_MEMORY = "%_used_mem";

    /* Memory category for property values */
    public static final String MEMORY = "memory";

    /* The main status property */
    public static final String STATUS = "status";

    /* The okay status response */
    public static final String OK = "ok";

    /* The warn status response */
    public static final String WARN = "warn";

    /* The error status response */
    public static final String ERROR = "error";

    private Status() {
    }

}
