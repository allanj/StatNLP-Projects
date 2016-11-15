
<h1> StatNLP Projects
==================

This package contains the work implemented using the [**StatNLP**](http://statnlp.org/) framework and we will keep updating to make it more efficient. Most of the project implemented with the Conditional Random Fields (CRF) model in Java.



### Current projects in this package

- [Dependency-guided Named Entity Recognition](#dgm)
  - associated with the paper _Efficient Dependency-Guided Named Entity Recognition_, to be appeared in AAAI 2017
- Dependency Parsing
- Named Entity Recognition with Linear-chain CRFs and semi-Markov CRFs.
- Part-of-Speech Tagging and Chunking with Linear-chain CRFs
- Factorial CRFs (_ongoing_)




##### Quick Guide

To run the code in Eclipse, simply import the project (i.e. pom.xml) into your eclipse.




##### <a name="dgm" />Dependency-guided Named Entity Recognition

The model locates in package ```com.statnlp.projects.entity.semi```, you can run the DGM model directly from the ```SemiCRFMain``` class. There are some configuration you may want to set beforehand. 

| Arguments | Default  | Description                              |
| --------- | -------- | ---------------------------------------- |
| -trainNum | all data | the number of training data              |
| -testNum  | all      | the number of testing data               |
| -iter     | 100      | number of iterations                     |
| -thread   | 5        | number of threads to train               |
| -reg      | 0.01     | regularization parameter                 |
| -depf     | false    | using dependency features                |
| -dev      | false    | the data to test is development set or no |
| -ext      | semi     | Different model to use. ```semi``` means semi-CRFs. ```dgm-s```and ```dgm``` represent the other two models in the paper. |
|           |          |                                          |



trainNum: the number of training instance, ```