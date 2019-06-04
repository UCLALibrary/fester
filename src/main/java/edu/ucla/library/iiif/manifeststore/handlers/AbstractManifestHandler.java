
package edu.ucla.library.iiif.manifeststore.handlers;

import com.amazonaws.regions.RegionUtils;

import info.freelibrary.vertx.s3.S3Client;

import edu.ucla.library.iiif.manifeststore.Config;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

abstract class AbstractManifestHandler implements Handler<RoutingContext> {

    protected S3Client myS3Client;

    AbstractManifestHandler(final Vertx aVertx, final JsonObject aConfig) {
        if (myS3Client == null) {
            final String s3AccessKey = aConfig.getString(Config.S3_ACCESS_KEY);
            final String s3SecretKey = aConfig.getString(Config.S3_SECRET_KEY);
            final String s3RegionName = aConfig.getString(Config.S3_REGION);
            final String s3Region = RegionUtils.getRegion(s3RegionName).getServiceEndpoint("s3");

            myS3Client = new S3Client(aVertx, s3AccessKey, s3SecretKey, s3Region);
        }
    }

}
