/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.control.RangeSlider;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.util.StringConverter;


/**
 * @author Clément Fournier
 */
public class LanguageVersionRangeSlider extends RangeSlider {


    private static final int NO_MIN = -1;
    private static final String NO_MIN_STR = "No minimum";
    private static final String NO_MAX_STR = "No maximum";
    private final Var<Language> currentLanguage = Var.newSimpleVar(null);
    private List<LanguageVersion> curVersions = new ArrayList<>();


    public LanguageVersionRangeSlider() {

        setMin(NO_MIN);
        setBlockIncrement(1);
        setMajorTickUnit(1);
        setMinorTickCount(0);
        setSnapToTicks(true);

        currentLanguage.values().distinct().subscribe(this::initLanguage);

        StringConverter<Number> converter = DesignerUtil.stringConverter(
            num -> {
                int n = num.intValue();
                if (n < 0) {
                    return NO_MIN_STR;
                } else if (n >= curVersions.size()) {
                    return NO_MAX_STR;
                } else {
                    return curVersions.get(n).getShortName();
                }
            },
            ver -> {
                switch (ver) {
                case NO_MIN_STR:
                    return -1;
                case NO_MAX_STR:
                    return curVersions.size();
                default:
                    LanguageVersion withName = curVersions.stream().filter(lv -> lv.getShortName().equals(ver)).findFirst().get();
                    return curVersions.indexOf(withName);
                }
            }
        );

        setLabelFormatter(converter);


    }


    private void initLanguage(Language language) {

        if (language == null) {
            curVersions = Collections.emptyList();
            setDisable(true);
            return;
        }

        // for some reason Collections.sort doesn't work
        curVersions = language.getVersions().stream().sorted(LanguageVersion::compareTo).collect(Collectors.toList());

        setDisable(curVersions.size() < 2);

        setMax(curVersions.size());
    }


    private LanguageVersion fromIndex(int idx) {
        return idx < 0 || idx >= curVersions.size() ? null : curVersions.get(idx);
    }


    public Var<LanguageVersion> minVersionProperty() {
        return Var.mapBidirectional(
            lowValueProperty(),
            num -> fromIndex(num.intValue()),
            ver -> ver == null ? -1 : curVersions.indexOf(ver)
        );
    }


    public Var<LanguageVersion> maxVersionProperty() {
        return Var.mapBidirectional(
            highValueProperty(),
            num -> fromIndex(num.intValue()),
            ver -> ver == null ? curVersions.size() : curVersions.indexOf(ver)
        );
    }


    public Var<Language> currentLanguageProperty() {
        return currentLanguage;
    }
}
