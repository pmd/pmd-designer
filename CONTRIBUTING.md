
## Contributing

* Bug reports and specific feature requests can be submitted on the
  [issue tracker](https://github.com/pmd/pmd-designer/issues).
* If you'd like to give usability feedback without a particular direction, or need some help using the app,
  please ask in our [Gitter room](https://app.gitter.im/#/room/#pmd_pmd:gitter.im).

### Code contributions

* PRs are welcome anytime

#### Clean build from source

* Make sure, you have Java 21+ installed. While we build with Java 21, the Designer still runs with Java 8.
* Clone the repository
* Run `./mvnw -Pshading clean verify`
* The artifact can then be found in the `target` directory 
(it won't include PMD dependencies)

#### IDE Setup

##### IntelliJ IDEA

1. Clone the repository
2. Open in IntelliJ
3. [Open IntelliJ's terminal](https://stackoverflow.com/a/28044371/6245827) and
   paste the following:
   ```shell
   # Ignore some config files with paths, that only are valid for your installation/setup
   git update-index --skip-worktree -- .idea/misc.xml pmd-designer.iml \
     .idea/runConfigurations/Designer__Java_25___Module_.xml \
     .idea/runConfigurations/Designer__Java_21___Module_.xml \
     .idea/runConfigurations/Designer__Java_17___Module_.xml \
     .idea/runConfigurations/Designer__Java_11___Module_.xml
   ./mvnw process-resources # Generate CSS resources
   ```
4. [Synchronize the directory contents](https://stackoverflow.com/a/4599243/6245827) to pick-up on the new CSS files
5. Invoke the [Reimport All Maven Projects Action](https://stackoverflow.com/a/29765077/6245827)

6. You can now run the designer with the existing Run Configurations:  
   There are two kinds of run configuration per Java version. JavaFX should be run by adding the JavaFX
   libraries on the *module-path* and not on the classpath. This is what the configurations that end with
   "Module" in their name do. However, this requires that JavaFX SDK is downloaded separately and the
   environment variable `JAVAFX_HOME` in the configuration is set up accordingly.  
   An easier way to start the Designer is to use the other configurations. That will add JavaFX to the
   classpath automatically. While this currently still works, this is not an officially supported way to
   run JavaFX, but is easier to get started. You'll see a warning in the console like
   "Unsupported JavaFX configuration: classes were loaded from 'unnamed module @7923f745'" which can
   be ignored.

7. Install the [File Watchers](https://plugins.jetbrains.com/plugin/7177-file-watchers)
plugin to compile the Less files to CSS when you edit them. Configuration is already
in your repo because it was cloned in step 1. The CSS files are generated into an
ignored resource directory so that the integrated SceneBuilder picks up on them.


##### Eclipse

1.  Clone the repository
2.  Open eclipse, Choose "File, Import, Existing Maven Projects"
3.  Right click on the project, select "Run as, Maven build...", enter `process-resources` as goal. Run it.
    This generates the CSS resources.
4.  Open class `net.sourceforge.pmd.util.fxdesigner.DesignerStarter`. Choose "Run as Java Application".

