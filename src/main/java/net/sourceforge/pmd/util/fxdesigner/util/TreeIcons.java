/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.EnumMap;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import net.sourceforge.pmd.util.designerbindings.DesignerBindings.TreeIconId;

public final class TreeIcons {

    private static final EnumMap<TreeIconId, String> ICONS_CSS = new EnumMap<>(TreeIconId.class);
    private static final EnumMap<TreeIconId, String> ICONS_DISPLAY_NAME = new EnumMap<>(TreeIconId.class);


    static {
        for (TreeIconId id : TreeIconId.values()) {
            String lowerName = id.name().toLowerCase(Locale.ROOT);
            String idCss = "icon-" + lowerName.replace('_', '-');

            String displayName = StringUtils.capitalize(lowerName) + " declaration";

            ICONS_CSS.put(id, idCss);
            ICONS_DISPLAY_NAME.put(id, displayName);
        }
    }


    public static String cssClass(TreeIconId id) {
        return id == null ? null : ICONS_CSS.get(id);
    }


    public static String displayName(TreeIconId id) {
        return id == null ? null : ICONS_DISPLAY_NAME.get(id);
    }

}
