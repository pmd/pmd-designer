/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import static java.util.Collections.emptyList;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.latestValue;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.VetoableEventStream.vetoableNull;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.util.Either;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.JvmLanguagePropertyBundle;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageProcessor;
import net.sourceforge.pmd.lang.LanguageProcessorRegistry;
import net.sourceforge.pmd.lang.LanguagePropertyBundle;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.Parser.ParserTask;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.ast.SemanticErrorReporter;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.document.TextDocument;
import net.sourceforge.pmd.util.fxdesigner.SourceEditorController;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.ParseAbortedException;
import net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry;
import net.sourceforge.pmd.util.log.PmdReporter;


/**
 * Manages a compilation unit for {@link SourceEditorController}.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ASTManagerImpl implements ASTManager {

    public static final PmdReporter NOOP_REPORTER = PmdReporter.quiet();

    private final DesignerRoot designerRoot;

    private final Var<List<File>> auxclasspathFiles = Var.newSimpleVar(emptyList());

    /**
     * Most up-to-date compilation unit. Is null if the current source cannot be parsed.
     */
    private final SuspendableVar<Node> compilationUnit = Var.<Node>newSimpleVar(null).suspendable();
    /**
     * Selected language version.
     */
    private final Var<@NonNull LanguageVersion> languageVersion = Var.newSimpleVar(AuxLanguageRegistry.defaultLanguageVersion());
    /**
     * Last valid source that was compiled, corresponds to {@link #compilationUnit}.
     */
    private final SuspendableVar<String> sourceCode = Var.newSimpleVar("").suspendable();
    private final SuspendableVar<TextDocument> sourceDocument = Var.newSimpleVar(TextDocument.readOnlyString("", languageVersion.getValue())).suspendable();
    private final SuspendableVar<LanguageProcessorRegistry> lpRegistry = Var.<LanguageProcessorRegistry>newSimpleVar(null).suspendable();

    private final Var<ParseAbortedException> currentException = Var.newSimpleVar(null);

    private final Var<Map<String, String>> ruleProperties = Var.newSimpleVar(Collections.emptyMap());

    public ASTManagerImpl(DesignerRoot owner) {
        this.designerRoot = owner;

        // Refresh the AST anytime the text, classloader, or language version changes
        sourceCode.values()
                  .or(classpathProperty().values())
                  .or(languageVersionProperty().values())
                  .subscribe(tick -> {
                      // note: if either of these values would be null
                      // the optional is empty.
                      Optional<List<File>> changedClasspath = tick.asLeft().filter(Either::isRight).map(Either::getRight);
                      Optional<LanguageVersion> changedLanguageVersion = Optional.of(tick).filter(Either::isRight).map(Either::getRight);

                      Node updated;
                      try {
                          updated = refreshAST(this, getSourceCode(), getLanguageVersion(),
                                  refreshRegistry(changedLanguageVersion.isPresent(), changedClasspath.isPresent())).orElse(null);
                          currentException.setValue(null);
                      } catch (ParseAbortedException e) {
                          updated = null;
                          currentException.setValue(e);
                      } catch (LinkageError e) {
                          // LinkageErrors might occur due to API incompatibilities with pmd-core at runtime.
                          updated = null;
                          logInternalException(e);
                      }

                      compilationUnit.setValue(updated);
                  });
    }


    @Override
    public TextDocument getSourceDocument() {
        return sourceDocument.getValue();
    }


    @Override
    public SuspendableVar<TextDocument> sourceDocumentProperty() {
        return sourceDocument;
    }


    public void setSourceDocument(TextDocument sourceDocument) {
        this.sourceDocument.setValue(sourceDocument);
    }


    @Override
    public SuspendableVar<String> sourceCodeProperty() {
        return sourceCode;
    }

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
    public Var<List<File>> classpathProperty() {
        return auxclasspathFiles;
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


    @Override
    public Val<LanguageProcessor> languageProcessorProperty() {
        return lpRegistry.mapDynamic(
            languageVersionProperty().map(LanguageVersion::getLanguage)
                                     .map(l -> lp -> lp.getProcessor(l))
        );
    }


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

    private LanguageProcessorRegistry createNewRegistry(LanguageVersion version, List<File> classpath) {
        Map<Language, LanguagePropertyBundle> langProperties = new HashMap<>();
        LanguagePropertyBundle bundle = version.getLanguage().newPropertyBundle();
        bundle.setLanguageVersion(version.getVersion());
        if (bundle instanceof JvmLanguagePropertyBundle) {
            bundle.setProperty(JvmLanguagePropertyBundle.AUX_CLASSPATH,
                classpath.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
        }

        langProperties.put(version.getLanguage(), bundle);

        LanguageRegistry languages =
                AuxLanguageRegistry.supportedLangs()
                        .getDependenciesOf(version.getLanguage());

        return LanguageProcessorRegistry.create(languages,
                langProperties,
                NOOP_REPORTER);
    }

    private LanguageProcessorRegistry refreshRegistry(boolean changedLanguageVersion, boolean changedClassLoader) {
        LanguageProcessorRegistry current = lpRegistry.getValue();
        if (current != null && !changedLanguageVersion && !changedClassLoader) {
            // the current one is fine
            return current;
        }

        if (current != null) {
            // current is invalid, recreate it
            current.close();
        }

        LanguageProcessorRegistry newRegistry = createNewRegistry(getLanguageVersion(), classpathProperty().getValue());
        lpRegistry.setValue(newRegistry);
        return newRegistry;
    }


    /**
     * Refreshes the compilation unit given the current state of the model.
     *
     * @throws ParseAbortedException if parsing fails and cannot recover
     */
    private static Optional<Node> refreshAST(ApplicationComponent component,
                                             String source,
                                             LanguageVersion version,
                                             LanguageProcessorRegistry lpRegistry) throws ParseAbortedException {

        String dummyFilePath = "dummy." + version.getLanguage().getExtensions().get(0);
        TextDocument textDocument = TextDocument.readOnlyString(source, FileId.fromPathLikeString(dummyFilePath), version);

        ParserTask task = new ParserTask(
            textDocument,
            SemanticErrorReporter.noop(),
            lpRegistry
        );

        LanguageProcessor processor = lpRegistry.getProcessor(version.getLanguage());
        RootNode node;
        try {
            node = processor.services().getParser().parse(task);
        } catch (Exception e) {
            component.logUserException(e, Category.PARSE_EXCEPTION);
            throw new ParseAbortedException(e);
        }

        // Notify that the parse went OK so we can avoid logging very recent exceptions

        component.raiseParsableSourceFlag(() -> "Param hash: " + Objects.hash(source, version, lpRegistry));

        return Optional.of(node);
    }
}
