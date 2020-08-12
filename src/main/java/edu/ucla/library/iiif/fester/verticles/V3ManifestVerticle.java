
package edu.ucla.library.iiif.fester.verticles;

import io.vertx.core.Promise;

/**
 * A verticle that updates pages on a version 3 presentation manifest.
 */
public class V3ManifestVerticle extends AbstractFesterVerticle {

    /**
     * Starts a verticle to update pages on a manifest.
     */
    @Override
    public void start(final Promise<Void> aPromise) {
        getJsonConsumer().handler(message -> {
            message.body();
        });

        aPromise.complete();
    }

}
