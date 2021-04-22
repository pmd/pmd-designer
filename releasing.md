# Releasing pmd-designer

1.  Checkout master branch:
    
    ``` shell
    git clone https://github.com/pmd/pmd-designer.git
    cd pmd-designer
    ```

2.  Verify changelog `/CHANGELOG.md`. Adjust version number if necessary.
    
    Note: The first section will be used for the release notes on github releases.

3.  Prepare the release (creates a new release tag).
    This will be done for you: http://maven.apache.org/plugins/maven-release-plugin/examples/prepare-release.html
    Maven will ask you about the release version, the tag name and the new version. You can simply hit enter,
    to use the default values.
    
    Note: the tag will be pushed automatically.
    
    ``` shell
    git switch master
    ./mvnw release:clean
    ./mvnw release:prepare
    ```

4.  Wait, until release is ready. The maven plugin will directly push the tag. The tag will be
    built by [github actions](https://github.com/pmd/pmd-designer/actions).
    After it is done, the new release should be available under <https://repo.maven.apache.org/maven2/net/sourceforge/pmd/pmd-ui/>.

5.  Verify the release on Github Releases: <https://github.com/pmd/pmd-designer/releases>
    
    The release notes from `/CHANGELOG.md` should be there. Also, the shaded pmd-ui-*.jar should
    have been uploaded.

6.  Add a new section at the top in `/CHANGELOG.md` to prepare for the next version.
