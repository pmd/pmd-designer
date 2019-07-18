# Releasing pmd-designer

1.  Checkout master branch:
    
    ``` shell
    git clone https://github.com/pmd/pmd-designer.git
    cd pmd-designer
    ```

2.  Verify changelog `/CHANGELOG.md`. Adjust version number if necessary.

3.  Prepare the release (creates a new release tag).
    This will be done for you: http://maven.apache.org/plugins/maven-release-plugin/examples/prepare-release.html
    Maven will ask you about the release version, the tag name and the new version. You can simply hit enter,
    to use the default values.
    
    Note: the tag will be pushed automatically.
    
    ``` shell
    ./mvnw release:clean
    ./mvnw release:prepare
    ```

4.  Wait, until release is ready. The maven plugin will directly push the tag. The tag will be
    built by [travis-ci](https://travis-ci.com/pmd/pmd-designer). After it is done, the new release
    should be available under <https://repo.maven.apache.org/maven2/net/sourceforge/pmd/pmd-ui/>.

5.  Add release notes to github releases: <https://github.com/pmd/pmd-designer/releases>

6.  Prepare `/CHANGELOG.md` for the next version.
