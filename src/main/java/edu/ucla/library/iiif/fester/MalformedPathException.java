
package edu.ucla.library.iiif.fester;

import java.util.Locale;

import info.freelibrary.util.I18nRuntimeException;

/**
 * A runtime exception thrown when a path is incorrectly constructed.
 */
public class MalformedPathException extends I18nRuntimeException {

    /**
     * The <code>serialVersionUID</code> for MalformedPathException.
     */
    private static final long serialVersionUID = 3817178883041384608L;

    /**
     * Creates a new path structure exception.
     */
    public MalformedPathException() {
    }

    /**
     * Creates a new path structure exception from the supplied parent exception.
     *
     * @param aCause An underlying cause of the exception
     */
    public MalformedPathException(final Throwable aCause) {
        super(aCause);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key).
     *
     * @param aMessageKey An exception message (or message key)
     */
    public MalformedPathException(final String aMessageKey) {
        super(Constants.MESSAGES, aMessageKey);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key) and locale.
     *
     * @param aLocale A locale
     * @param aMessageKey An exception message (or message key)
     */
    public MalformedPathException(final Locale aLocale, final String aMessageKey) {
        super(aLocale, Constants.MESSAGES, aMessageKey);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key) and additional
     * details.
     *
     * @param aMessageKey An exception message (or message key)
     * @param aVarargs Additional details about the exception
     */
    public MalformedPathException(final String aMessageKey, final Object... aVarargs) {
        super(Constants.MESSAGES, aMessageKey, aVarargs);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key) and parent
     * exception.
     *
     * @param aCause An underlying exception
     * @param aMessageKey An exception message (or message key)
     */
    public MalformedPathException(final Throwable aCause, final String aMessageKey) {
        super(aCause, Constants.MESSAGES, aMessageKey);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key), locale, and
     * additional details.
     *
     * @param aLocale A locale
     * @param aMessageKey An exception message (or message key)
     * @param aVarargs Additional details about the exception
     */
    public MalformedPathException(final Locale aLocale, final String aMessageKey, final Object... aVarargs) {
        super(aLocale, Constants.MESSAGES, aMessageKey, aVarargs);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key), locale, and parent
     * exception.
     *
     * @param aCause An underlying exception
     * @param aLocale A locale
     * @param aMessageKey An exception message (or message key)
     */
    public MalformedPathException(final Throwable aCause, final Locale aLocale, final String aMessageKey) {
        super(aCause, aLocale, Constants.MESSAGES, aMessageKey);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key), parent exception,
     * and additional details.
     *
     * @param aCause An underlying exception
     * @param aMessageKey An exception message (or message key)
     * @param aVarargs Additional details about the exception
     */
    public MalformedPathException(final Throwable aCause, final String aMessageKey, final Object... aVarargs) {
        super(aCause, Constants.MESSAGES, aMessageKey, aVarargs);
    }

    /**
     * Creates a new path structure exception from the supplied exception message (or message key), locale, and parent
     * exception.
     *
     * @param aCause An underlying exception
     * @param aLocale A locale
     * @param aMessageKey An exception message (or message key)
     * @param aVarargs Additional details about the exception
     */
    public MalformedPathException(final Throwable aCause, final Locale aLocale, final String aMessageKey,
            final Object... aVarargs) {
        super(aCause, aLocale, Constants.MESSAGES, aMessageKey, aVarargs);
    }

}
