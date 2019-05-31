/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.MatchResult;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.CamelCaseMatcher;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.MatchLimiter;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.StringMatchAlgo;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class SearchableTreeView<T> extends TreeView<T> {

    private final TreeViewWrapper<T> myWrapper = new TreeViewWrapper<>(this);


    private StringMatchAlgo selector = new CamelCaseMatcher();


    public SearchableTreeView() {

    }

    public void setRealRoot(SearchableTreeItem<T> root) {
        setRoot(root);
    }

    private SearchableTreeItem<T> getRealRoot() {
        return (SearchableTreeItem<T>) getRoot();
    }

    /**
     * Update the cells to search for anything.
     */
    public final Subscription bindSearchQuery(ObservableValue<String> query) {
        return searchSub(query);
    }

    private Subscription searchSub(ObservableValue<String> query) {

        Val<List<SearchableTreeItem<T>>> allItems =
            Val.wrap(rootProperty())
               .map(it -> getRealRoot())
               .map(it -> {
                   List<SearchableTreeItem<T>> tmp = new ArrayList<>();
                   it.foreach(tmp::add);
                   return tmp;
               })
               .orElseConst(Collections.emptyList());


        Val<String> queryVal = Val.wrap(query).filter(StringUtils::isNotBlank);

        return ReactfxUtil.subscribeDynamic(
            queryVal,
            q -> {
                Val<List<MatchResult<SearchableTreeItem<T>>>> selectedResults = allItems.map(it -> selectMatches(q, it));
                return selectedResults.values()
                                      .subscribe(newRes -> {
                                          // the values are never null, at most empty, because of orElseConst above
                                          newRes.forEach(res -> res.getData().currentSearchResult.setValue(res));
                                          if (!newRes.isEmpty()) {
                                              getSelectionModel().select(newRes.get(0).getData());
                                              int idx = getSelectionModel().getSelectedIndex();
                                              if (!myWrapper.isIndexVisible(idx)) {
                                                  scrollTo(idx);
                                              }
                                          }
                                          refresh();
                                      })
                                      .and(() -> {
                                          selectedResults.ifPresent(lst -> lst.forEach(it -> it.getData().currentSearchResult.setValue(null)));
                                          refresh();
                                      });
            }
        );

    }

    private List<MatchResult<SearchableTreeItem<T>>> selectMatches(String query, List<SearchableTreeItem<T>> items) {
        return selector.filterResults(items, SearchableTreeItem::getSearchableText, query, MatchLimiter.selectBestTies())
                       .collect(Collectors.toList());
    }

    public static abstract class SearchableTreeItem<T> extends TreeItem<T> {

        private final Var<SearchableTreeCell<T>> treeCell = Var.newSimpleVar(null);
        private final Var<MatchResult> currentSearchResult = Var.newSimpleVar(null);

        public SearchableTreeItem() {
        }

        public SearchableTreeItem(T value) {
            super(value);
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
