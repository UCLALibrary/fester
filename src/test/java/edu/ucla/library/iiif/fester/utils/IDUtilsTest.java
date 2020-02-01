
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.Test;

/**
 * Tests of IDUtils.
 */
public class IDUtilsTest {

    private static final String FAKE_HOST = "http://localhost:9999";

    private static final String WORK_MANIFEST_ID = "ark:/21198/zz000bjfsv";

    private static final String WORK_MANIFEST_URI_PATH = "/ark%3A%2F21198%2Fzz000bjfsv/manifest";

    private static final String WORK_MANIFEST_URI = FAKE_HOST + WORK_MANIFEST_URI_PATH;

    private static final String WORK_MANIFEST_S3_KEY = "ark:/21198/zz000bjfsv.json";


    private static final String COLLECTION_MANIFEST_ID = "ark:/21198/zz0009gss9";

    private static final String COLLECTION_MANIFEST_URI_PATH = "/collections/ark%3A%2F21198%2Fzz0009gss9";

    private static final String COLLECTION_MANIFEST_URI = FAKE_HOST + COLLECTION_MANIFEST_URI_PATH;

    private static final String COLLECTION_MANIFEST_S3_KEY = "collections/ark:/21198/zz0009gss9.json";

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
     * Tests S3 key to URI mapping for a work.
     */
    @Test
    public final void testGetResourceURIWork() {
        final String expected = WORK_MANIFEST_URI;
        final String found = IDUtils.getResourceURI(FAKE_HOST, WORK_MANIFEST_S3_KEY).toString();

        assertEquals(expected, found);
    }

    /**
     * Tests S3 key to URI mapping for a collection.
     */
    @Test
    public final void testGetResourceURICollection() {
        final String expected = COLLECTION_MANIFEST_URI;
        final String found = IDUtils.getResourceURI(FAKE_HOST, COLLECTION_MANIFEST_S3_KEY)
                .toString();

        assertEquals(expected, found);
    }

    /**
     * Tests S3 key to URI path mapping for a work.
     */
    @Test
    public final void testGetResourceURIPathWork() {
        final String expected = WORK_MANIFEST_URI_PATH;
        final String found = IDUtils.getResourceURIPath(WORK_MANIFEST_S3_KEY);

        assertEquals(expected, found);
    }

    /**
     * Tests S3 key to URI path mapping for a collection.
     */
    @Test
    public final void testGetResourceURIPathCollection() {
        final String expected = COLLECTION_MANIFEST_URI_PATH;
        final String found = IDUtils.getResourceURIPath(COLLECTION_MANIFEST_S3_KEY);

        assertEquals(expected, found);
    }

    /**
     * Tests URI to S3 key mapping for a work.
     */
    @Test
    public final void testGetResourceS3KeyWork() {
        final String expected = WORK_MANIFEST_S3_KEY;
        final String found = IDUtils.getResourceS3Key(URI.create(WORK_MANIFEST_URI));

        assertEquals(expected, found);
    }

    /**
     * Tests URI to S3 key mapping for a collection.
     */
    @Test
    public final void testGetResourceS3KeyCollection() {
        final String expected = COLLECTION_MANIFEST_S3_KEY;
        final String found = IDUtils.getResourceS3Key(URI.create(COLLECTION_MANIFEST_URI));

        assertEquals(expected, found);
    }

    /**
     * Tests URI to ID (ARK) mapping for a work.
     */
    @Test
    public final void testGetResourceIDWorkURI() {
        final String expected = WORK_MANIFEST_ID;
        final String found = IDUtils.getResourceID(URI.create(WORK_MANIFEST_URI));

        assertEquals(expected, found);
    }

    /**
     * Tests URI to ID (ARK) mapping for a collection.
     */
    @Test
    public final void testGetResourceIDCollectionURI() {
        final String expected = COLLECTION_MANIFEST_ID;
        final String found = IDUtils.getResourceID(URI.create(COLLECTION_MANIFEST_URI));

        assertEquals(expected, found);
    }

    /**
     * Tests S3 key to ID (ARK) mapping for a work.
     */
    @Test
    public final void testGetResourceIDWorkS3Key() {
        final String expected = WORK_MANIFEST_ID;
        final String found = IDUtils.getResourceID(WORK_MANIFEST_S3_KEY);

        assertEquals(expected, found);
    }

    /**
     * Tests S3 key to ID (ARK) mapping for a collection.
     */
    @Test
    public final void testGetResourceIDCollectionS3Key() {
        final String expected = COLLECTION_MANIFEST_ID;
        final String found = IDUtils.getResourceID(COLLECTION_MANIFEST_S3_KEY);

        assertEquals(expected, found);
    }

    /**
     * Tests ID (ARK) to S3 key mapping for a work.
     */
    @Test
    public final void testGetWorkS3Key() {
        final String expected = WORK_MANIFEST_S3_KEY;
        final String found = IDUtils.getWorkS3Key(WORK_MANIFEST_ID);

        assertEquals(expected, found);
    }

    /**
     * Tests ID (ARK) to S3 key mapping for a collection.
     */
    @Test
    public final void testGetCollectionS3Key() {
        final String expected = COLLECTION_MANIFEST_S3_KEY;
        final String found = IDUtils.getCollectionS3Key(COLLECTION_MANIFEST_ID);

        assertEquals(expected, found);
    }
}
