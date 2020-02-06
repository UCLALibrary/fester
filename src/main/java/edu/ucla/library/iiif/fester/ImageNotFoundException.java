
package edu.ucla.library.iiif.fester;

import info.freelibrary.util.I18nException;

/**
 * An exception thrown when an image can't be found on the IIIF server.
 */
public class ImageNotFoundException extends I18nException {

    /**
     * The <code>serialVersionUID</code> for an ImageNotFoundException.
     */
    private static final long serialVersionUID = -1307404135679864330L;

    /**
     * Creates a new image not found exception.
     *
     * @param aMessageCode A message code
     */
    public ImageNotFoundException(final String aMessageCode) {
        super(Constants.MESSAGES, aMessageCode);
    }

    /**
     * Creates a new image not found exception.
     *
     * @param aMessageCode A message code
     * @param aDetails Additional details about the exception
     */
    public ImageNotFoundException(final String aMessageCode, final Object... aDetails) {
        super(Constants.MESSAGES, aMessageCode, aDetails);
    }

}
