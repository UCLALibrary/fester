
package edu.ucla.library.iiif.fester.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MalformedPathException;

/**
 * Tests of IDUtils.
 */
public class IDUtilsTest {

    private static final String FAKE_HOST = "http://localhost:9999";

    private static final String WORK_MANIFEST_ID = "ark:/21198/zz000bjfsv";

    private static final String WORK_MANIFEST_URI_PATH = "/ark%3A%2F21198%2Fzz000bjfsv/manifest";

    private static final String WORK_MANIFEST_URI = FAKE_HOST + WORK_MANIFEST_URI_PATH;

    private static final String WORK_MANIFEST_S3_KEY = "works/ark:/21198/zz000bjfsv.json";

    private static final String COLLECTION_MANIFEST_ID = "ark:/21198/zz0009gss9";

    private static final String COLLECTION_MANIFEST_URI_PATH = "/collections/ark%3A%2F21198%2Fzz0009gss9";

    private static final String COLLECTION_MANIFEST_URI = FAKE_HOST + COLLECTION_MANIFEST_URI_PATH;

    private static final String COLLECTION_MANIFEST_S3_KEY = "collections/ark:/21198/zz0009gss9.json";

    private String myTestID;

    /**
     * Create a fake ID for testing.
     */
    @Before
    public final void setUp() {
        myTestID = UUID.randomUUID().toString();
    }

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
        assertEquals("z1z61s9k",
                IDUtils.getLastPart("https://iiif.sinaimanuscripts.library.ucla.edu/iiif/2/21198%2Fz1z61s9k"));
    }

    /**
     * Tests getting an ID from a supplied string.
     */
    @Test
    public final void testGetLastPartStringWithoutSlashes() {
        assertEquals(myTestID, IDUtils.getLastPart(myTestID));
    }

    /**
     * Tests S3 key to URI mapping for a work.
     */
    @Test
    public final void testGetResourceURIWork() {
        assertEquals(WORK_MANIFEST_URI, IDUtils.getResourceURI(FAKE_HOST, WORK_MANIFEST_S3_KEY).toString());
    }

    /**
     * Tests S3 key to URI mapping for a collection.
     */
    @Test
    public final void testGetResourceURICollection() {
        assertEquals(COLLECTION_MANIFEST_URI, IDUtils.getResourceURI(FAKE_HOST, COLLECTION_MANIFEST_S3_KEY).toString());
    }

    /**
     * Tests S3 key to URI path mapping for a work.
     */
    @Test
    public final void testGetResourceURIPathWork() {
        assertEquals(WORK_MANIFEST_URI_PATH, IDUtils.getResourceURIPath(WORK_MANIFEST_S3_KEY));
    }

    /**
     * Tests S3 key to URI path mapping for a collection.
     */
    @Test
    public final void testGetResourceURIPathCollection() {
        assertEquals(COLLECTION_MANIFEST_URI_PATH, IDUtils.getResourceURIPath(COLLECTION_MANIFEST_S3_KEY));
    }

    /**
     * Tests that getResourceURIPath() doesn't accept things without the required paths.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceURIPathInvalid() {
        IDUtils.getResourceURIPath(myTestID);
    }

    /**
     * Tests URI to S3 key mapping for a work.
     */
    @Test
    public final void testGetResourceS3KeyWork() {
        assertEquals(WORK_MANIFEST_S3_KEY, IDUtils.getResourceS3Key(URI.create(WORK_MANIFEST_URI)));
    }

    /**
     * Tests URI to S3 key mapping for a collection.
     */
    @Test
    public final void testGetResourceS3KeyCollection() {
        assertEquals(COLLECTION_MANIFEST_S3_KEY, IDUtils.getResourceS3Key(URI.create(COLLECTION_MANIFEST_URI)));
    }

    /**
     * Tests getResourceS3Key() method to make sure it doesn't accept keys that are invalid.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceS3KeyInvalid() {
        IDUtils.getResourceS3Key(URI.create(myTestID));
    }

    /**
     * Tests the check on pre-existing collection S3 key prefixes.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceS3KeyPrefixed() {
        IDUtils.getResourceS3Key(URI.create(Constants.COLLECTION_S3_KEY_PREFIX + myTestID));
    }

    /**
     * Tests the check on pre-existing collection S3 key extensions.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceS3KeyExtension() {
        IDUtils.getResourceS3Key(URI.create(myTestID + Constants.DOT + Constants.JSON_EXT));
    }

    /**
     * Tests URI to ID (ARK) mapping for a work.
     */
    @Test
    public final void testGetResourceIDWorkURI() {
        assertEquals(WORK_MANIFEST_ID, IDUtils.getResourceID(URI.create(WORK_MANIFEST_URI)));
    }

    /**
     * Tests URI to ID (ARK) mapping for a collection.
     */
    @Test
    public final void testGetResourceIDCollectionURI() {
        assertEquals(COLLECTION_MANIFEST_ID, IDUtils.getResourceID(URI.create(COLLECTION_MANIFEST_URI)));
    }

    /**
     * Tests getResourceID() method to make sure it doesn't except invalid URIs.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceIDInvalidURI() {
        IDUtils.getResourceID(URI.create(myTestID));
    }

    /**
     * Tests S3 key to ID (ARK) mapping for a work.
     */
    @Test
    public final void testGetResourceIDWorkS3Key() {
        assertEquals(WORK_MANIFEST_ID, IDUtils.getResourceID(WORK_MANIFEST_S3_KEY));
    }

    /**
     * Tests S3 key to ID (ARK) mapping for a collection.
     */
    @Test
    public final void testGetResourceIDCollectionS3Key() {
        assertEquals(COLLECTION_MANIFEST_ID, IDUtils.getResourceID(COLLECTION_MANIFEST_S3_KEY));
    }

    /**
     * Tests to make sure getResourceID() doesn't accept invalid keys.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetResourceIDInvalidKey() {
        IDUtils.getResourceID(myTestID);
    }

    /**
     * Tests ID (ARK) to S3 key mapping for a work.
     */
    @Test
    public final void testGetWorkS3Key() {
        assertEquals(WORK_MANIFEST_S3_KEY, IDUtils.getWorkS3Key(WORK_MANIFEST_ID));
    }

    /**
     * Tests S3 key mapping when the key already has the prefix to be added.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetWorkS3KeyPrefix() {
        IDUtils.getWorkS3Key(Constants.WORK_S3_KEY_PREFIX + WORK_MANIFEST_ID);
    }

    /**
     * Tests S3 key mapping when the key already has the extension to be added.
     */
    @Test(expected = MalformedPathException.class)
    public final void testGetWorkS3KeyExt() {
        IDUtils.getWorkS3Key(WORK_MANIFEST_ID + Constants.DOT + Constants.JSON_EXT);
    }

    /**
     * Tests ID (ARK) to S3 key mapping for a collection.
     */
    @Test
    public final void testGetCollectionS3Key() {
        assertEquals(COLLECTION_MANIFEST_S3_KEY, IDUtils.getCollectionS3Key(COLLECTION_MANIFEST_ID));
    }
}
