/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.model.VersionedXPathQuery;
import net.sourceforge.pmd.util.fxdesigner.model.XPathEvaluationException;
import net.sourceforge.pmd.util.fxdesigner.model.XPathEvaluator;

public abstract class XPathUpdateSubscriber implements ApplicationComponent {

    private final DesignerRoot root;
    private Subscription subscription = () -> {};

    public XPathUpdateSubscriber(DesignerRoot root) {
        this.root = root;
    }

    public Subscription init(ASTManager astManager) {
        MessageChannel<VersionedXPathQuery> service = root.getService(DesignerRoot.LATEST_XPATH);


        EventStream<?> merged = EventStreams.merge(
            astManager.compilationUnitProperty().values(),
            additionalTicks(),
            service.messageStream(true, this),
            astManager.ruleProperties().values().withDefaultEvent(Collections.emptyMap())
        );

        subscription = merged.subscribe(tick -> {
                                     Node compil = astManager.compilationUnitProperty().getOrElse(null);
                                     VersionedXPathQuery query = service.latestMessage().getOrElse(null);
                                     Map<String, String> props = astManager.ruleProperties().getOrElse(Collections.emptyMap());

                                     if (compil == null) {
                                         handleNoCompilationUnit();
                                         return;
                                     }
                                     if (query == null || StringUtils.isBlank(query.getExpression())) {
                                         handleNoXPath();
                                         return;
                                     }


                                     try {
                                         List<Node> results = XPathEvaluator.evaluateQuery(compil,
                                                                                           astManager.languageVersionProperty().getValue(),
                                                                                           query.getVersion(),
                                                                                           query.getExpression(),
                                                                                           props,
                                                                                           query.getDefinedProperties());

                                         handleXPathSuccess(results);
                                     } catch (XPathEvaluationException e) {
                                         handleXPathError(e);
                                     }

                                 });

        return this::unsubscribe;
    }

    /**
     * Additional refresh ticks. By default, the changes of
     * {@link ASTManager#compilationUnitProperty()}, of the local XPath
     * query (local as in scoped by a {@link DesignerRoot}), and of
     * the {@link ASTManager#ruleProperties()} are taken into account.
     */
    public EventStream<?> additionalTicks() {
        return EventStreams.never();
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    public void handleNoXPath() {
        // do nothing
    }


    public abstract void handleNoCompilationUnit();


    public abstract void handleXPathSuccess(List<Node> results);


    public abstract void handleXPathError(Exception e);

    public void unsubscribe() {
        subscription.unsubscribe();
        subscription = Subscription.EMPTY;
    }

}
