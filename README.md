# PMD Rule Designer

[![Build Status](https://travis-ci.com/pmd/pmd-designer.svg?branch=master)](https://travis-ci.com/pmd/pmd-designer) [![Join the chat at https://gitter.im/pmd/pmd-designer](https://badges.gitter.im/pmd/pmd-designer.svg)](https://gitter.im/pmd/pmd-designer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

WIP: the designer is being moved from [pmd/pmd/pmd-ui](https://github.com/pmd/pmd/tree/master/pmd-ui) to this repository. 

## TODOs

* Review release procedure: short howto (locally mvn release:prepare, travis will deploy the tag)

* Move [open issues](https://github.com/pmd/pmd/labels/in%3Aui)
  * Close the designer project on pmd/pmd

* Delete the pmd-ui directory from the main repo
  * Basically just https://github.com/oowekyala/pmd/commit/cc44bac3c3b8e0e680f8dd6c9da2898c2e39b7d9
  * Document the change:
    * in CONTRIBUTING.md, README.md
    * in the issue template of pmd/pmd
    * on the mailing list?
    * leave a pmd-ui/README.md behind, which says: The designer lives now at pmd/pmd-designer

* Update release_procedure/do_release.sh
  * Before releasing PMD, we need to check and update the pmd-ui/designer
    dependency to the latest release, so that the latest version is included
    in the binary distribution.


## Differences from the current pmd-ui in the main repo

* Some IntelliJ config files are checked in VCS to ease installation
  * You're welcome to check in Eclipse config files as well
* The jar artifact is a shaded Jar:
    *  It doesn't include the pmd dependencies
    *  It relocates dependencies that are both depended-on by pmd-core and this
       module (apache)
    *  It's a multi-release jar. That's because ControlsFX has two incompatible
    versions to support JavaFX 8 and 9+. More is explained in comments in the
    POM.
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

TODO describe minimum Java config

## Usage

TODO put usage doc on the main website


## Contributing

TODO describe packaging procedure, branching model, versioning system

### IDE Setup

#### IntelliJ IDEA

1. Clone the repository
1. Open in IntelliJ
1. [Open IntelliJ's terminal](https://stackoverflow.com/a/28044371/6245827) and
paste the following:
```shell
git update-index --skip-worktree -- .idea/misc.xml pmd-ui.iml # Ignore some config files
mvn process-resources # Generate CSS resources
```

4. [Synchronize the directory contents](https://stackoverflow.com/a/4599243/6245827) to pick-up on the new CSS files
1. Invoke the [Reimport All Maven Projects Action](https://stackoverflow.com/a/29765077/6245827)
1. You can now run the designer with the existing Run Configurations

1. Install the [File Watchers](https://plugins.jetbrains.com/plugin/7177-file-watchers)
plugin to compile the Less files to CSS when you edit them. Configuration is already
in your repo because it was cloned in step 1. The CSS files are generated into an
ignored resource directory so that the integrated SceneBuilder picks up on them.


#### Eclipse

TODO
