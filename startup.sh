#!/bin/bash

## This is a pre script that before you run our program

## Better set the memory to maximum to avoid memory issue
sed -i '$ a export _JAVA_OPTIONS=-Xmx244000m' ~/.bashrc

## Make the above configuration work
source ~/.bashrc

### check set memory, optional
java -version

## This script is the evaluation script. Please make it executable since we will run this command.
chmod +x data/semeval10t1/conlleval.pl

