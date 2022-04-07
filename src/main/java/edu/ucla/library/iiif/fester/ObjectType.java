
package edu.ucla.library.iiif.fester;

import static edu.ucla.library.iiif.fester.Constants.EMPTY;

/**
 * An enumeration of the expected object types.
 */
public enum ObjectType {

    /**
     * A collection type.
     */
    COLLECTION("Collection"),

    /**
     * A work type.
     */
    WORK("Work"),

    /**
     * A page type.
     */
    PAGE("Page"),

    /**
     * A page missing.
     */
    MISSING(EMPTY);

    /**
     * An object type.
     */
    private String myValue;

    /**
     * Creates a new ObjectType from the supplied string value
     *
     * @param aValue A valid string value of an object type
     */
    ObjectType(final String aValue) {
        myValue = aValue;
    }

    /**
     * Gets the value of the object type.
     *
     * @return The value of the object type
     */
    public String getValue() {
        return myValue;
    }

    @Override
    public String toString() {
        return getValue();
    }

    /**
     * Returns true if the supplied string equals the string value of this object type.
     *
     * @param aString A string to compare to the string value of this object type
     * @return True if the two strings are equal; else, false
     */
    @SuppressWarnings("PMD.SuspiciousEqualsMethodName") // Cannot override enum's equals
    public boolean equals(final String aString) {
        return getValue().equalsIgnoreCase(aString);
    }
}
