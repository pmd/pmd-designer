/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.latestValue;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.VetoableEventStream.vetoableNull;

import java.io.StringReader;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.LanguageVersionHandler;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.SourceEditorController;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.ParseAbortedException;
import net.sourceforge.pmd.util.fxdesigner.util.Tuple3;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;


/**
 * Manages a compilation unit for {@link SourceEditorController}.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ASTManagerImpl implements ASTManager {

    private final DesignerRoot designerRoot;

    private final Var<ClassLoader> auxclasspathClassLoader = Var.newSimpleVar(null);

    /**
     * Most up-to-date compilation unit. Is null if the current source cannot be parsed.
     */
    private final Var<Node> compilationUnit = Var.newSimpleVar(null);
    /**
     * Selected language version.
     */
    private final Var<LanguageVersion> languageVersion = Var.newSimpleVar(LanguageRegistry.getDefaultLanguage().getDefaultVersion());
    /**
     * Last valid source that was compiled, corresponds to {@link #compilationUnit}.
     */
    private Var<String> sourceCode = Var.newSimpleVar("");

    private Var<ParseAbortedException> currentException = Var.newSimpleVar(null);

    private Var<Map<String, String>> ruleProperties = Var.newSimpleVar(Collections.emptyMap());

    public ASTManagerImpl(DesignerRoot owner) {
        this.designerRoot = owner;

        // Refresh the AST anytime the text, classloader, or language version changes
        sourceCode.values()
                  .or(auxclasspathClassLoader.values())
                  .or(languageVersionProperty().values())
                  .map(tick -> new Tuple3<>(getSourceCode(), getLanguageVersion(), classLoaderProperty().getValue()))
                  .distinct()
                  .subscribe(tick -> {

                      String source = tick.first;
                      LanguageVersion version = tick.second;
                      ClassLoader classLoader = tick.third;


                      if (StringUtils.isBlank(source) || version == null) {
                          compilationUnit.setValue(null);
                          currentException.setValue(null);
                          return;
                      }

                      if (classLoader == null) {
                          classLoader = ASTManagerImpl.class.getClassLoader();
                      }

                      Node updated;
                      try {
                          updated = refreshAST(this, source, version, classLoader).orElse(null);
                          currentException.setValue(null);
                      } catch (ParseAbortedException e) {
                          updated = null;
                          currentException.setValue(e);
                      }

                      compilationUnit.setValue(updated);
                  });
    }

    public ASTManagerImpl(ASTManagerImpl base, Function<LanguageVersion, LanguageVersion> languageVersionMap) {
        this(base.getDesignerRoot());

        languageVersionProperty().bind(base.languageVersionProperty().map(languageVersionMap));
        sourceCode.bind(base.sourceCodeProperty());
        classLoaderProperty().bind(base.classLoaderProperty());

    }


    @Override
    public Var<String> sourceCodeProperty() {
        return Var.fromVal(sourceCode.orElseConst(""), this::setSourceCode);
    }

    @PersistentProperty
    @Override
    public String getSourceCode() {
        return sourceCode.getValue();
    }

    @Override
    public void setSourceCode(String sourceCode) {
        if (StringUtils.isEmpty(sourceCode)) {
            sourceCode = "";
        }
        this.sourceCode.setValue(sourceCode);
    }

    @Override
    public Var<ClassLoader> classLoaderProperty() {
        return auxclasspathClassLoader;
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }


    @Override
    public Var<Map<String, String>> ruleProperties() {
        return ruleProperties;
    }

    @Override
    public Var<LanguageVersion> languageVersionProperty() {
        return languageVersion;
    }


    @PersistentProperty
    public LanguageVersion getLanguageVersion() {
        return languageVersion.getValue();
    }

    public void setLanguageVersion(LanguageVersion version) {
        this.languageVersion.setValue(version);
    }

    @Override
    public Val<Node> compilationUnitProperty() {
        // veto null events to ignore null compilation units if they're
        // followed by a valid one quickly
        Val<Node> nodeVal = latestValue(vetoableNull(compilationUnit.values(), Duration.ofMillis(500)));
        nodeVal.pin();
        return nodeVal;
    }

    @Override
    public Var<ParseAbortedException> currentExceptionProperty() {
        return currentException;
    }

    /**
     * Refreshes the compilation unit given the current state of the model.
     *
     * @throws ParseAbortedException if parsing fails and cannot recover
     */
    private static Optional<Node> refreshAST(ApplicationComponent component,
                                             String source,
                                             LanguageVersion version,
                                             ClassLoader classLoader) throws ParseAbortedException {

        LanguageVersionHandler languageVersionHandler = version.getLanguageVersionHandler();
        Parser parser = languageVersionHandler.getParser(languageVersionHandler.getDefaultParserOptions());

        Node node;
        try {
            node = parser.parse(null, new StringReader(source));
        } catch (Exception e) {
            component.logUserException(e, Category.PARSE_EXCEPTION);
            throw new ParseAbortedException(e);
        }


        try {
            languageVersionHandler.getSymbolFacade().start(node);
        } catch (Exception e) {
            component.logUserException(e, Category.SYMBOL_FACADE_EXCEPTION);
        }
        try {
            languageVersionHandler.getQualifiedNameResolutionFacade(classLoader).start(node);
        } catch (Exception e) {
            component.logUserException(e, Category.QNAME_RESOLUTION_EXCEPTION);
        }

        try {
            languageVersionHandler.getTypeResolutionFacade(classLoader).start(node);
        } catch (Exception e) {
            component.logUserException(e, Category.TYPERESOLUTION_EXCEPTION);
        }

        // Notify that the parse went OK so we can avoid logging very recent exceptions

        component.raiseParsableSourceFlag(() -> "Param hash: " + Objects.hash(source, version, classLoader));

        return Optional.of(node);
    }


}
