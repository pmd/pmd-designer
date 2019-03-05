package net.sourceforge.pmd.util.fxdesigner.app.services;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.TextAwareNodeWrapper;

/**
 * Can provide the rich text for a node.
 *
 * @author Clément Fournier
 */
public interface RichTextMapper {

    /** Wraps a node into a convenience layer that can for example provide the rich text associated with it. */
    TextAwareNodeWrapper wrapNode(Node node);


}
