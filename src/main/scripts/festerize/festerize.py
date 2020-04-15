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
@click.option('--server', default='https://iiif.library.ucla.edu', show_default=True,
              help='URL of the Fester IIIF manifest service')
@click.option('--endpoint', default='/collections', show_default=True, help='API endpoint for CSV uploading')
@click.option('--out', default='output', show_default=True, help='local directory to put the updated CSV')
@click.option('--iiifhost', default=None, help='IIIF image server URL (optional)', )
@click.option('--loglevel', type=click.Choice(['INFO', 'DEBUG', 'ERROR']), default='INFO', show_default=True)
def cli(src, server, endpoint, out, iiifhost, loglevel):
    """Uploads CSV files to the Fester IIIF manifest service.

    SRC is either a path to a CSV file or a Unix-style glob like '*.csv'.
    """
    if not os.path.exists(out):
        click.echo('Output directory {} not found, creating it.'.format(out))
        os.makedirs(out)
    else:
        click.confirm('Output directory {} found, should we continue? YES might overwrite any existing output files.'.format(out), abort=True)

    # Logging setup.
    started = datetime.now()
    logfile_path = os.path.join(out, '{}.log'.format(started.strftime('%Y-%m-%d--%H-%M-%S')))
    logging.basicConfig(filename=logfile_path, filemode='w', level=loglevel, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    logging.info('STARTING at {}...'.format(started.strftime('%Y-%m-%d %H:%M:%S')))

    # If Fester is unavailable, abort.
    try:
        s = requests.get(server + '/fester/status')
        s.raise_for_status()
    except requests.exceptions.RequestException as e:
        error_msg = 'Fester IIIF manifest service unavailable: {}'.format(str(e))
        click.echo(error_msg)
        logging.error(error_msg)
        sys.exit(1)

    request_url = server + endpoint

    for pathstring in src:
        csv_filepath = pathlib.Path(pathstring)
        csv_filename = csv_filepath.name

        if not csv_filepath.exists():
            error_msg = 'File {} does not exist, skipping'.format(csv_filename)
            click.echo(error_msg)
            logging.error(error_msg)

        # Only works with CSV files that have the proper extension.
        elif csv_filepath.suffix == '.csv':
            click.echo('Uploading {} to {}'.format(csv_filename, request_url))

            # Upload the file.
            files = {'file': (pathstring, open(pathstring, 'rb'), 'text/csv', {'Expires': '0'})}
            payload = None
            if iiifhost is not None:
                payload = [('iiif-host', iiifhost)]
            r = requests.post(request_url, files=files, data=payload)

            # Handle the response.
            if r.status_code == 201 :
                click.echo('Uploaded {}'.format(csv_filename))

                # Save it in the out directory with the same filename.
                out_file = click.open_file(os.path.join(out, csv_filename), 'wb')
                out_file.write(r.content)
            else:
                error_msg = 'Failed to upload {}: {}'.format(csv_filename, r.status_code)
                click.echo(error_msg)
                logging.error(error_msg)
                logging.error(r.text)
                logging.error('--------------------------------------------')
        else:
            error_msg = 'File {} is not a CSV, skipping'.format(csv_filename)
            click.echo(error_msg)
            logging.error(error_msg)

    logging.info('DONE at {}.'.format(datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
