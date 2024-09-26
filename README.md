# PMD Rule Designer

[![Build Status](https://github.com/pmd/pmd-designer/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/pmd/pmd-designer/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/net.sourceforge.pmd/pmd-designer.svg)](https://maven-badges.herokuapp.com/maven-central/net.sourceforge.pmd/pmd-designer)
[![Join the chat](https://img.shields.io/gitter/room/pmd/pmd-designer)](https://app.gitter.im/#/room/#pmd_pmd-designer:gitter.im?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)



The Rule Designer is a graphical tool that helps PMD users develop their custom
rules. Main features:
* [AST inspection](https://docs.pmd-code.org/latest/pmd_userdocs_extending_designer_reference.html#ast-inspection): inspect AST structure for any language, including XPath attributes
* [XPath rule design](https://docs.pmd-code.org/latest/pmd_userdocs_extending_designer_reference.html#xpath-rule-design): integrated XPath editor, and rule metadata editor
* [Rule test edition](https://docs.pmd-code.org/latest/pmd_userdocs_extending_designer_reference.html#testing-a-rule): create or edit rule test files for the [PMD testing framework](https://docs.pmd-code.org/latest/pmd_userdocs_extending_testing.html)


![testing-designer](https://user-images.githubusercontent.com/24524930/61461094-504a7900-a970-11e9-822e-30cc121b568c.gif)


## Installation

The designer is part of PMD's binary distributions. To install a distribution, see the
[documentation page](https://docs.pmd-code.org/latest/pmd_userdocs_installation.html) about installing PMD.

The app needs either Oracle Java 8 (which includes JavaFX) or OpenJDK 11+ and a separately installed
OpenJFX distribution. Visit [JavaFX - Gluon](https://gluonhq.com/products/javafx/) to download an SDK distribution,
extract it, and set the `JAVAFX_HOME` environment variable.

If the `bin` directory of your PMD distribution is on your shell's path, then you can **launch the app** with
* `pmd designer` on Linux/ OSX
* `pmd.bat designer` on Windows

Alternatively, you can launch the program "from source" with Maven.
* `$ ./mvnw -Prunning exec:java` will launch the program after compiling it, using the JavaFX distribution of your system
* `$ ./mvnw -Prunning,with-javafx exec:java` will also add JavaFX dependencies on your classpath.
You can change the version of those dependencies with eg `-Dopenjfx.version=21.0.2` for OpenJFX 21.
See the list of available versions [here](https://search.maven.org/artifact/org.openjfx/javafx).

### Updating

The latest version of the designer currently **works with PMD 7.0.0 and above**.
You can simply replace `pmd-designer-7.X.Y.jar` with the [latest build](https://github.com/pmd/pmd-designer/releases/latest)
in the installation folder of your PMD distribution.

## Usage

### [Usage documentation is on the website](https://docs.pmd-code.org/latest/pmd_userdocs_extending_designer_reference.html)

## Building from source/ contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for instructions to build the project from source and setup your IDE.


##### Building a runnable JAR

You can package a runnable jar containing the PMD dependencies with maven. For
now the only option is to build a jar that contains pmd-core and pmd-java:

```
./mvnw clean package -Dfat-java -Dpmd.core.version=7.0.0-SNAPSHOT
```
The `pmd.core.version` property selects the version of pmd-core *and pmd-java*
that will be included. The built jar can then be found in your `target` directory.
**Such a jar cannot be used in a PMD distribution** and must be used in a
standalone fashion, otherwise classpath conflicts may arise.
You can additionally enable the profile `with-javafx` to include openjfx as well.

You should never run the `install` goal with the `-Dfat-java` property! This
would install the fat jar in your local repo and may cause dependency conflicts.

