#!/bin/bash




java -jar hp-3.0.jar -trainNum 500 -testNum -1 -dev false -iter 250 -thread 36 -reg 0.01 -debug false > log-hp-3.0-500-001.txt 2>&1

#java -jar hp-3.0.jar -trainNum 500 -testNum -1 -dev false -iter 250 -thread 35 -reg 0.001 -debug false > log-hp2d-1.0-500-0001.txt 2>&1

#java -jar hp-3.0.jar -trainNum 500 -testNum -1 -dev false -iter 250 -thread 35 -reg 0.1 -debug false > log-hp2d-1.0-500-01.txt 2>&1

