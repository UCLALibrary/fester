# Fester &nbsp;[![Build Status](https://api.travis-ci.com/uclalibrary/fester.svg?branch=master)](https://travis-ci.com/uclalibrary/fester) [![Known Vulnerabilities](https://snyk.io/test/github/UCLALibrary/fester/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/UCLALibrary/fester?targetFile=pom.xml)

A IIIF manifest storage microservice. It will provide almost-full CRUD access (currently there is no Update) to a collection of IIIF manifest files. This is a work in progress.

## Configuring the Build

Fester uses an S3 bucket for back-end storage. To be able to run the project's tests, several configuration values must be supplied:

* fester.s3.bucket
* fester.s3.access_key
* fester.s3.secret_key
* fester.s3.region

These values can be set as properties in your system's Maven settings.xml file (or be supplied on the command line at build time).

## Building the Project

The project builds an executable Jar that can be run to start the microservice. To build the project, run:

    mvn package

This will put the executable Jar in the `target/build-artifact` directory.

The application, in its simplest form, can be run with the following command:

    java -jar target/build-artifact/fester-*.jar

To generate the site's documentation, run:

    mvn site

This will generate the documentation in the `target/site` directory.

## Configuring the Tests

The project contains unit, functional, and integration tests, with controls on how to control which tests are run. In order to run the functional and integration tests, the build machine must have a working Docker environment. Setting up Docker on your machine will depend on the type of machine you have (e.g., Linux, Mac, or Windows). Docker's [documentation](https://docs.docker.com/get-docker/) should be consulted on how to do this.

When running the build using the 'package' phase (as described above), only the unit tests are run. If you want to run all the possible tests, the project can be built with:

    mvn integration-test

or

    mvn install

This will run the functional and integration tests, in addition to the unit tests. If you want to skip a particular type of test but still run the 'install' phase, you can use one of the following arguments to your Maven command:

    -DskipUTs
    -DskipITs
    -DskipFTs

The first will skip the unit tests; the second will skip the integration tests; and, the third will skip the functional tests. They can also be combined so that two types of tests are skipped. For instance, only the functional tests will be run if the following is typed:

    mvn install -DskipUTs -DskipITs

For what it's worth, the difference between the 'install' phase and the 'integration-test' phase is just that the install phase installs the built Jar file into your machine's local Maven repository.

When running the integration and functional tests, it may be desirable to turn on logging for the containers that run the tests. This can be useful in debugging test failures that happen within the container. To do this, supply one (or both) of the following arguments to your build:

    -DseeLogsFT
    -DseeLogsIT

This will tunnel the container's logs (including the application within the container's logs) to Maven's logging mechanism so that you will be able to see what's happening in the container as the tests are being run against it.

## Running the Application for Development

You can run a development instance of Fester by typing the following within the project root:

    mvn -Plive test

Once run, the service can be verified/accessed at [http://localhost:8888/status/fester](http://localhost:8888/status/fester). The API documentation can be accessed at [http://localhost:8888/docs/fester](http://localhost:8888/docs/fester)

## Debugging with Eclipse IDE

You can run a development instance of Fester with debugging turned on by typing the following within the project root:

    mvn -Pdebug test
Development instances are configured to accept remote debugger connections on port `5555`.

To debug Fester with [Eclipse IDE](https://www.eclipse.org/eclipseide/):

1. Create a new run configuration
    - In the top-level menu, select *Run* > *Run Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Maven Build*
        - Set *Name* to something like `Fester (development mode)`
        - In the *Main* tab:
            - Set *Base directory* to the Fester project directory
            - Set *Goals* to `test`
            - Set *Profiles* to `live`
            - Set *User settings* to the path to a `settings.xml` that contains your AWS S3 credentials
2. Create a new debug configuration
    - In the top-level menu, select *Run* > *Debug Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Remote Java Application*
        - Set *Name* to something like `Fester (socket attach)`
        - In the *Connect* tab:
            - Set *Project* to the Fester project directory
            - Set *Connection Type* to `Standard (Socket Attach)`
            - Set *Host* to `localhost`
            - Set *Port* to `5555`
            - Check *Allow termination of remote VM* (optional)
3. Run the new run configuration created in Step 1 *
4. Run the new debug configuration created in Step 2 *

_* If you're doing this for the first time, you may need to bring back the pop-up window where you created the configuration in order to invoke it. Otherwise, you can use toolbar buttons, or hotkeys <kbd>Ctrl</kbd> <kbd>F11</kbd> (Run) or <kbd>F11</kbd> (Debug)._

Tested with Eclipse IDE 4.14.0 (2019-12).

## Load Testing

A [Locust](https://docs.locust.io/en/stable/index.html) test file is included, it only tests PUTs of manifests. If you wish to run the test, you need to have Locust installed, and then run the following command from the src/test/scripts/locust folder:

    locust --host=url-of-the-server-you-are-testing

For example, if you wish to run a Locust test against a dev instance on your own machine, you would enter:

    locust --host=http://localhost:8888

## Git Hooks

To prevent accidentally pushing commits that would cause the Travis CI build to fail, you can configure your Git client to use a pre-push hook:

    ln -s ../../src/test/scripts/git-hooks/pre-push .git/hooks

## Contact

We use an internal ticketing system, but we've left the GitHub [issues](https://github.com/UCLALibrary/fester/issues) open in case you'd like to file a ticket or make a suggestion. You can also contact Kevin S. Clarke at <a href="mailto:ksclarke@ksclarke.io">ksclarke@ksclarke.io</a> if you have a question about the project.
