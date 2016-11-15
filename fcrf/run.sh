#!/bin/bash

java -jar fcrf.jar -trainNum -1 -testNum -1 -iter 4000\
         -thread 64 -reg 0.01 -npchunking true -neural false\
         -task joint -iobes true -optim adam 0.01 -mfround 4 > logs/FCRF_JOINT_MF.log 2>&1





