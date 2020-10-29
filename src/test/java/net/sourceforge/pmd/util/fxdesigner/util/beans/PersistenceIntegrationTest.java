/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.util.fxdesigner.util.beans.testdata.SomeBean;

public class PersistenceIntegrationTest {






    @Test
    public void testBeanRoundTrip() throws IOException {

        SomeBean bean = new SomeBean();
        bean.setK(IOException.class);
        bean.setStr("hahahaha");

        File tmp = Files.createTempFile("pmd-ui-test", "").toFile();

        SettingsPersistenceUtil.persistProperties(bean, tmp);

        SomeBean other = new SomeBean();

        assertNotEquals(bean, other);

        SettingsPersistenceUtil.restoreProperties(other, tmp);

        assertEquals(bean, other);
    }






}
