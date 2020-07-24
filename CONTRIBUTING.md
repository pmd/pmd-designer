
## Contributing

* Bug reports and specific feature requests can be submitted on the [issue tracker](https://github.com/pmd/pmd-designer/issues).
* If you'd like to give usability feedback without a particular direction, or need some help using the app, please start a chat on the [Gitter channel](https://gitter.im/pmd/pmd-designer)

### Code contributions

* PRs are welcome anytime

#### Clean build from source

* Clone the repository
* Run `./mvnw -Pshading clean verify`
* The artifact can then be found in the `target` directory 
(it won't include PMD dependencies)

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

1.  Clone the repository
2.  Open eclipse, Choose "File, Import, Existing Maven Projects"
3.  Right click on the project, select "Run as, Maven build...", enter `process-resources` as goal. Run it.
    This generates the CSS resources.
4.  Open class `net.sourceforge.pmd.util.fxdesigner.DesignerStarter`. Choose "Run as Java Application".

