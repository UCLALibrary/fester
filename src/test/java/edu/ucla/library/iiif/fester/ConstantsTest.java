
package edu.ucla.library.iiif.fester;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests of constant values.
 */
public class ConstantsTest {

    /**
     * Tests the constants path which is also a part of our OpenAPI Fester specification.
     */
    @Test
    public final void test() {
        assertEquals("/collections", Constants.POST_CSV_ROUTE);
    }

}
