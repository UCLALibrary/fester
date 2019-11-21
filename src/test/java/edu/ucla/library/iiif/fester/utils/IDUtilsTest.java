
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * Tests of IDUtils.
 */
public class IDUtilsTest {

    private static final String COLLECTIONS = "/collections";

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
