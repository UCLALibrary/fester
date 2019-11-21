
package edu.ucla.library.iiif.fester;

/**
 * CSV headers that we can about for our manifest generation.
 */
public final class CSV {

    /**
     * The name of a project. We make the assumption of one project per CSV.
     */
    public static final String PROJECT_NAME = "Project Name";

    /**
     * The ARK identifier for a row.
     */
    public static final String ITEM_ARK = "Item ARK";

    /**
     * The ARK identifier for the parent object.
     */
    public static final String PARENT_ARK = "Parent ARK";

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

    private CSV() {
    }
}
