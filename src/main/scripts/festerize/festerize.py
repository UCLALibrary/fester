#!/usr/bin/env python

import click
import requests
import os
import logging
from datetime import datetime
import sys
import pathlib

@click.command()
@click.argument('src', required=True, nargs=-1)
@click.option('--server', default='https://iiif.library.ucla.edu',
              help='URL the Fester service we are using. Default: https://iiif.library.ucla.edu')
@click.option('--endpoint', default='/collections', help='Service endpoint to use. Default: /collections')
@click.option('--out', default='output', help='Folder to store the results of festerizing. Default: output')
@click.option('--iiifhost', default='undefined', help="IIIF-host this collection uses. Optional, no default.", )
@click.option('--loglevel', default='INFO', help='Log level for Festerizer logs. Default: INFO, can also be DEBUG or ERROR')
def cli(src, server, endpoint, out, iiifhost, loglevel):
    """FESTERIZE uploads CSV files to the UCLA Library Fester service.
    """
    request_url = server + endpoint
    status_url = server + "/fester/status"

    # get ready to log some stuff
    started = datetime.now()
    right_now = started.strftime("%Y-%m-%d--%H-%M-%S")
    logfile_name = "%s.log" % (right_now)
    logfile_path = "%s%s%s" % (out, os.sep, logfile_name)

    # if the output folder does not exist, create it
    if not os.path.exists(out):
        click.echo("Output directory (%s) not found, creating it." % (out))
        os.makedirs(out)
    else:
        # alert the user, ask if they want to continue?
        click.confirm("Output directory (%s) found, should we continue? YES might overwrite any existing output files." % (out), abort=True)

    festerizer_loglevel = loglevel

    # start a log file in the out folder
    logging.basicConfig(filename=logfile_path, filemode='w', level=festerizer_loglevel, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    # log our start time
    logging.info("FESTERIZER STARTED at %s..." % (started.strftime("%Y-%m-%d %H:%M:%S")))

    # If Fester is unavailable, abort
    try:
        s = requests.get(status_url)
    except requests.exceptions.RequestException as e:
        logging.error("FESTERIZER service status unavailable at %s" % (status_url))
        click.echo(e)
        sys.exit("FESTERIZER service status unavailable, aborting.")

    if s.status_code != 200:
        logging.error("FESTERIZER service status unusable at %s" % (status_url))
        sys.exit("FESTERIZER service status unusable, aborting.")

    # LET'S DO THIS!
    for fp in src:

        # derive a filename (fn) from provided filepath (fp)
        filePath = pathlib.Path(fp)
        fn = filePath.name

        if not filePath.exists():
            click.echo("Skipping %s: file does not exist" % (fn))
            logging.error("%s does not exist, skipping" % (fn))

        # only work for .csv files
        elif fn.endswith('.csv'):

            click.echo("Uploading %s to %s" % (fn, request_url))

            # upload the file via a post
            files = {'file': (fp, open(fp, 'rb'), 'text/csv', {'Expires': '0'})}

            # handle the iiifhost option
            if iiifhost == 'undefined':
                r = requests.post(request_url, files=files )
            else:
                payload = [('iiif-host', iiifhost)]
                r = requests.post(request_url, files=files, data=payload)

            if r.status_code == 201 :
                click.echo("  SUCCESS! (status code %s)" % (r.status_code))
                # For now, let's assume the response content is a binary file
                # and also assume that this file is a CSV, we will save it
                # in the out folder, with the same fn we sent
                out_file = click.open_file("%s%s%s" %(out, os.sep, fn), "wb")
                out_file.write(r.content)
            else:
                click.echo("  ERROR! (status code %s)" %(r.status_code))
                logging.error("%s failed to load, status code %s" % (fn, r.status_code))
                logging.error(r.text)
                logging.error('--------------------------------------------')

        # skip any files that do no have an extension of .csv
        else:
            click.echo("Skipping %s: not a CSV" % (fn))
            logging.error("%s is not a CSV, skipping" % (fn))

# log our end time
    ended = datetime.now()
    logging.info("FESTERIZER ENDED at %s..." % (ended.strftime("%Y-%m-%d %H:%M:%S")))
