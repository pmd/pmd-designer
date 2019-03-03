# PMD Rule Designer

[![Build Status](https://travis-ci.com/pmd/pmd-designer.svg?branch=master)](https://travis-ci.com/pmd/pmd-designer)

WIP: the designer is being moved from [pmd/pmd/pmd-ui](https://github.com/pmd/pmd/tree/master/pmd-ui) to this repository. 

## TODOs

* Review release procedure: short howto (locally mvn release:prepare, travis will deploy the tag)

* Move [open issues](https://github.com/pmd/pmd/labels/in%3Aui)
  * Close the designer project on pmd/pmd

* Delete the pmd-ui directory from the main repo (PR pending)

* Update release_procedure/do_release.sh
  * Before releasing PMD, we need to check and update the pmd-ui/designer
    dependency to the latest release, so that the latest version is included
    in the binary distribution.

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

* Bug reports and specific feature requests can be submitted on the [issue tracker](https://github.com/pmd/pmd-designer/issues).
* If you'd like to give usability feedback without a particular direction, or need some help using the app, please start a chat on the [Gitter channel](https://gitter.im/pmd/pmd-designer)

### Code contributions

* PRs are welcome anytime

#### Clean build from source

* Clone the repository
* Run `./mvnw clean verify`
* The artifact can then be found in the `target` directory

#### IDE Setup

##### IntelliJ IDEA

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


##### Eclipse

TODO
