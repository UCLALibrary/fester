# Festerize

Script to upload CSV files to the Fester service.

## Using festerize.py

You need to have Python 3 installed to use Festerize. This README will describe the simplest way to get started; if you're more experienced and would like to use a virtualized Python environment, feel free to do that.

First, check the Fester project out of GitHub and change into the script's directory:

    git clone https://github.com/UCLALibrary/fester.git
    cd fester/src/main/scripts

Then you can install `festerize` and its dependencies.

### Installing on a Linux machine

To install on a Linux machine, type:

    ./setup.py install --user

If you want to install it in a place that's available to all users on a system (and you have sudo privileges) you can use:

    sudo ./setup.py

### Installing on a Mac

To install on a Mac that's installed Python 3 with Homebrew, type:

    python3 setup.py install

## Running Festerize

After festerize is installed, you can see the available options by running:

    festerize --help

When you do this, you should see the following:

```
Usage: festerize [OPTIONS] SRC...

  FESTERIZE uploads CSV files to the UCLA Library Fester service.

Options:
  --server TEXT    URL the Fester service we are using. Default:
                   https://iiif.library.ucla.edu
  --endpoint TEXT  Service endpoint to use. Default: /collections
  --out TEXT       Folder to store the results of festerizing. Default: output
  --iiifhost TEXT  IIIF host this collection uses. Optional, no default.
  --loglevel TEXT  Log level for Festerizer logs. Default: INFO, can also be
                   DEBUG or ERROR
  --help           Show this message and exit.
```

Note that the SRC argument above supports standard [filename globbing](https://en.wikipedia.org/wiki/Glob_(programming)) rules. In other words, `*.csv` is a valid entry for the SRC argument.

*There are limits* to how many arguments can be sent to a command. This depends on your OS and its configuration. See this [StackExchange](https://unix.stackexchange.com/questions/110282/cp-max-source-files-number-arguments-for-copy-utility) post for more information.

Festerize will ignore any files that do not end with `.csv` so a command of `festerize *.*` should be safe to run. Festerize does not recursively search folders.

Festerize creates a folder (by default called `./output`) for all output. CSVs returned by the Fester service are stored there, with the same name as the SRC file.

Festerize also creates a log file in the output folder, named the current date and time of the run, with an extension of `.log`. By default, the start and end time of the run are added as INFO rows to this log file, but this can be disabled by setting the `--loglevel` option to `--loglevel ERROR`.

## Contact

Feel free to use this project's [issues queue](https://github.com/uclalibrary/fester/issues) to ask questions, make suggestions, or provide other feedback.