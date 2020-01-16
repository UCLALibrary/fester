
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

    private CSV() {
    }
}
