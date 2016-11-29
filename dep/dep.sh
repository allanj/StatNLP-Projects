#!/bin/bash

### Testing the dependency CRF with basic features without/with entity features
#java -Djava.library.path=/usr/local/lib -jar dep.jar -trainNum 800  -testNum -1 -iter 3000 -thread 40 \
# 	 -dev false -las false -data semeval10t1 -neural false -basicf true > logs/dep_semeval10t1.log 2>&1


#java -Djava.library.path=/usr/local/lib -jar dep.jar -trainNum 800  -testNum -1 -iter 3000 -thread 40 \
# 	 -dev false -las false -data semeval10t1 -neural false -basicf true -entityf true  > logs/dep_semeval10t1_entityf.log 2>&1


#java -Djava.library.path=/usr/local/lib -jar dep.jar -trainNum 800  -testNum -1 -iter 3000 -thread 40 \
# 	 -dev false -las false -data semeval10t1 -neural false -basicf true -entityf true  > logs/dep_semeval10t1_refined_entityf.log 2>&1

#java -Djava.library.path=/usr/local/lib -jar dep.jar -trainNum -1  -testNum -1 -iter 3000 -thread 40 \
# 	 -dev false -las false -data allanprocess voa -neural false -basicf true > logs/dep_voa.log 2>&1


#java -Djava.library.path=/usr/local/lib -jar dep.jar -trainNum -1  -testNum -1 -iter 3000 -thread 40 \
# 	 -dev false -las false -data allanprocess voa -neural false -basicf true -entityf true > logs/dep_voa_entityf.log 2>&1


datasets=(abc cnn mnb nbc p25 pri voa)

for i in "${datasets[@]}"
do
   java -jar segdep.jar -trainNum -1 -testNum -1 -iter 3000 -thread 63 -reg 0.1 \
       -dev false -data $i -lenone true > logs/segdep_${i}_lenone.log 2>&1
    
   java -jar segdep.jar -trainNum -1 -testNum -1 -iter 3000 -thread 63 -reg 0.1 \
       -dev false -data $i -lenone false > logs/segdep_${i}.log 2>&1
done


