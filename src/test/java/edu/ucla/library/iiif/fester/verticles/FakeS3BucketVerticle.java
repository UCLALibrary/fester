
package edu.ucla.library.iiif.fester.verticles;

import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import edu.ucla.library.iiif.fester.Constants;
import edu.ucla.library.iiif.fester.MessageCodes;
import edu.ucla.library.iiif.fester.Op;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

/**
 * A fake S3 bucket verticle that imitates the real S3BucketVerticle (without actually uploading anything).
 */
public class FakeS3BucketVerticle extends AbstractFesterVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeS3BucketVerticle.class, Constants.MESSAGES);

    @Override
    public void start(final Promise<Void> aPromise) throws Exception {
        LOGGER.debug(MessageCodes.MFS_110, getClass().getName(), deploymentID());

        vertx.eventBus().consumer("edu.ucla.library.iiif.fester.verticles.S3BucketVerticle").handler(message -> {
            // All messages passed to the S3BucketVerticle _should_ be JsonObjects
            LOGGER.debug(MessageCodes.MFS_124, ((JsonObject) message.body()).encode());
            message.reply(Op.SUCCESS);
        });

        aPromise.complete();
    }
}
