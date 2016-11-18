#!/bin/bash

### Testing the linear-chain CRF
java -jar dep.jar -trainNum -1 -testNum -1 -iter 2000 -thread 40 \
 	 -dev false -las false -data abc > logs/dep_ABC.log 2>&1
