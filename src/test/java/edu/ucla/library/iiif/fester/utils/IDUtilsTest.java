
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.Test;

/**
 * Tests of IDUtils.
 */
public class IDUtilsTest {

    private static final String COLLECTIONS = "/collections";

    /**
     * Tests getIDs().
     */
    @Test
    public final void testGetIDs() {
        final List<String> ids = IDUtils.getIDs();

        assertEquals(1000, ids.size());
        assertEquals(6, ids.get(0).length());
    }

    /**
     * Test getIDs() with a size and length of ID supplied.
     */
    @Test
    public final void testGetIDsIntInt() {
        final List<String> ids = IDUtils.getIDs(100, 5);

        assertEquals(100, ids.size());
        assertEquals(5, ids.get(0).length());
    }

    /**
     * Tests getting an ID from a supplied string.
     */
    @Test
    public final void testGetLastPartString() {
        assertEquals("z1z61s9k", IDUtils.getLastPart(
                "https://iiif.sinaimanuscripts.library.ucla.edu/iiif/2/21198%2Fz1z61s9k"));
    }

    /**
     * Tests getting an ID from a supplied string.
     */
    @Test
    public final void testGetLastPartStringWithoutSlashes() {
        final String id = "YadaYadaYada";
        assertEquals(id, IDUtils.getLastPart(id));
    }

    /**
     * Tests collection ID encoding.
     */
    @Test
    public final void testEncodeStringStringString() {
        final String expected = "http://localhost:9999/collections/ark%3A%2F21198%2Fzz0009gss9";
        final String found = IDUtils.encode("http://localhost:9999", COLLECTIONS, "ark:/21198/zz0009gss9");

        assertEquals(expected, found);
    }

    /**
     * Tests work ID encoding.
     */
    @Test
    public final void testEncodeStringString() {
        final String expected = "https://localhost:9999/ark%3A%2F21198%2Fzz000bjfsv/manifest";
        final String found = IDUtils.encode("https://localhost:9999", "ark:/21198/zz000bjfsv");

        assertEquals(expected, found);
    }

    /**
     * Tests work ID decoding.
     */
    @Test
    public final void testDecodeURI() {
        final URI encoded = URI.create("http://localhost:9999/ark%3A%2F21198%2Fzz000bjfvv/manifest");
        final String expected = "ark:/21198/zz000bjfvv";

        assertEquals(expected, IDUtils.decode(encoded));
    }

    /**
     * Tests collection ID decoding.
     */
    @Test
    public final void testDecodeUriString() {
        final URI encoded = URI.create("http://localhost:9999/collections/ark%3A%2F21198%2Fzz0009gsq9");
        final String expected = "ark:/21198/zz0009gsq9";

        assertEquals(expected, IDUtils.decode(encoded, COLLECTIONS));
    }

}
