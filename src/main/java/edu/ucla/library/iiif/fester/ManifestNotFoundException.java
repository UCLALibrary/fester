package edu.ucla.library.iiif.fester;

import info.freelibrary.util.I18nException;

/**
 * An exception thrown when a collection or manifest can't be found.
 */
public class ManifestNotFoundException extends I18nException {

    /**
     * The <code>serialVersionUID</code> for a ManifestNotFoundException.
     */
    private static final long serialVersionUID = -278145895817091916L;

    /**
     * Creates a new manifest not found exception.
     *
     * @param aCause The throwable that caused the exception
     * @param aMessageCode A message code
     * @param aDetails Additional details about the exception
     */
    public ManifestNotFoundException(final Throwable aCause, final String aMessageCode, final Object... aDetails) {
        super(aCause, Constants.MESSAGES, aMessageCode, aDetails);
    }
}
