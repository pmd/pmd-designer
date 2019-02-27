# TODO before going public

* Review release procedure
    * Do we need a release script to select profiles?
    * How do we configure the maven release plugin and with which credentials?
* This repo is to be moved to the pmd org

## Differences from the current pmd-ui in the main repo

* Some IntelliJ config files are checked in VCS to ease installation
* The jar artifact is a shaded Jar:
    *  It doesn't ship the pmd dependencies
    *  It relocates dependencies that are both depended-on by pmd-core and this
       module (apache)
    *  It must be built with profile `-PuberJar` otherwise pmd dependencies are
       included (this is not very cool but I couldn't get it to work in another way). 
       Would you know another way?

Maybe it would be better to package
* a fat jar with pmd dependencies as the main jar artifact
* and another artifact with no pmd dependencies and a special classifier, that
  would be depended-on by pmd-dist
  
Wdyt?




---------------
---------------

# PMD Rule Designer


The Rule Designer is a program that helps PMD users develop their custom PMD
rules.

TODO Gifs



## Installation

The designer is part of PMD's binary distributions.

TODO release a fat jar containing PMD too using classifiers?

TODO describe minimum Java config

## Usage

TODO put usage doc on the main website


## Contributing

TODO describe packaging procedure, branching model, versioning system

### IDE Setup

#### IntelliJ IDEA

* Clone the repository
* Open in IntelliJ
* Open IntelliJ's terminal and paste the following:
```shell

git update-index --skip-worktree -- .idea/misc.xml pmd-ui.iml # Ignore some config files
mvn process-resources # Generate CSS resources
```
* Invoke the Reimport All Maven Projects Action
* You can now run the designer with the provided Run Configurations

TODO make Gifs?


#### Eclipse

TODO

