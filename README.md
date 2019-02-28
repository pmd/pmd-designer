# TODO before going public

* Review release procedure
    * Do we need a release script to select profiles?
    * How do we configure the maven release plugin and with which credentials?
* This repo is to be moved to the pmd org

## Differences from the current pmd-ui in the main repo

* Some IntelliJ config files are checked in VCS to ease installation
  * You're welcome to check in Eclipse config files as well
* The jar artifact is a shaded Jar:
    *  It doesn't include the pmd dependencies
    *  It relocates dependencies that are both depended-on by pmd-core and this
       module (apache)
    *  There are profiles for IDE maven import (m2e and IJ) to avoid having the
    language modules as provided. This is similar to what pmd-core does with the
    Jaxen shaded jar.
* The PMD ruleset specific to pmd-ui is in this repo (see config dir)
  * It was a pain to update build-tools when we add a new control with a
  specific naming convention

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

* Install the [File Watchers](https://plugins.jetbrains.com/plugin/7177-file-watchers)
  plugin to compile the Less files to CSS when you edit them. The CSS files are
  generated into an ignored resource directory so that the integrated
  SceneBuilder picks up on them.

TODO make Gifs?


#### Eclipse

TODO

