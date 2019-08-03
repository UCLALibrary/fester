# Manifest Store &nbsp;[![Build Status](https://api.travis-ci.com/uclalibrary/manifest-store.svg?branch=master)](https://travis-ci.com/uclalibrary/manifest-store) [![Known Vulnerabilities](https://img.shields.io/snyk/vulnerabilities/github/uclalibrary/manifest-store.svg)](https://snyk.io/test/github/uclalibrary/manifest-store)

A IIIF manifest storage microservice. It will provide full CRUD access to a collection of IIIF manifest files. This is a work in progress.

## Configuring the Build

The IIIF manifest store uses an S3 bucket for back-end storage. To be able to run the project's tests, several configuration values must be supplied:

* manifeststore.s3.bucket
* manifeststore.s3.access_key
* manifeststore.s3.secret_key
* manifeststore.s3.region

These values can be set as properties in your system's Maven settings.xml file (or be supplied on the command line at build time).

## Building the Project

The project builds an executable Jar that can be run to start the microservice. To build the project, run:

    mvn package

This will put the executable Jar in the `target/build-artifact` directory.

The application, in its simplest form, can be run with the following command:

    java -jar target/build-artifact/manifest-store-*.jar

To generate the site's documentation, run:

    mvn site

This will generate the documentation in the `target/site` directory.

## Running the Application for Development

You can run a development instance of Manifest-Store by typing the following within the project root:

    mvn -Plive test

Once run, the service can be verified/accessed at [http://localhost:8888/ping](http://localhost:8888/ping). The API documentation can be accessed at [http://localhost:8888/docs/manifest-store](http://localhost:8888/docs/manifest-store)

## Contact

We use an internal ticketing system, but we've left the GitHub [issues](https://github.com/UCLALibrary/manifest-store/issues) open in case you'd like to file a ticket or make a suggestion. You can also contact Kevin S. Clarke at <a href="mailto:ksclarke@ksclarke.io">ksclarke@ksclarke.io</a> if you have a question about the project.
