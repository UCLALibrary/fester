
package edu.ucla.library.iiif.fester.utils;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;

/**
 * Convenience utilities for working with MessageCodes.
 */
public final class CodeUtils {

    /** The logger used by the code utilities. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeUtils.class, Constants.MESSAGES);

    /**
     * Creates a new utilities class.
     */
    private CodeUtils() {
        // This is a utility class
    }

    /**
     * This convenience method returns a message code in integer form when supplied with the string form.
     *
     * @param aMessageCode A message code
     * @return A message code in integer form
     * @throws IllegalArgumentException If the supplied string isn't a message code
     */
    public static int getInt(final String aMessageCode) {
        try {
            return Integer.parseInt(aMessageCode.substring(aMessageCode.lastIndexOf('-') + 1));
        } catch (final NumberFormatException details) {
            throw new IllegalArgumentException(LOGGER.getMessage(MessageCodes.MFS_109, aMessageCode), details);
        }
    }

    /**
     * This convenience method returns a message code in string form when supplied with the int form.
     *
     * @param aMessageCode An int version of the message code
     * @return The message code in string form
     */
    public static String getCode(final int aMessageCode) {
        final String prefix = MessageCodes.MFS_000.substring(0, MessageCodes.MFS_000.indexOf('-'));
        final String intCode = String.format("%03d", aMessageCode);

        return new StringBuilder(prefix).append('-').append(intCode).toString();
    }
}
