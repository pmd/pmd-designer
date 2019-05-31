/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.ResultSelectionStrategy;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.TextFlow;

public class SearchableTreeView<T> extends TreeView<T> {


    private ResultSelectionStrategy selector = new ResultSelectionStrategy();


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

        LiveList<SearchableTreeItem<T>> allItems = new LiveArrayList<>();

        Subscription rootSub = Val.wrap(rootProperty())
                                  .map(it -> getRealRoot())
                                  .changes()
                                  .subscribe(ch -> {
                                      if (ch.getOldValue() != null) {
                                          allItems.clear();
                                      }
                                      if (ch.getNewValue() != null) {
                                          List<SearchableTreeItem<T>> tmp = new ArrayList<>();
                                          ch.getNewValue().foreach(tmp::add);
                                          allItems.addAll(tmp);
                                      }
                                  });


        LiveList<SearchableTreeCell<T>> cells = ReactfxExtensions.flattenVals(allItems.map(SearchableTreeItem::treeCellProperty));

        Val<String> queryVal = Val.wrap(query).filter(StringUtils::isNotBlank);
        return ReactfxUtil.subscribeDynamic(
            queryVal,
            q -> {
                return ReactfxExtensions.dynamic(
                    cells,
                    (c, i) -> Optional.ofNullable(c)
                                      .flatMap(it -> selector.evaluateBestSingle(it.getSearchableText(), q))
                                      .map(result -> {
                                          c.searchText.setValue(result.getTextFlow());
                                          c.updateItem(c.getItem(), c.isEmpty());
                                          return (Subscription) () -> {
                                              c.searchText.setValue(null);
                                              c.updateItem(c.getItem(), c.isEmpty());
                                          };
                                      })
                                      .orElse(Subscription.EMPTY)
                );

                //                cells.forEach();

//                return () ->
//                    cells.forEach(c -> {
//                        c.searchText.setValue(null);
//                        c.updateItem(c.getItem(), c.isEmpty());
//                    });
            }
        ).and(rootSub);
    }

    public static abstract class SearchableTreeItem<T> extends TreeItem<T> {

        private final Var<SearchableTreeCell<T>> treeCell = Var.newSimpleVar(null);

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
    }

    public abstract static class SearchableTreeCell<T> extends TreeCell<T> {


        protected final Var<TextFlow> searchText = Var.newSimpleVar(null);

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

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
        }

        public Val<SearchableTreeItem<T>> realItemProperty() {
            return Val.wrap(treeItemProperty()).map(it -> (SearchableTreeItem<T>) it);
        }

        /**
         * Text that should be matched against the query.
         */
        public abstract String getSearchableText();


    }
}
