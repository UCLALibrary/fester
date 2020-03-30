
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests related to the ContainerUtils test class.
 */
public class ContainerUtilsTest {

    private static final String STABLE_VERSION = "uclalibrary/fester:0.0.1";

    /**
     * Tests the <code>toTag()</code> method.
     */
    @Test
    public final void testToTagLatest() {
        assertEquals("uclalibrary/fester:latest", ContainerUtils.toTag("uclalibrary/fester:0.0.1-SNAPSHOT"));
    }

    /**
     * Tests the <code>toTag()</code> method.
     */
    @Test
    public final void testToTagVersioned() {
        assertEquals(STABLE_VERSION, ContainerUtils.toTag(STABLE_VERSION));
    }
}
