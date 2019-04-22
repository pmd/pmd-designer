# Changelog since version 6.12.0

* Added the possibility to select a node directly from the code area.
To use this, hold the CTRL key pressed for a second. The code area should
change colors. In that state, hovering above some text should select the
node to which it corresponds:


![Node selection demo](https://user-images.githubusercontent.com/24524930/53699223-3c014780-3de6-11e9-9c6b-b9382a3c1117.gif)

* Added a crumb bar that displays the ancestors of the currently selected node.

* When you have selected a node and start editing the code again, your selection
should be preserved in most cases (on a best-effort basis) instead of reset.

* Improved the XPath export wizard
