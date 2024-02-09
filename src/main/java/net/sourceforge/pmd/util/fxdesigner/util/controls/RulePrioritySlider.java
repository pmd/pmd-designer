/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.lang.rule.RulePriority.HIGH;
import static net.sourceforge.pmd.lang.rule.RulePriority.LOW;
import static net.sourceforge.pmd.lang.rule.RulePriority.MEDIUM;

import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.scene.control.Slider;


/**
 * @author Clément Fournier
 */
public class RulePrioritySlider extends Slider {


    public RulePrioritySlider() {

        setMin(HIGH.getPriority());
        setMax(LOW.getPriority());
        setValue(MEDIUM.getPriority());
        setMajorTickUnit(1);
        setBlockIncrement(1);
        setMinorTickCount(0);
        setSnapToTicks(true);

        setLabelFormatter(DesignerUtil.stringConverter(
            d -> {
                RulePriority rp = RulePriority.valueOf(invert(d.intValue()));
                return rp != LOW && rp != HIGH && rp != MEDIUM ? "" : rp.getName();
            },
            s -> {
                throw new IllegalStateException("Shouldn't be called");
            }
        ));
    }

    // lowest priority has highest number, so we revert
    // the indices to get the slider in the right order
    private int invert(int num) {
        return LOW.getPriority() + HIGH.getPriority() - num;
    }


    public Var<RulePriority> priorityProperty() {
        return Var.doubleVar(valueProperty()).mapBidirectional(
            d -> RulePriority.valueOf(invert(d.intValue())),
            p -> Double.valueOf(invert(p.getPriority()))
        );
    }
}
