/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.reactfx.value.Val;


/**
 * Some titled object.
 *
 * @author Clément Fournier
 */
public interface TitleOwner {

    /** Title of the region. */
    Val<String> titleProperty();

}
