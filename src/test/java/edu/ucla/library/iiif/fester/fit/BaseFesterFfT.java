
package edu.ucla.library.iiif.fester.fit;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.Features;
import io.vertx.ext.web.multipart.MultipartForm;

/**
 * A base class for feature flag tests.
 */
public class BaseFesterFfT extends BaseFesterFT {

    /* The human friendly name of the batch ingest feature */
    protected static final String BATCH_INGEST_FEATURE = Features.getDisplayName(Features.BATCH_INGEST);

    /* The path at which our CSV upload form can be found */
    protected static final String UPLOAD_FORM_PATH = "/fester/upload/csv";

    /* The upload form passed to the POST collections endpoint */
    protected static final MultipartForm CSV_UPLOAD_FORM = MultipartForm.create().textFileUpload(Constants.CSV_FILE,
            "capostcards.csv", "src/test/resources/csv/capostcards.csv", Constants.CSV_MEDIA_TYPE);

}
