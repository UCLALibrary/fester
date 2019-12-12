#!/usr/bin/env python

import click
import requests
import os

@click.command()
@click.argument('src', required=True, nargs=-1)
@click.option('--server', default='https://iiif.library.ucla.edu',
              help='URL the Fester service we are using. Default: https://iiif.library.ucla.edu')
@click.option('--endpoint', default='/collections', help='Service endpoint to use. Default: /collections')
@click.option('--out', default='./output', help='Folder to store the results of festerizing. Default: ./output')
def cli(src, server, endpoint, out):
    """FESTERIZE uploads CSV files to the UCLA Library Fester service.
    """
    request_url = server + endpoint

    # if the output folder does not exist, create it
    if not os.path.exists(out):
        click.echo("Output directory (%s) not found, creating it." % (out))
        os.makedirs(out)
    else:
        # alert the user, ask if they want to continue?
        click.confirm("Output directory (%s) found, should we continue? YES might overwrite any existing output files." % (out), abort=True)

    # LET'S DO THIS!
    for fn in src:
        # only work for .csv files
        if fn.endswith('.csv'):
            # TODO: confirm this file exists and is readable

            click.echo("Uploading %s to %s" % (fn, request_url))

            # upload the file via a post
            files = {'file': (fn, open(fn, 'rb'), 'text/csv', {'Expires': '0'})}
            r = requests.post(request_url, files=files )

            if r.status_code == 201 :
                click.echo("  SUCCESS! (status code %s)" %(r.status_code))
                # For now, let's assume the response content is a binary file
                # and also assume that this file is a CSV, we will save it
                # in the out folder, with the same fn we sent
                out_file = click.open_file("%s/%s" %(out, fn), "wb")
                out_file.write(r.content)
            else:
                click.echo("  ERROR! (status code %s)" %(r.status_code))
                click.echo(r.text)

        # skip any files that do no have an extension of .csv
        else:
            click.echo("Skipping %s: not a CSV" % (fn))
