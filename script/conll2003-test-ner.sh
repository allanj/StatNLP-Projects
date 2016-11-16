#!/bin/bash

### Testing the linear-chain CRF
java -jar ner.jar -trainNum -1 -testNum -1 -iter 1000 \
		-thread 38 -pipe false -reg 0.1  -model crf \
		-dataset conll2003 -data / -depf false -dev false > conll2003logs/lcrf-depfFalse-test-conll2003.log 2>&1

# java -jar ner.jar -trainNum -1 -testNum -1 -iter 1000 \
# 		-thread 38 -pipe false -reg 0.1  -model crf \
# 		-dataset conll2003 -data / -depf true -dev false > conll2003logs/lcrf-depfTrue-test-conll2003.log 2>&1

# #### Testing the semi-markov CRFs model
# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf false -reg 0.1 -ext semi \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-semi-depfFalse-test-conll2003.log 2>&1


# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf true -reg 0.1 -ext semi \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-semi-depfTrue-test-conll2003.log 2>&1


# ###Testing the DGM-s model
# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf false -reg 0.1 -ext dgm-s \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-dgms-depfFalse-test-conll2003.log 2>&1


# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf true -reg 0.1 -ext dgm-s \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-dgms-depfTrue-test-conll2003.log 2>&1

# ###Testing the DGM model
# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf false -reg 0.1 -ext dgm \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-dgm-depfFalse-test-conll2003.log 2>&1


# java -jar seminer.jar -trainNum -1 -testNum -1 -iter 1000 \
#  			-thread 38 -model crf -depf true -reg 0.1 -ext dgm \
#  			-ignore false -data / -dataset conll2003 -dev false > conll2003logs/semi-dgm-depfTrue-test-conll2003.log 2>&1

