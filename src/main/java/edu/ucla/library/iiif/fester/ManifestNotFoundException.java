
package edu.ucla.library.iiif.fester;

import info.freelibrary.util.I18nException;

/**
 * An exception thrown when the manifest couldn't be found on the IIIF server.
 */
public class ManifestNotFoundException extends I18nException {

    /**
     * The <code>serialVersionUID</code> for a ManifestNotFoundException.
     */
    private static final long serialVersionUID = -1307404135679864330L;

    /**
     * Creates a new manifest not found exception.
     *
     * @param aMessageCode A message code
     */
    public ManifestNotFoundException(final String aMessageCode) {
        super(Constants.MESSAGES, aMessageCode);
    }

    /**
     * Creates a new manifest not found exception.
     *
     * @param aMessageCode A message code
     * @param aDetails Additional details about the exception
     */
    public ManifestNotFoundException(final String aMessageCode, final Object... aDetails) {
        super(Constants.MESSAGES, aMessageCode, aDetails);
    }

}
