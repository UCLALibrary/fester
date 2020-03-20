# Festerize

Uploads CSV files to the UCLA Library IIIF manifest service.

## Installation

You need to have Python 3 installed to use Festerize. This README will describe the simplest way to get started; if you're more experienced and would like to use a virtualized Python environment, feel free to do that.

First, check the Fester project out of GitHub and change into the script's directory:

```bash
$ git clone https://github.com/UCLALibrary/fester.git
$ cd fester/src/main/scripts
```

Then you can install `festerize` and its dependencies.

### Installing on a Linux machine

To install on a Linux machine, type:

```bash
$ ./setup.py install --user
```

If you want to install it in a place that's available to all users on a system (and you have sudo privileges) you can use:

```bash
$ sudo ./setup.py
```

### Installing on a Mac

To install on a Mac that's installed Python 3 with Homebrew, type:

```bash
$ python3 setup.py install
```

## Usage

After it's installed, you can see the available options by running:

```bash
$ festerize --help
```

Tips:

1. The SRC argument supports standard [filename globbing](https://en.wikipedia.org/wiki/Glob_(programming)) rules. In other words, `*.csv` is a valid entry for the SRC argument.
2. *There are limits* to how many arguments can be sent to a command. This depends on your OS and its configuration. See this [StackExchange](https://unix.stackexchange.com/questions/110282/cp-max-source-files-number-arguments-for-copy-utility) post for more information.
3. Festerize will ignore any files that do not end with `.csv`, so a command of `festerize *.*` should be safe to run. Festerize does not recursively search folders.
4. Festerize creates a folder (by default called `./output`) for all output. CSVs returned by the Fester service are stored there, with the same name as the SRC file.
5. Festerize also creates a log file in the output folder, named the current date and time of the run, with an extension of `.log`. By default, the start and end time of the run are added as INFO rows to this log file, but this can be disabled by setting the `--loglevel` option to `--loglevel ERROR`.

## Contact

Feel free to use this project's [issues queue](https://github.com/uclalibrary/fester/issues) to ask questions, make suggestions, or provide other feedback.