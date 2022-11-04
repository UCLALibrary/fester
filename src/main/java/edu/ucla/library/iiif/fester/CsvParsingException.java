
package edu.ucla.library.iiif.fester;

import info.freelibrary.util.I18nException;

/**
 * A CSV parsing exception.
 */
public class CsvParsingException extends I18nException {

    /**
     * The <code>serialVersionUID</code> for CsvParsingException.
     */
    private static final long serialVersionUID = 4027512612554546268L;

    /**
     * Creates a new CSV parsing exception.
     *
     * @param aThrowable A underlying cause of the exception
     */
    public CsvParsingException(final Throwable aThrowable) {
        super(aThrowable);
    }

    /**
     * Creates a new CSV parsing exception.
     *
     * @param aMessageCode A message code
     */
    public CsvParsingException(final String aMessageCode) {
        super(Constants.MESSAGES, aMessageCode);
    }

    /**
     * Creates a new CSV parsing exception.
     *
     * @param aMessageCode A message code
     * @param aDetails Additional details about the exception
     */
    public CsvParsingException(final String aMessageCode, final Object... aDetails) {
        super(Constants.MESSAGES, aMessageCode, aDetails);
    }
}
