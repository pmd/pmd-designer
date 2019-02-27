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

