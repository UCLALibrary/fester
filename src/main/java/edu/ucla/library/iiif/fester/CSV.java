
package edu.ucla.library.iiif.fester;

/**
 * CSV headers that we care about for our manifest generation.
 */
public final class CSV {

    /**
     * The ARK identifier for a row.
     */
    public static final String ITEM_ARK = "Item ARK";

    /**
     * The ARK identifier for the parent object.
     */
    public static final String PARENT_ARK = "Parent ARK";

    /**
     * The manifest URL for works and collections.
     */
    public static final String MANIFEST_URL = "IIIF Manifest URL";

    /**
     * The object type for the row
     */
    public static final String OBJECT_TYPE = "Object Type";

    /**
     * The row's file name. Not all rows are intended to have files.
     */
    public static final String FILE_NAME = "File Name";

    /**
     * The row's title.
     */
    public static final String TITLE = "Title";

    /**
     * The row's item sequence.
     */
    public static final String ITEM_SEQ = "Item Sequence";

    /**
     * The row's viewing hint.
     */
    public static final String VIEWING_HINT = "viewingHint";

    /**
     * The row's viewing direction.
     */
    public static final String VIEWING_DIRECTION = "Text direction";

    /**
     * The row's image access URL.
     */
    public static final String IIIF_ACCESS_URL = "IIIF Access URL";

    /**
     * The rows's audio/video access URL.
     */
    public static final String AV_ACCESS_URL = "AV Access URL";

    /**
     * The row's repository name.
     */
    public static final String REPOSITORY_NAME = "Name.repository";

    /**
     * The row's local rights statement.
     */
    public static final String LOCAL_RIGHTS_STATEMENT = "Rights.statementLocal";

    /**
     * The row's rights contact.
     */
    public static final String RIGHTS_CONTACT = "Rights.servicesContact";

    /**
     * The row's media width.
     */
    public static final String MEDIA_WIDTH = "media.width";

    /**
     * The row's media height.
     */
    public static final String MEDIA_HEIGHT = "media.height";

    /**
     * The row's media duration.
     */
    public static final String MEDIA_DURATION = "media.duration";

    /**
     * The row's media format.
     */
    public static final String MEDIA_FORMAT = "media.format";

    private CSV() {
    }
}
