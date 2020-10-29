/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;


/**
 * @author Clément Fournier
 * @since 6.0.0
 */
public class DesignerUtilTest {

    @Test
    public void testGetFxml() {
        assertNotNull(DesignerUtil.getFxml("designer"));
        assertNotNull(DesignerUtil.getFxml("xpath-rule-editor"));
    }


}
