# PMD Rule Designer

[![Build Status](https://travis-ci.com/pmd/pmd-designer.svg?branch=master)](https://travis-ci.com/pmd/pmd-designer) 
[![Maven Central](https://img.shields.io/maven-central/v/net.sourceforge.pmd/pmd-ui.svg)](https://maven-badges.herokuapp.com/maven-central/net.sourceforge.pmd/pmd-ui)
[![Join the chat at https://gitter.im/pmd/pmd-designer](https://badges.gitter.im/pmd/pmd-designer.svg)](https://gitter.im/pmd/pmd-designer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)



The Rule Designer is a graphical tool that helps PMD users develop their custom
rules. Main features:
* [AST inspection](https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html#ast-inspection): inspect AST structure for any language, including XPath attributes
* [XPath rule design](https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html#xpath-rule-design): integrated XPath editor, and rule metadata editor
* [Rule test edition](https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html#testing-a-rule): create or edit rule test files for the [PMD testing framework](https://pmd.github.io/latest/pmd_userdocs_extending_testing.html)


![testing-designer](https://user-images.githubusercontent.com/24524930/61461094-504a7900-a970-11e9-822e-30cc121b568c.gif)


## Installation

The designer is part of PMD's binary distributions. To install a distribution, see the [documentation page](https://pmd.github.io/latest/pmd_userdocs_installation.html) about installing PMD.

The app needs JRE 1.8 or above to run. Be aware that on JRE 11+, the JavaFX distribution should be installed separately. Visit [the download page](https://gluonhq.com/products/javafx/) to download a distribution, extract it, and set the `JAVAFX_HOME` environment variable.

If the `bin` directory of your PMD distribution is on your shell's path, then you can **launch the app** with
* `run.sh designer` on Linux/ OSX
* `designer.bat` on Windows

### Updating

The latest version of the designer currently **works with PMD 6.20.0 and above**.
You can simply replace `pmd-ui-6.X.Y.jar` with the [latest build](https://github.com/pmd/pmd-designer/releases/tag/6.16.0) in the installation folder of your
PMD distribution.

## Usage

### [Usage documentation is on the website](https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html)

## Building from source/ contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for instructions to build the project from source and setup your IDE.


##### Building a runnable JAR

You can package a runnable jar containing the PMD dependencies with maven. For
now the only option is to build a jar that contains pmd-core and pmd-java:

```
mvn clean package -Dfat-java -Dpmd.core.version=7.0.0-SNAPSHOT
```
The `pmd.core.version` property selects the version of pmd-core *and pmd-java*
that will be included. The built jar can then be found in your `target` directory.
**Such a jar cannot be used in a PMD distribution** and must be used in a
standalone fashion, otherwise classpath conflicts may arise.

You should never run the `install` goal with the `-Dfat-java` property! This
would install the fat jar in your local repo and may cause dependency conflicts.

