#!/bin/bash

### Tunning the regularization parameter
# java -jar ner.jar -trainNum -1 -testNum -1 -iter 500 \
# 		-thread 38 -pipe false -reg 0.001  -model crf \
# 		-dataset conll2003 -data / -depf false -dev true > conll2003logs/lcrf-depFalse-dev-reg0001-conll2003.log 2>&1

# java -jar ner.jar -trainNum -1 -testNum -1 -iter 500 \
# 		-thread 38 -pipe false -reg 0.01  -model crf \
# 		-dataset conll2003 -data / -depf false -dev true > conll2003logs/lcrf-depFalse-dev-reg001-conll2003.log 2>&1

# java -jar ner.jar -trainNum -1 -testNum -1 -iter 500 \
# 		-thread 38 -pipe false -reg 0.1  -model crf \
# 		-dataset conll2003 -data / -depf false -dev true > conll2003logs/lcrf-depFalse-dev-reg01-conll2003.log 2>&1

# java -jar ner.jar -trainNum -1 -testNum -1 -iter 500 \
# 		-thread 38 -pipe false -reg 1  -model crf \
# 		-dataset conll2003 -data / -depf false -dev true > conll2003logs/lcrf-depFalse-dev-reg1-conll2003.log 2>&1

# java -jar ner.jar -trainNum -1 -testNum -1 -iter 500 \
# 		-thread 38 -pipe false -reg 0.0001  -model crf \
# 		-dataset conll2003 -data / -depf false -dev true > conll2003logs/lcrf-depFalse-dev-reg00001-conll2003.log 2>&1


#### Tuning the semi-markov CRFs model
java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.001 -ext semi \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-semi-depFalse-dev-reg0001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.01 -ext semi \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-semi-depFalse-dev-reg001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.1 -ext semi \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-semi-depFalse-dev-reg01-conll2003.log 2>&1

java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 1 -ext semi \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-semi-depFalse-dev-reg1-conll2003.log 2>&1	


###Tuning the DGM-s model
java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.001 -ext dgm-s \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgms-depFalse-dev-reg0001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.01 -ext dgm-s \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgms-depFalse-dev-reg001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.1 -ext dgm-s \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgms-depFalse-dev-reg01-conll2003.log 2>&1

java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 1 -ext dgm-s \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgms-depFalse-dev-reg1-conll2003.log 2>&1


###Tuning the DGM model
java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.001 -ext dgm \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgm-depFalse-dev-reg0001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.01 -ext dgm \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgm-depFalse-dev-reg001-conll2003.log 2>&1


java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 0.1 -ext dgm \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgm-depFalse-dev-reg01-conll2003.log 2>&1

java -jar seminer.jar -trainNum -1 -testNum -1 -iter 500 \
 			-thread 38 -model crf -depf false -reg 1 -ext dgm \
 			-ignore false -data / -dataset conll2003 -dev true > conll2003logs/semi-dgm-depFalse-dev-reg1-conll2003.log 2>&1				

 