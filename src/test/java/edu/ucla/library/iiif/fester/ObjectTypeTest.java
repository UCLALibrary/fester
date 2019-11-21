
package edu.ucla.library.iiif.fester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests of the ObjectType enumeration.
 */
public class ObjectTypeTest {

    private static final String COLLECTION = "Collection";

    private static final String WORK = "Work";

    private static final String PAGE = "Page";

    /**
     * Tests getting the collection value.
     */
    @Test
    public final void testGetCollectionValue() {
        assertEquals(COLLECTION, ObjectType.COLLECTION.getValue());
    }

    /**
     * Tests the collection toString method.
     */
    @Test
    public final void testCollectionToString() {
        assertEquals(COLLECTION, ObjectType.COLLECTION.toString());
    }

    /**
     * Tests getting the work value.
     */
    @Test
    public final void testGetWorkValue() {
        assertEquals(WORK, ObjectType.WORK.getValue());
    }

    /**
     * Tests the work toString method.
     */
    @Test
    public final void testWorkToString() {
        assertEquals(WORK, ObjectType.WORK.toString());
    }

    /**
     * Tests getting the page value.
     */
    @Test
    public final void testGetPageValue() {
        assertEquals(PAGE, ObjectType.PAGE.getValue());
    }

    /**
     * Tests the page toString method.
     */
    @Test
    public final void testPageToString() {
        assertEquals(PAGE, ObjectType.PAGE.toString());
    }

    /**
     * Tests the equality of a collection string.
     */
    @Test
    public final void testEqualsCollectionString() {
        assertTrue(ObjectType.COLLECTION.equals(COLLECTION));
    }

    /**
     * Tests the equality of a work string.
     */
    @Test
    public final void testEqualsWorkString() {
        assertTrue(ObjectType.WORK.equals(WORK));
    }

    /**
     * Tests the equality of a page string.
     */
    @Test
    public final void testEqualsPageString() {
        assertTrue(ObjectType.PAGE.equals(PAGE));
    }
}
