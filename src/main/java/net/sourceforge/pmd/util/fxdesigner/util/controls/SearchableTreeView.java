/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.CamelCaseMatcher;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.MatchResult;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.MatchSelector;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.StringMatchAlgo;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;

public class SearchableTreeView<T> extends TreeView<T> {

    private final TreeViewWrapper<T> myWrapper = new TreeViewWrapper<>(this);

    public SearchableTreeView() {


        addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            // CTRL + F should be normal
            if (evt.isControlDown() && evt.getCode() == KeyCode.F) {
                popSearchField();
                evt.consume();
            }
        });

    }

    public void setRealRoot(SearchableTreeItem<T> root) {
        setRoot(root);
    }

    private SearchableTreeItem<T> getRealRoot() {
        return (SearchableTreeItem<T>) getRoot();
    }


    private void popSearchField() {
        TextField textField = new TextField();


        HBox box = new HBox(10., textField);

        Val<String> query = Val.wrap(textField.textProperty());
        textField.setPrefWidth(150);
        Subscription subscription = bindSearchQuery(query.conditionOnShowing(box));

        Popup popup = new Popup();
        popup.getScene().getStylesheets().addAll(DesignerUtil.getCss("designer").toExternalForm());
        popup.getContent().addAll(box);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        Bounds bounds = localToScreen(getBoundsInLocal());
        popup.show(this, bounds.getMaxX() - textField.getPrefWidth() - 5, bounds.getMinY());
        popup.setOnHidden(e -> subscription.unsubscribe());

        // Hide popup when ENTER is pressed
        EventStreams.eventsOf(popup, KeyEvent.KEY_RELEASED)
                    .filter(it -> it.getCode() == KeyCode.ENTER)
                    .subscribeForOne(e -> {
                        popup.hide();
                        e.consume();
                    });

        textField.requestFocus();
    }

    /**
     * Update the cells to search for anything.
     */
    public final Subscription bindSearchQuery(ObservableValue<String> query) {

        Val<String> queryVal = Val.wrap(query).filter(StringUtils::isNotBlank).map(String::trim)
                                  .filter(it -> it.length() > 1);

        Val<List<SearchableTreeItem<T>>> allItems = Val.wrap(rootProperty())
                                                       .map(it1 -> getRealRoot())
                                                       .map(it1 -> {
                                                           List<SearchableTreeItem<T>> tmp = new ArrayList<>();
                                                           it1.foreach(tmp::add);
                                                           return tmp;
                                                       })
                                                       .orElseConst(Collections.emptyList());

        return ReactfxUtil.subscribeDisposable(
            queryVal,
            q -> {

                Val<List<MatchResult<SearchableTreeItem<T>>>> selectedResults =
                    allItems.map(it -> selectMatches(q, it));

                return ReactfxUtil.subscribeDisposable(
                    selectedResults,
                    newRes -> {
                        // the values are never null, at most empty, because of orElseConst above
                        newRes.forEach(res -> res.getData().currentSearchResult.setValue(res));
                        Subscription sub = Subscription.EMPTY;
                        if (!newRes.isEmpty()) {

                            Var<Integer> curIdx = Var.newSimpleVar(0);
                            curIdx.values()
                                  .subscribe(idx -> {
                                      SearchableTreeItem<T> item = newRes.get(idx).getData();
                                      int row = getRow(item);
                                      getSelectionModel().select(row);

                                      if (!myWrapper.isIndexVisible(row)) {
                                          int safe = row < 3 ? 0 : row - 3;
                                          scrollTo(safe);
                                      }
                                  });

                            sub = sub.and(subscribeKeyNav(newRes.size(), curIdx));
                        }
                        refresh();
                        return sub;
                    }).and(
                    () -> {
                        selectedResults.ifPresent(lst -> lst.forEach(it -> it.getData().currentSearchResult.setValue(null)));
                        refresh();
                    });
            }
        );

    }

    private Subscription subscribeKeyNav(int numResults, Var<Integer> curIdx) {

        return EventStreams.eventsOf(this, KeyEvent.KEY_RELEASED)
                           .filter(it -> it.getCode() == KeyCode.F3)
                           .subscribe(ke -> {
                               curIdx.setValue((curIdx.getValue() + 1) % numResults);
                               ke.consume();
                           });


    }


    private List<MatchResult<SearchableTreeItem<T>>> selectMatches(String query, List<SearchableTreeItem<T>> items) {
        MatchSelector<SearchableTreeItem<T>> limiter =
            CamelCaseMatcher.<SearchableTreeItem<T>>allQueryStarts()
                .andThen(c -> c.filter(it -> it.getScore() > 0))
                //.andThen(CamelCaseMatcher.onlyWordStarts())
                .andThen(MatchSelector.selectBestTies());

        return StringMatchAlgo.filterResults(items, SearchableTreeItem::getSearchableText, query, limiter)
                              .sorted(Comparator.comparingInt(res -> res.getData().getTreeIndex()))
                              .collect(Collectors.toList());
    }

    public static abstract class SearchableTreeItem<T> extends TreeItem<T> {

        private final Var<SearchableTreeCell<T>> treeCell = Var.newSimpleVar(null);
        private final Var<MatchResult> currentSearchResult = Var.newSimpleVar(null);
        private final int treeIndex;

        public SearchableTreeItem(T n, int treeIndex) {
            super(n);
            this.treeIndex = treeIndex;
        }


        void foreach(Consumer<? super SearchableTreeItem<T>> consumer) {
            ASTTreeItem.foreach(this, consumer);
        }


        public Var<SearchableTreeCell<T>> treeCellProperty() {
            return treeCell;
        }

        public Val<MatchResult> currentSearchResultProperty() {
            return currentSearchResult;
        }

        public abstract String getSearchableText();

        public int getTreeIndex() {
            return treeIndex;
        }
    }

    public abstract static class SearchableTreeCell<T> extends TreeCell<T> {

        public SearchableTreeCell() {

            // Binds the cell to its treeItem
            realItemProperty()
                .changes()
                .subscribe(change -> {
                    if (change.getOldValue() != null) {
                        change.getOldValue().treeCellProperty().setValue(null);
                    }
                    if (change.getNewValue() != null) {
                        change.getNewValue().treeCellProperty().setValue(this);
                    }
                });
        }

        protected Val<MatchResult> searchResultProperty() {
            return realItemProperty().flatMap(SearchableTreeItem::currentSearchResultProperty);
        }


        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {

                Optional<MatchResult> completionResult = searchResultProperty().getOpt();

                if (completionResult.isPresent()) {
                    setGraphic(completionResult.get().getTextFlow());
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(realItemProperty().getValue().getSearchableText());
                }

                commonUpdate(item);
            }
        }

        public abstract void commonUpdate(T item);


        public final Val<SearchableTreeItem<T>> realItemProperty() {
            return Val.wrap(treeItemProperty()).map(it -> (SearchableTreeItem<T>) it);
        }
    }
}
