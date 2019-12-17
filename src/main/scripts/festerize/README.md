# festerize
Script to upload CSV files to the UCLA Library Fester service.

## Using festerize.py

First, make sure you have Python 3 available (we recommend using [pyenv](https://github.com/pyenv/pyenv) if you need to 
upgrade the version of python installed on your system)  and [install pipenv](https://pipenv.kennethreitz.org/en/latest/#install-pipenv-today). Then you can use pipenv to install the project's dependencies in a new virtual environment: 

```
pipenv install

# or, if you intend to make changes to the festerize.py script, you can run

pipenv install --editable .

```
> NOTE: if you're familiar with Python development and prefer not to use
> pipenv, that's fine, do whatever is comfortable to you. This README will assume
> that you are using pipenv.

To run commands inside the new virtual environment, you can either enter `pipenv shell` to enter the virtual environment, or you can prefix your commands with `pipenv run`.

You can then use the script to load one or more CSVs into Fester:

```
festerize --help
Usage: festerize [OPTIONS] SRC...

  FESTERIZE uploads CSV files to the UCLA Library Fester service.

Options:
  --server TEXT    URL the Fester service we are using. Default:
                   https://iiif.library.ucla.edu
  --endpoint TEXT  Service endpoint to use. Default: /collections
  --out TEXT       Folder to store the results of festerizing. Default: output
  --iiifhost TEXT  IIIF-host this collection uses. Optional, no default.
  --loglevel TEXT  Log level for Festerizer logs. Default: INFO, can also be
                   DEBUG or ERROR
  --help           Show this message and exit.
```

Note that the SRC argument above supports standard [filename globbing](https://en.wikipedia.org/wiki/Glob_(programming))
rules. In other words, `*.csv` is a valid entry for the SRC argument.

*There are limits* to how many arguments can be sent to a command. This depends
on your OS and its configuration. See this [StackExchange](https://unix.stackexchange.com/questions/110282/cp-max-source-files-number-arguments-for-copy-utility)
post for more information.

Festerize will ignore any files that do not end with `.csv` so
a command of `festerize *.*` should be safe to run. Festerize does not
recursively search folders.

Festerize creates folder (by default called `./output`) for all output. CSVs returned
by the Fester service are stored there, with the same name as the SRC file.

Festerize also creates a log file in the output folder, named the current date and
time of the run, with an extension of `.log`. By default, the start and end time
of the run are added as INFO rows to this log file, but this can be disabled
by setting the `--loglevel` option to `--loglevel ERROR`.
