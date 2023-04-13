/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model;

import net.sourceforge.pmd.lang.metrics.Metric;

/**
 * @author Clément Fournier
 * @since 6.0.0
 */
public class MetricResult<R extends Number> {

    private final Metric<?, R> metric;
    private final R value;


    public MetricResult(Metric<?, R> metric, R value) {
        this.metric = metric;
        this.value = value;
    }


    public Metric<?, R> getKey() {
        return metric;
    }


    public R getValue() {
        return value;
    }
}
