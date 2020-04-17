# Festerize

Uploads CSV files to the Fester IIIF manifest service.

## Installation

First, ensure that you have Bash, cURL, Python 3 and Pip installed on your system.

When that's done, run the following command in your shell to install the latest release of Festerize:

    bash <(curl -sSL \
      https://raw.githubusercontent.com/UCLALibrary/fester/master/src/main/scripts/festerize/install.sh)

## Usage

After it's installed, you can see the available options by running:

    festerize --help

When you do this, you should see the following:

```
Usage: festerize [OPTIONS] SRC...

  Uploads CSV files to the Fester IIIF manifest service.

  SRC is either a path to a CSV file or a Unix-style glob like '*.csv'.

Options:
  --server TEXT                  URL of the Fester IIIF manifest service
                                 [default: https://iiif.library.ucla.edu]
  --endpoint TEXT                API endpoint for CSV uploading  [default:
                                 /collections]
  --out TEXT                     local directory to put the updated CSV
                                 [default: output]
  --iiifhost TEXT                IIIF image server URL (optional)
  --loglevel [INFO|DEBUG|ERROR]  [default: INFO]
  --help                         Show this message and exit.
```

The SRC argument supports standard [filename globbing](https://en.wikipedia.org/wiki/Glob_(programming)) rules. In other words, `*.csv` is a valid entry for the SRC argument.

*There are limits* to how many arguments can be sent to a command. This depends on your OS and its configuration. See this [StackExchange](https://unix.stackexchange.com/questions/110282/cp-max-source-files-number-arguments-for-copy-utility) post for more information.

Festerize will ignore any files that do not end with `.csv`, so a command of `festerize *.*` should be safe to run. Festerize does not recursively search folders.

Festerize creates a folder (by default called `./output`) for all output. CSVs returned by the Fester service are stored there, with the same name as the SRC file.

Festerize also creates a log file in the output folder, named the current date and time of the run, with an extension of `.log`. By default, the start and end time of the run are added as INFO rows to this log file, but this can be disabled by setting the `--loglevel` option to `--loglevel ERROR`.

## Contact

Feel free to use this project's [issues queue](https://github.com/uclalibrary/fester/issues) to ask questions, make suggestions, or provide other feedback.