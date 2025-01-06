# Changelog

## Unreleased

**New GPG Release Signing Key**

Since January 2025, we switched the GPG Key we use for signing releases in Maven Central to be
[A0B5CA1A4E086838](https://keyserver.ubuntu.com/pks/lookup?search=0x2EFA55D0785C31F956F2F87EA0B5CA1A4E086838&fingerprint=on&op=index).
The full fingerprint is `2EFA 55D0 785C 31F9 56F2  F87E A0B5 CA1A 4E08 6838`.

**New Git default branch - "main"**

We are joining the Git community and updating "master" to "main". Using the term "master" for the main
development branch can be offensive to some people. Existing versions of Git have been always capable of
working with any branch name and since 2.28.0 (July 2020) the default initial branch is configurable
(`init.defaultBranch`). Since October 2020, the default branch for new repositories on GitHub
is "main". Finally, PMD will also use this new name for the main branch in all our own repositories.

**Fixed issues:**

**Merged pull requests:**

* [#93](https://github.com/pmd/pmd-designer/pull/93) Change branch master to main by [@adangel](https://github.com/adangel)
* [#98](https://github.com/pmd/pmd-designer/pull/98) Use new gpg key (A0B5CA1A4E086838) by [@adangel](https://github.com/adangel)

See https://github.com/pmd/pmd-designer/milestone/16

## 7.2.0

**Merged pull requests:**

* [#85](https://github.com/pmd/pmd-designer/pull/85) Properly stringify collection attributes as sequences by [@jsotuyod](https://github.com/jsotuyod)

See https://github.com/pmd/pmd-designer/milestone/15

## 7.0.0

* **Bump required pmd-core version to 7.0.0.**

**Changed maven GAV**
The maven coordinates (GAV) have been changed. The artifactId has been renamed to pmd-designer.

```xml
<dependency>
  <groupId>net.sourceforge.pmd</groupId>
  <artifactId>pmd-designer</artifactId>
  <version>7.0.0</version>
</dependency>
```

At the same time, the release artefact name changed to **pmd-designer-7.0.0.jar**.

**Changed default OpenJFX version to be 17**

The designer is built now against openjfx 17. When using the designer with OpenJDK, at least Java 11
is required with openjfx 17 or later.

The designer can still be started with Oracle's Java 8, which includes JavaFX. But this is not
explicitly tested.

**Fixed issues:**

* [#54](https://github.com/pmd/pmd-designer/issues/54) Upgrade openjfx to 17
* [#65](https://github.com/pmd/pmd-designer/issues/65) Rename artifactId to pmd-designer

**Merged pull requests:**

* [#77](https://github.com/pmd/pmd-designer/pull/77) Enable PMD checks with PMD 7 by [@adangel](https://github.com/adangel)
* [#78](https://github.com/pmd/pmd-designer/pull/78) Fix deprecations from PMD 7 (getTerseName -> getId) by [@adangel](https://github.com/adangel)
* [#79](https://github.com/pmd/pmd-designer/pull/79) Update to latest PMD 7.0.0-SNAPSHOT by [@adangel](https://github.com/adangel)
* [#83](https://github.com/pmd/pmd-designer/pull/83) Avoid deprecated and internal API usage by [@adangel](https://github.com/adangel)

See https://github.com/pmd/pmd-designer/milestone/11

## 7.0.0-rc4

* **Bump required pmd-core version to 7.0.0-rc4.**

**Fixed issues:**

* [#61](https://github.com/pmd/pmd-designer/issues/61) Remove dependency to jcommander
* [#62](https://github.com/pmd/pmd-designer/issues/62) Exceptions and errors are not always logged
* [#63](https://github.com/pmd/pmd-designer/issues/63) Update to PMD 7.0.0-rc3
* [#72](https://github.com/pmd/pmd-designer/issues/72) NPE when launching Designer
* [#73](https://github.com/pmd/pmd-designer/issues/73) Remove commons-io dependency

**Merged pull requests:**

* [#68](https://github.com/pmd/pmd-designer/pull/68) Expose all properties with default values by [@jsotuyod](https://github.com/jsotuyod)
* [#69](https://github.com/pmd/pmd-designer/pull/69) Perform the persistence asynchronously to not block the main (UI) thread #69 by [@adangel](https://github.com/adangel)
* [#70](https://github.com/pmd/pmd-designer/pull/70) Fix drag and drop for tests case violations by [@adangel](https://github.com/adangel)
* [#74](https://github.com/pmd/pmd-designer/pull/74) Remove commons-io by [@adangel](https://github.com/adangel)
* [#75](https://github.com/pmd/pmd-designer/pull/75) Update to PMD 7.0.0-SNAPSHOT (upcoming 7.0.0-rc4) by [@adangel](https://github.com/adangel)
* [#76](https://github.com/pmd/pmd-designer/pull/76) Correctly determine JavaFX version when embedded by [@adangel](https://github.com/adangel)

See https://github.com/pmd/pmd-designer/milestone/11

## 7.0.0-rc1

* **Bump required pmd-core version to 7.0.0-rc1.**

**Fixed issues:**

* [#43](https://github.com/pmd/pmd-designer/issues/43) Update PMD 7 Logo in the Designer

**Merged pull requests:**

* [#51](https://github.com/pmd/pmd-designer/pull/51) Compat with pmd/pmd#3893 by [@oowekyala](https://github.com/oowekyala)
* [#57](https://github.com/pmd/pmd-designer/pull/57) Update to latest pmd 7 changes by [@oowekyala](https://github.com/oowekyala)

See https://github.com/pmd/pmd-designer/milestone/11

## 6.49.0

**Fixed issues:**

* [#10](https://github.com/pmd/pmd-designer/issues/10) [ui] Designer font display

**Merged pull requests:**

* [#52](https://github.com/pmd/pmd-designer/pull/52) Better expose entry points for pmd-cli integration by [@jsotuyod](https://github.com/jsotuyod)
* [#53](https://github.com/pmd/pmd-designer/pull/53) Resolve text rendering issues across different platforms by [@jsotuyod](https://github.com/jsotuyod)

See https://github.com/pmd/pmd-designer/milestone/14

## 6.37.0

* [#50](https://github.com/pmd/pmd-designer/pull/50) Usability improvements by [@jsotuyod](https://github.com/jsotuyod)

* **Bump required pmd-core version to 6.27.0.** The previous minimal pmd-core version was 6.23.0.

See https://github.com/pmd/pmd-designer/milestone/13

## 6.27.0

* This version of pmd-designer requires at least PMD 6.27.0 due to changes in tab size.
* Adjust tab width char handling (refs [#45](https://github.com/pmd/pmd-designer/pull/45))

See https://github.com/pmd/pmd-designer/milestone/12

## 6.24.0

* **Bump required pmd-core version to 6.23.0.** The previous minimal pmd-core version was 6.21.0.
* Display the "main attribute" of nodes in the tree. This is usually the `@Name` of a node or the `@Image`. (refs [#42](https://github.com/pmd/pmd-designer/pull/42))
* XPath attributes panel is revamped to a table, "extra info" is listed at the bottom (this includes the resolved type for Java) (refs [#42](https://github.com/pmd/pmd-designer/pull/42))
* Deprecated XPath attributes are displayed crossed out. There is even a tooltip showing, which attribute to use instead. (refs [#42](https://github.com/pmd/pmd-designer/pull/42))

See https://github.com/pmd/pmd-designer/milestone/10

## 6.21.0

* **Bump required pmd-core version to 6.21.0.** The previous minimal pmd-core version was 6.12.0.
* Add Modelica syntax highlighter, thanks to [Anatoly Trosinenko](https://github.com/atrosinenko)
* Add GUI to export trees to XML or text format. This integrates the new experimental TreeRenderer API. (refs [#37](https://github.com/pmd/pmd-designer/pull/37))
* Add scrollbars to code area (fixes [#22](https://github.com/pmd/pmd-designer/issues/22))
* Fix popover focus issues (fixes [#38](https://github.com/pmd/pmd-designer/issues/38))
* Internal improvements (refs [#33](https://github.com/pmd/pmd-designer/issues/33))
* Plain text language is not loaded through the language registry anymore (see revision da47f3758)


See https://github.com/pmd/pmd-designer/milestone/9


## 6.19.0

* Add scala syntax highlighter
* When no language modules are on the classpath, the app will still run normally, opening files with a "plain text" language and displaying a warning.
* Fix [#27](https://github.com/pmd/pmd-designer/issues/27): Rule export escapes special characters in rules unnecessarily
* Fix some internal issues
  * Version stamping not working

See https://github.com/pmd/pmd-designer/milestone/7

## 6.17.0

* Added test case edition features. Usage is [documented on the wiki](https://github.com/pmd/pmd-designer/wiki/Testing_rules). Preview:

![testing-designer](https://user-images.githubusercontent.com/24524930/61461094-504a7900-a970-11e9-822e-30cc121b568c.gif)


## 6.16.0

* Added the possibility to search the treeview. Press CTRL+F when it's focused, or click on the `Search` :mag_right: button and enter a search query. You can cycle through results with `CTRL+TAB` or `CTRL+F3`, and cycle back with `CTRL+SHIFT+TAB` or `CTRL+SHIFT+F3`.

![TreeView search demo](https://user-images.githubusercontent.com/24524930/58752348-a3926600-84ad-11e9-9ef2-11920590b5e5.gif)

* Fix some bugs with XPath autocompletion

## 6.14.0

* Added the possibility to select a node directly from the code area.
To use this, hold the CTRL key pressed for a second. The code area should
change colors. In that state, hovering above some text should select the
node to which it corresponds:


![Node selection demo](https://user-images.githubusercontent.com/24524930/53699223-3c014780-3de6-11e9-9c6b-b9382a3c1117.gif)

* Added a crumb bar that displays the ancestors of the currently selected node.

* When you have selected a node and start editing the code again, your selection
should be preserved in most cases (on a best-effort basis) instead of reset.

* Improved the XPath export wizard

* Allow multiple independent xpath editors
