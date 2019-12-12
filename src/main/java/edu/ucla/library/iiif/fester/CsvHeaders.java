
package edu.ucla.library.iiif.fester;

/**
 * The CSV file's headers.
 */
public class CsvHeaders {

    private int myItemArkIndex;

    private int myParentArkIndex;

    private int myTitleIndex;

    private int myProjectNameIndex;

    private int myObjectTypeIndex;

    private int myFileNameIndex;

    private int myItemSequence;

    /**
     * Create a new CSV headers object.
     *
     * @param aRow CSV header data
     * @throws CsvParsingException If there is trouble parsing the headers
     */
    public CsvHeaders(final String[] aRow) throws CsvParsingException {
        for (int index = 0; index < aRow.length; index++) {
            switch (aRow[index]) {
                case CSV.TITLE:
                    setTitleIndex(index);
                    break;
                case CSV.PROJECT_NAME:
                    setProjectNameIndex(index);
                    break;
                case CSV.ITEM_ARK:
                    setItemArkIndex(index);
                    break;
                case CSV.PARENT_ARK:
                    setParentArkIndex(index);
                    break;
                case CSV.OBJECT_TYPE:
                    setObjectTypeIndex(index);
                    break;
                case CSV.FILE_NAME:
                    setFileNameIndex(index);
                    break;
                case CSV.ITEM_SEQ:
                    setItemSequence(index);
                    break;
                default:
                    // Our default is to ignore things we don't care about
            }
        }

        // Check to make sure we have the data components that we need to build a manifest
        if (!hasItemArkIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_113);
        } else if (!hasParentArkIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_114);
        } else if (!hasProjectNameIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_105);
        } else if (!hasObjectTypeIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_115);
        } else if (!hasTitleIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_111);
        } else if (!hasFileNameIndex()) {
            throw new CsvParsingException(MessageCodes.MFS_112);
        } else if (!hasItemSequence()) {
            throw new CsvParsingException(MessageCodes.MFS_123);
        }
    }

    /**
     * Gets the Item ARK index position.
     *
     * @return The Item ARK index position
     */
    public int getItemArkIndex() {
        return myItemArkIndex;
    }

    /**
     * Sets the Item ARK index position.
     *
     * @param aItemArkIndex The position of the Item ARK header.
     */
    public CsvHeaders setItemArkIndex(final int aItemArkIndex) {
        myItemArkIndex = aItemArkIndex;
        return this;
    }

    /**
     * Checks whether there is an Item ARK index position
     *
     * @return True if there is an Item ARK index position; else, false
     */
    public boolean hasItemArkIndex() {
        return myItemArkIndex != -1;
    }

    /**
     * Gets the Parent ARK index position.
     *
     * @return The Parent ARK index position
     */
    public int getParentArkIndex() {
        return myParentArkIndex;
    }

    /**
     * Sets the Parent ARK index position.
     *
     * @param aParentArkIndex The index position of the Parent ARK
     */
    public CsvHeaders setParentArkIndex(final int aParentArkIndex) {
        myParentArkIndex = aParentArkIndex;
        return this;
    }

    /**
     * Checks whether there is an Parent ARK index position
     *
     * @return True if there is an Parent ARK index position; else, false
     */
    public boolean hasParentArkIndex() {
        return myParentArkIndex != -1;
    }

    /**
     * Gets the Title index position.
     *
     * @return The Title index position
     */
    public int getTitleIndex() {
        return myTitleIndex;
    }

    /**
     * Sets the Title index position.
     *
     * @param aTitleIndex The index position of the Title
     */
    public CsvHeaders setTitleIndex(final int aTitleIndex) {
        myTitleIndex = aTitleIndex;
        return this;
    }

    /**
     * Checks whether there is a Title index position
     *
     * @return True if there is an Title index position; else, false
     */
    public boolean hasTitleIndex() {
        return myTitleIndex != -1;
    }

    /**
     * Gets the Project Name index position.
     *
     * @return The index position of the Project Name
     */
    public int getProjectNameIndex() {
        return myProjectNameIndex;
    }

    /**
     * Sets the Project Name index position.
     *
     * @param aProjectNameIndex The index position of the Project Name
     */
    public CsvHeaders setProjectNameIndex(final int aProjectNameIndex) {
        myProjectNameIndex = aProjectNameIndex;
        return this;
    }

    /**
     * Checks whether there is an Project Name index position
     *
     * @return True if there is a Project name index position; else, false
     */
    public boolean hasProjectNameIndex() {
        return myProjectNameIndex != -1;
    }

    /**
     * Gets the Object Type index position.
     *
     * @return The index position of the Object Type
     */
    public int getObjectTypeIndex() {
        return myObjectTypeIndex;
    }

    /**
     * Sets the Object Type index position.
     *
     * @param aObjectTypeIndex The index position of the Object Type
     */
    public CsvHeaders setObjectTypeIndex(final int aObjectTypeIndex) {
        myObjectTypeIndex = aObjectTypeIndex;
        return this;
    }

    /**
     * Checks whether there is an Object Type index position
     *
     * @return True if there is an Object Type index position; else, false
     */
    public boolean hasObjectTypeIndex() {
        return myObjectTypeIndex != -1;
    }

    /**
     * Gets the File Name index position.
     *
     * @return The index position of the File Name
     */
    public int getFileNameIndex() {
        return myFileNameIndex;
    }

    /**
     * Sets the File Name index position.
     *
     * @param aFileNameIndex The index position of the File Name
     */
    public CsvHeaders setFileNameIndex(final int aFileNameIndex) {
        myFileNameIndex = aFileNameIndex;
        return this;
    }

    /**
     * Checks whether there is a File Name index position
     *
     * @return True if there is a File Name index position; else, false
     */
    public boolean hasFileNameIndex() {
        return myFileNameIndex != -1;
    }

    /**
     * Sets the Item Sequence index position.
     *
     * @param aItemSequence
     * @return
     */
    public CsvHeaders setItemSequence(final int aItemSequence) {
        myItemSequence = aItemSequence;
        return this;
    }

    /**
     * Gets the Item Sequence header position.
     */
    public int getItemSequence() {
        return myItemSequence;
    }

    /**
     * Checks whether the CSV headers have a item sequence position registered.
     */
    public boolean hasItemSequence() {
        return myItemSequence != -1;
    }
}
