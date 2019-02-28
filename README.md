# TODO before going public

* Review release procedure
    * How do we configure the maven release plugin and with which credentials?
* Should we introduce the 4-segment versioning system for pmd-ui before 7.0.0? It could
be confusing to users, and there’s probably not many releases left before 7.0.0 anyway
    * E.g. releasing pmd-ui:6.12.0.1 instead of 6.13.0 could be weird,
    especially so since that version is in fact compatible with pmd-core:6.11.0.
    pmd-ui:6.11.0.1 would be even weirder and would in fact be lower in version
    ranges than 6.12.0

* This repo is to be moved to the pmd org
  * @oowekyala I don't have permissions to create the repo

* Move [open issues](https://github.com/pmd/pmd/labels/in%3Aui)
  * @oowekyala I don't have permissions
  * Close the designer project on pmd/pmd

* Delete the pmd-ui directory from the main repo
  * Fix parent pom.xml
  * Document the change:
    * in CONTRIBUTING.md, README.md
    * in the issue template of pmd/pmd
    * on the mailing list?



## Differences from the current pmd-ui in the main repo

* Some IntelliJ config files are checked in VCS to ease installation
  * You're welcome to check in Eclipse config files as well
* The jar artifact is a shaded Jar:
    *  It doesn't include the pmd dependencies
    *  It relocates dependencies that are both depended-on by pmd-core and this
       module (apache)
    *  It's a multi-release jar. That's because ControlsFX has two incompatible
    versions to support JavaFX 8 and 9+. They're unpacked into versioned directories
    to make it possible to use those controls that aren't compatible with
    Java 9+ (e.g. BreadCrumbBar, RangeSlider).
    *  There are profiles for IDE maven import (m2e and IJ) to avoid having the
    language modules as provided. This is similar to what pmd-core does with the
    Jaxen shaded jar.
* The PMD ruleset specific to pmd-ui is in this repo (see config dir)
  * It was a pain to update build-tools when we add a new control with a
  specific naming convention

---------------
---------------

# PMD Rule Designer


The Rule Designer is a graphical tool that helps PMD users develop their custom
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

