
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.I18nException;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

/**
 * Exception thrown when a request is missing a required media-type
 */
public class MissingMediaTypeException extends I18nException {

    /**
     * The <code>serialVersionUID</code> for this class.
     */
    private static final long serialVersionUID = -3004544782946247794L;

    /**
     * Creates an incorrect media type exception from the value of the incorrect media type.
     */
    public MissingMediaTypeException() {
        super(Constants.MESSAGES, MessageCodes.MFS_020);
    }

}
