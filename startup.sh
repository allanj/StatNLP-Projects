#!/bin/bash

sed -i '$ a export _JAVA_OPTIONS=-Xmx244000m' ~/.bashrc

source ~/.bashrc

#check set memory
java -version

chmod +x data/semeval10t1/conlleval.pl

