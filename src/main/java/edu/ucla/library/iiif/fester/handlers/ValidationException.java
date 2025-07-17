
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.I18nException;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

/**
 * An exception thrown if a manifest or collection document isn't valid according to the IIIF Presentation spec.
 */
public class ValidationException extends I18nException {

    /** The validation exception's <code>serialVersionUID</code>. */
    private static final long serialVersionUID = -5771374166580990926L;

    /**
     * Creates a new validation exception.
     */
    public ValidationException(final String aResourceType, final String aMessage) {
        super(Constants.MESSAGES, MessageCodes.MFS_180, aResourceType, aMessage);
    }

    /**
     * Creates a new validation exception from the supplied parent exception.
     */
    public ValidationException(final String aResourceType, final Exception anException) {
        super(Constants.MESSAGES, MessageCodes.MFS_180, aResourceType, anException.getMessage(), anException);
    }
}
