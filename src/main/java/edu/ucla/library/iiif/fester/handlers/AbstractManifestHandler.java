
package edu.ucla.library.iiif.fester.handlers;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;

import info.freelibrary.util.Logger;
import info.freelibrary.vertx.s3.S3Client;

import edu.ucla.library.iiif.fester.Config;
import edu.ucla.library.iiif.fester.MessageCodes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Creates an abstract handler so that other instantiated handlers can use its S3Client.
 */
abstract class AbstractManifestHandler implements Handler<RoutingContext> {

    protected S3Client myS3Client;

    protected String myS3Bucket;

    /**
     * An abstract handler that initializes an S3 client.
     *
     * @param aVertx A Vert.x instance
     * @param aConfig An application configuration
     */
    AbstractManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        if (myS3Client == null) {
            final String s3AccessKey = aConfig.getString(Config.S3_ACCESS_KEY);
            final String s3SecretKey = aConfig.getString(Config.S3_SECRET_KEY);
            final String s3RegionName = aConfig.getString(Config.S3_REGION);
            final Region s3Region = RegionUtils.getRegion(s3RegionName);

            getLogger().debug(MessageCodes.MFS_003, s3RegionName);

            if (s3Region != null) {
                myS3Client = new S3Client(aVertx, s3AccessKey, s3SecretKey, s3Region.getServiceEndpoint("s3"));
            } else {
                myS3Client = new S3Client(aVertx, s3AccessKey, s3SecretKey);
            }

            myS3Bucket = aConfig.getString(Config.S3_BUCKET);
        }
    }

    protected abstract Logger getLogger();

}