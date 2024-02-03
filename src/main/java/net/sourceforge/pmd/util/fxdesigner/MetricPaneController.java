/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.metrics.LanguageMetricsProvider;
import net.sourceforge.pmd.lang.metrics.Metric;
import net.sourceforge.pmd.lang.metrics.MetricOptions;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.model.MetricResult;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;


/**
 * Controller of the node info panel (left).
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class MetricPaneController extends AbstractController implements NodeSelectionSource {


    @FXML
    private ToolbarTitledPane metricsTitledPane;
    @FXML
    private ListView<MetricResult<?>> metricResultsListView;

    private final Var<Integer> numAvailableMetrics = Var.newSimpleVar(0);


    public MetricPaneController(DesignerRoot designerRoot) {
        super(designerRoot);
    }


    @Override
    protected void beforeParentInit() {
        initNodeSelectionHandling(getDesignerRoot(), EventStreams.never(), false);

        metricsTitledPane.titleProperty().bind(numAvailableMetrics().map(i -> "Metrics\t(" + (i == 0 ? "none" : i) + " available)"));

    }


    /**
     * Displays info about a node. If null, the panels are reset.
     *
     * @param node    Node to inspect
     * @param options
     */
    @Override
    public void setFocusNode(final Node node, DataHolder options) {

        ObservableList<MetricResult<?>> metrics = evaluateAllMetrics(node);
        metricResultsListView.setItems(metrics);

        numAvailableMetrics.setValue((int) metrics.stream()
                                                  .map(MetricResult::getValue)
                                                  .count());
    }


    public Val<Integer> numAvailableMetrics() {
        return numAvailableMetrics;
    }


    private ObservableList<MetricResult<?>> evaluateAllMetrics(Node n) {
        LanguageMetricsProvider provider = n.getAstInfo().getLanguageProcessor().services().getLanguageMetricsProvider();
        if (provider == null) {
            return FXCollections.emptyObservableList();
        }
        List<MetricResult<?>> results = new ArrayList<>();
        for (Metric<?, ?> metric : provider.getMetrics()) {
            MetricResult<?> result = computeMetric(metric, n);
            if (result != null) {
                results.add(result);
            }
        }
        return FXCollections.observableArrayList(results);
    }


    private <R extends Number> MetricResult<R> computeMetric(Metric<?, R> metric, Node node) {
        @Nullable R result = Metric.compute(metric, node, MetricOptions.emptyOptions());
        if (result != null) {
            return new MetricResult<>(metric, result);
        }
        return null;
    }


    @Override
    public String getDebugName() {
        return "metric-panel";
    }
}
