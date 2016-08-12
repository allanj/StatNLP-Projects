#!/bin/bash

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 500 -thread 36 -reg 0.1 -debug false -pipe false > log-dp-sem-reg01.txt 2>&1 

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 200 -thread 36 -reg 0.1 -debug false -data abc -pipe false -comb > log-dp-dev-abc-reg01 2>&1

###########
java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 500 -thread 36 -reg 0.1 -debug false -data cnn -pipe false -comb > log-dp-dev-cnn-reg01 2>&1

######################

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 200 -thread 36 -reg 0.1 -debug false -data mnb  -pipe false -comb > log-dp-dev-mnb-reg01 2>&1

#################################

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 200 -thread 36 -reg 0.1 -debug false -data nbc  -pipe false -comb > log-dp-dev-nbc-reg01 2>&1

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 400 -thread 36 -reg 0.1 -debug false -data pri  -pipe false -comb > log-dp-dev-pri-reg01 2>&1

java -jar dp.jar -trainNum -1 -testNum -1 -dev false -iter 400 -thread 36 -reg 0.1 -debug false -data voa  -pipe false -comb > log-dp-dev-voa-reg01 2>&1

