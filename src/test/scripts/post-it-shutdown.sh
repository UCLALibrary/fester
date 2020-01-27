#! /bin/sh

# We have to provide a little wrapper script for exec-maven-plugin to background our job
kill `cat fester-it.pid`
rm fester-it.pid
