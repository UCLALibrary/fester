# Fester &nbsp;[![Build Status](https://api.travis-ci.com/uclalibrary/fester.svg?branch=master)](https://travis-ci.com/uclalibrary/fester) [![Known Vulnerabilities](https://snyk.io/test/github/uclalibrary/fester/badge.svg)](https://snyk.io/test/github/uclalibrary/fester)

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

To generate the site's documentation, run:

    mvn site

This will generate the documentation in the `target/site` directory.

## Configuring the Tests

The project contains unit, functional, and integration tests, with controls on how to control which tests are run. In order to run the functional and integration tests, the build machine must have a working Docker environment. Setting up Docker on your machine will depend on the type of machine you have (e.g., Linux, Mac, or Windows). Docker's [documentation](https://docs.docker.com/get-docker/) should be consulted on how to do this.

When running the build using the 'package' phase (as described above), only the unit tests are run. If you want to run all the possible tests, the project can be built with:

    mvn integration-test

or

    mvn install

This will run the functional, feature flag, and integration tests, in addition to the unit tests. If you want to skip a particular type of test but still run the 'install' phase, you can use one of the following arguments to your Maven command:

    -DskipUTs
    -DskipITs
    -DskipFTs
    -DskipFfTs

The first will skip the unit tests; the second will skip the integration tests; the third will skip the functional tests; and, the fourth will skip the feature flag tests. They can also be combined so that two types of tests are skipped. For instance, only the functional tests will be run if the following is typed:

    mvn install -DskipUTs -DskipITs

For what it's worth, the difference between the 'install' phase and the 'integration-test' phase is just that the install phase installs the built Jar file into your machine's local Maven repository.

When running the integration and functional tests, it may be desirable to turn on logging for the containers that run the tests. This can be useful in debugging test failures that happen within the container. To do this, supply one (or any) of the following arguments to your build:

    -DseeLogsFT
    -DseeLogsIT
    -DseeLogsFfT

This will tunnel the container's logs (including the application within the container's logs) to Maven's logging mechanism so that you will be able to see what's happening in the container as the tests are being run against it.

You might also want to adjust the logging level on the tests themselves. By default, the test loggers are configured to write DEBUG logs to a log file in the `target` directory and ERROR logs to standard out. To change the log level of the standard out logging, run Maven with the `logLevel` argument; for instance:

    mvn -DlogLevel=DEBUG test

If you want more fine-grained control over the logging, you can copy the `src/test/resources/logback-test.xml` file to the project's root directory and modify it. A `logback-test.xml` file in the project's home directory will be used instead of the standard one in `src/rest/resources` if it's available. That hypothetical file has also been added to the project's `.gitignore` so you don't need to worry about checking it into Git.

## Running the Application for Development

You can run a development instance of Fester by typing the following within the project root:

    mvn -Plive test

Once run, the service can be verified/accessed at [http://localhost:8888/fester/status](http://localhost:8888/fester/status). The API documentation can be accessed at [http://localhost:8888/fester/docs](http://localhost:8888/fester/docs)

## Debugging with Eclipse IDE

There are two ways to debug Fester:

- **Debugging the tests.** This enables the developer to step through both the test and application code as the test suite runs.
- **Debugging a running instance.** This enables the developer to step through the application code as they interact with the HTTP API.

The following setup instructions were tested with [Eclipse IDE](https://www.eclipse.org/eclipseide/) 4.14.0 (2019-12).

### Debugging the tests

From within Eclipse:

1. Create a new debug configuration
    - In the top-level menu, select *Run* > *Debug Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Remote Java Application*
        - Set *Name* to something like `Fester (JDWP server for containerized instances created by test suite)`
        - In the *Connect* tab:
            - Set *Project* to the Fester project directory
            - Set *Connection Type* to `Standard (Socket Listen)`
            - Set *Port* to `5556`
            - Set *Connection limit* to `16`
            - Check *Allow termination of remote VM* (optional)
2. Create another debug configuration *
    - In the top-level menu, select *Run* > *Debug Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Maven Build*
        - Set *Name* to something like `Fester (debug test suite)`
        - In the *Main* tab:
            - Set *Base directory* to the Fester project directory
            - Set *Goals* to `integration-test`
            - Set *Profiles* to `debug`
            - Set *User settings* to the path to a `settings.xml` that contains your AWS S3 credentials
3. Run the debug configuration created in Step 1 **
4. Run the debug configuration created in Step 2 **

_* As an alternative to step 2 (and 4), run the following from the command line (after completing steps 1 and 3):_

    mvn -Pdebug integration-test

_** If you're doing this for the first time, you may need to bring back the pop-up window where you created the configuration in order to invoke it. Otherwise, you can use toolbar buttons, or hotkeys <kbd>Ctrl</kbd> <kbd>F11</kbd> (Run) or <kbd>F11</kbd> (Debug)._

### Debugging a running instance

This procedure will start an instance of Fester with port `5555` open for incoming JDWP connections.

From within Eclipse:

1. Create a new run configuration ***
    - In the top-level menu, select *Run* > *Run Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Maven Build*
        - Set *Name* to something like `Fester (debugging mode)`
        - In the *Main* tab:
            - Set *Base directory* to the Fester project directory
            - Set *Goals* to `test`
            - Set *Profiles* to `runDebug`
            - Set *User settings* to the path to a `settings.xml` that contains your AWS S3 credentials
2. Create a new debug configuration
    - In the top-level menu, select *Run* > *Debug Configurations...*
    - In the pop-up window:
        - Create a new configuration of type *Remote Java Application*
        - Set *Name* to something like `Fester (JDWP client)`
        - In the *Connect* tab:
            - Set *Project* to the Fester project directory
            - Set *Connection Type* to `Standard (Socket Attach)`
            - Set *Host* to `localhost`
            - Set *Port* to `5555`
            - Check *Allow termination of remote VM* (optional)
3. Run the new run configuration created in Step 1
4. Run the new debug configuration created in Step 2

_*** As an alternative to step 1 (and 3), run the following from the command line:_

    mvn -PrunDebug test

_and then proceed with steps 2 and 4._

## Load Testing

A [Locust](https://docs.locust.io/en/stable/index.html) test file is included, it only tests PUTs of manifests. If you wish to run the test, you need to have Locust installed, and then run the following command from the src/test/scripts/locust folder:

    locust --host=url-of-the-server-you-are-testing

For example, if you wish to run a Locust test against a dev instance on your own machine, you would enter:

    locust --host=http://localhost:8888

## Git Hooks

To prevent accidentally pushing commits that would cause the Travis CI build to fail, you can configure your Git client to use a pre-push hook:

    ln -s ../../src/test/scripts/git-hooks/pre-push .git/hooks

## Releases

Releases follow semantic versioning, with the exception that we don't consider anything stable until the project reaches the 1.0.0 release.

To create a new release, update the `version` element in the POM file (if needed). The updated version should still end with `-SNAPSHOT`, just change the numeric designation.

After the version in the POM file is ready, the following script can be run:

    src/main/tools/travis/prepare_release

This will prepare the code for release by making two commits to Git. The first will be one for the version to be released (minus the snapshot designation), and the second will be one for a new snapshot version.

The actual release will be done by the Travis build. When a non-snapshot version is built by Travis, a Docker image will be uploaded to the Docker registry.

## Contact

We use an internal ticketing system, but we've left the GitHub [issues](https://github.com/UCLALibrary/fester/issues) open in case you'd like to file a ticket or make a suggestion. You can also contact Kevin S. Clarke at <a href="mailto:ksclarke@ksclarke.io">ksclarke@ksclarke.io</a> if you have a question about the project.
