
package edu.ucla.library.iiif.fester.handlers;

import info.freelibrary.util.I18nException;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

/**
 * An exception thrown when the request's media type doesn't match the expected media type.
 */
public class IncorrectMediaTypeException extends I18nException {

    /**
     * A <code>serialVersionUID</code> for this class.
     */
    private static final long serialVersionUID = 2470821347266040691L;

    /**
     * Creates an incorrect media type exception from the value of the incorrect media type.
     *
     * @param aDetail
     */
    public IncorrectMediaTypeException(final String aDetail) {
        super(Constants.MESSAGES, MessageCodes.MFS_021, aDetail);
    }

}
