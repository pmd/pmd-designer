/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.xpath.Attribute;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;
import net.sourceforge.pmd.lang.symboltable.Scope;
import net.sourceforge.pmd.lang.symboltable.ScopedNode;
import net.sourceforge.pmd.util.designerbindings.RelatedNodesSelector;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;

import com.sun.javafx.fxml.builder.ProxyBuilder;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.util.BuilderFactory;
import javafx.util.Callback;
import javafx.util.StringConverter;


/**
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class DesignerUtil {

    private static final Pattern EXCEPTION_PREFIX_PATTERN = Pattern.compile("(?:(?:\\w+\\.)*\\w+:\\s*)*\\s*(.*)$", Pattern.DOTALL);

    private static final Pattern JJT_ACCEPT_PATTERN = Pattern.compile("net.sourceforge.pmd.lang.\\w++.ast.AST(\\w+).jjtAccept");
    public static final String DESIGNER_DOC_URL = "https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html";
    public static final String DESIGNER_NEW_ISSUE_URL = "https://github.com/pmd/pmd-designer/issues/new/choose";


    private DesignerUtil() {

    }


    public static <T> Set<T> setOf(T... ts) {
        LinkedHashSet<T> set = new LinkedHashSet<>(ts.length);
        Collections.addAll(set, ts);
        return set;
    }

    public static <T> Set<T> setOf(T ts) {
        return Collections.singleton(ts);
    }

    public static <T> Set<T> setOf() {
        return Collections.emptySet();
    }


    /**
     * Was added in java 9...
     */
    public static <T> Optional<T> or(Optional<T> base, Supplier<Optional<T>> fallback) {
        if (base.isPresent()) {
            return base;
        }

        return fallback.get();
    }


    /**
     * Gets the URL to an fxml file from its simple name.
     *
     * @param simpleName Simple name of the file, i.e. with no directory prefixes or extension
     *
     * @return A URL to an fxml file
     */
    public static URL getFxml(String simpleName) {
        return getResource("fxml/" + simpleName + ".fxml");
    }

    /**
     * Gets the URL to a file from its simple name.
     *
     * @return A URL to a file
     */
    public static URL getResource(String resRelativeToDesignerDir) {
        return DesignerUtil.class.getResource("/net/sourceforge/pmd/util/fxdesigner/" + resRelativeToDesignerDir);
    }

    /**
     * Gets the URL to an css file from its simple name.
     *
     * @param simpleName Simple name of the file, i.e. with no directory prefixes or extension
     *
     * @return A URL to a css file
     */
    public static URL getCss(String simpleName) {
        return getResource("css/" + simpleName + ".css");
    }

    public static void addCustomStyleSheets(Parent target, String... styleSheetSimpleName) {
        Arrays.stream(styleSheetSimpleName)
              .map(DesignerUtil::getCss)
              .map(URL::toExternalForm)
              .forEach(target.getStylesheets()::add);
    }


    public static <T> Callback<ListView<T>, ListCell<T>> simpleListCellFactory(Function<T, String> converter, Function<T, String> toolTipMaker) {
        return collection -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    Tooltip.uninstall(this, getTooltip());
                } else {
                    setText(converter.apply(item));
                    Tooltip.install(this, new Tooltip(toolTipMaker.apply(item)));
                }
            }
        };
    }


    public static <T> StringConverter<T> stringConverter(@NonNull Function<T, String> toString, @NonNull Function<String, T> fromString) {
        return new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object == null ? "null" : toString.apply(object);
            }


            @Override
            public T fromString(String string) {
                return fromString.apply(string);
            }
        };
    }


    /**
     * Given a toggle group whose toggles all have user data of type T,
     * maps the selected toggle property to a Var&lt;T>
     */
    @SuppressWarnings("unchecked")
    public static <T> Var<T> mapToggleGroupToUserData(ToggleGroup toggleGroup, Supplier<T> defaultValue) {
        return Var.fromVal(toggleGroup.selectedToggleProperty(), toggleGroup::selectToggle)
                  .mapBidirectional(
                      item -> (T) item.getUserData(),
                      t -> selectFirst(
                          () -> findToggleWithUserData(toggleGroup, t),
                          () -> findToggleWithUserData(toggleGroup, defaultValue.get())
                      )
                          .orElseThrow(() -> new IllegalStateException("Unknown toggle " + t))
                  );
    }


    /** Returns the first non-empty optional in the arguments, or else Optional.empty. */
    @SafeVarargs
    public static <T> Optional<T> selectFirst(Supplier<Optional<T>>... opts) {
        for (Supplier<Optional<T>> optGetter : opts) {
            Optional<T> o = optGetter.get();
            if (o.isPresent()) {
                return o;
            }
        }
        return Optional.empty();
    }


    private static <T> Optional<Toggle> findToggleWithUserData(ToggleGroup toggleGroup, T data) {
        return toggleGroup.getToggles()
                          .stream()
                          .filter(toggle -> toggle.getUserData().equals(data))
                          .findFirst();
    }


    /** Like the other overload, using the setter of the ui property. */
    public static <T> Subscription rewireInit(Property<T> underlying, Property<T> ui) {
        return rewireInit(underlying, ui, ui::setValue);
    }


    /**
     * Binds the underlying property to a source of values (UI property). The UI
     * property is also initialised using a setter.
     *
     * @param underlying The underlying property
     * @param ui         The property exposed to the user (the one in this wizard)
     * @param setter     Setter to initialise the UI value
     * @param <T>        Type of values
     */
    public static <T> Subscription rewireInit(Property<T> underlying,
                                              ObservableValue<? extends T> ui, Consumer<? super T> setter) {
        setter.accept(underlying.getValue());
        return rewire(underlying, ui);
    }


    /** Like rewireInit, with no initialisation. */
    public static <T> Subscription rewire(Property<T> underlying, ObservableValue<? extends T> source) {
        underlying.unbind();
        underlying.bind(source); // Bindings are garbage collected after the popup dies
        return underlying::unbind;
    }


    /**
     * Works out an xpath query that matches the node
     * which was being visited during the failure.
     *
     * <p>The query selects nodes that have exactly the
     * same ancestors than the node in which the last call
     * from the stack trace.
     *
     * @param stackTrace full stack trace
     *
     * @return An xpath expression if possible
     */
    public static Optional<String> stackTraceToXPath(String stackTrace) {
        List<String> lines = Arrays.stream(stackTrace.split("\\n"))
                                   .map(JJT_ACCEPT_PATTERN::matcher)
                                   .filter(Matcher::find)
                                   .map(m -> m.group(1))
                                   .collect(Collectors.toList());

        Collections.reverse(lines);

        return lines.isEmpty() ? Optional.empty() : Optional.of("//" + String.join("/", lines));
    }

    public static void printShortStackTrace(Throwable e) {
        printShortStackTrace(e, System.err);
    }

    public static void printShortStackTrace(Throwable e, PrintStream stream) {
        List<String> frames = new ArrayList<>(Arrays.asList(ExceptionUtils.getStackFrames(e)));
        List<String> myFrames = Arrays.asList(ExceptionUtils.getStackFrames(new Throwable()));
        ExceptionUtils.removeCommonFrames(frames, myFrames);
        frames.forEach(stream::println);
    }


    public static String sanitizeExceptionMessage(Throwable exception) {
        Matcher matcher = EXCEPTION_PREFIX_PATTERN.matcher(exception.getMessage());
        return matcher.matches() ? matcher.group(1) : exception.getMessage();
    }

    /**
     * Works out an xpath query that matches the node
     * which was being visited during the failure.
     *
     * @param e Exception
     *
     * @return A query, if possible.
     *
     * @see #stackTraceToXPath(String)
     */
    public static Optional<String> stackTraceToXPath(Throwable e) {
        return stackTraceToXPath(ExceptionUtils.getStackTrace(e));
    }


    public static RelatedNodesSelector getDefaultRelatedNodesSelector() {
        return node -> node instanceof ScopedNode ? getNameOccurrences((ScopedNode) node)
                                                  : Collections.emptyList();
    }

    private static List<Node> getNameOccurrences(ScopedNode node) {

        // For MethodNameDeclaration the scope is the method scope, which is not the scope it is declared
        // in but the scope it declares! That means that getDeclarations().get(declaration) returns null
        // and no name occurrences are found. We thus look in the parent, but ultimately the name occurrence
        // finder is broken since it can't find e.g. the use of a method in another scope. Plus in case of
        // overloads both overloads are reported to have a usage.

        // Plus this is some serious law of Demeter breaking there...

        Set<NameDeclaration> candidates = new HashSet<>(node.getScope().getDeclarations().keySet());

        Optional.ofNullable(node.getScope().getParent())
                .map(Scope::getDeclarations)
                .map(Map::keySet)
                .ifPresent(candidates::addAll);

        return candidates.stream()
                         .filter(nd -> node.equals(nd.getNode()))
                         .findFirst()
                         .map(nd -> {
                             // nd.getScope() != nd.getNode().getScope()?? wtf?

                             List<NameOccurrence> usages = nd.getNode().getScope().getDeclarations().get(nd);

                             if (usages == null) {
                                 usages = nd.getNode().getScope().getParent().getDeclarations().get(nd);
                             }

                             return usages;
                         })
                         .map(it -> it.stream().<Node>map(NameOccurrence::getLocation).collect(Collectors.toList()))
                         .orElse(Collections.emptyList());
    }


    public static Callback<Class<?>, Object> controllerFactoryKnowing(Object... controllers) {
        return type -> {

            for (Object o : controllers) {
                if (o.getClass().equals(type)) {
                    return o;
                }
            }

            // default behavior for controllerFactory:
            try {
                return type.newInstance();
            } catch (Exception exc) {
                exc.printStackTrace();
                throw new RuntimeException(exc); // fatal, just bail...
            }
        };
    }


    public static Subscription updateProgressOnConsole(Supplier<Double> progressGetter) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> updateProgress(progressGetter.get()), 0, 100, TimeUnit.MILLISECONDS);

        return () -> {
            scheduler.shutdown();
            updateProgress(1.0);
            System.out.println("\r"); // delete
        };
    }

    private static void updateProgress(double progress) {
        final int width = 30; // progress bar width in chars


        StringBuilder builder = new StringBuilder("\r[");
        int i = 0;
        int progressWidth = (int) (progress * width);
        for (; i <= progressWidth; i++) {
            builder.append(".");
        }
        for (; i < width; i++) {
            builder.append(" ");
        }
        builder.append("] ").append(progress * 100).append("%").append(" ");

        System.out.print(builder);
    }


    public static BuilderFactory customBuilderFactory(@NonNull DesignerRoot owner) {
        return type -> {

            boolean needsRoot = Arrays.stream(type.getConstructors()).anyMatch(it -> ArrayUtils.contains(it.getParameterTypes(), DesignerRoot.class));

            if (needsRoot) {
                // Controls that need the DesignerRoot can declare a constructor
                // with a parameter w/ signature @NamedArg("designerRoot") DesignerRoot
                // to be injected with the relevant instance of the app.
                ProxyBuilder<Object> builder = new ProxyBuilder<>(type);
                builder.put("designerRoot", owner);
                return builder;
            } else {
                return null; //use default
            }
        };
    }


    public static String attrToXpathString(Attribute attr) {
        String stringValue = attr.getStringValue();
        Object v = attr.getValue();
        if (v instanceof String || v instanceof Enum) {
            stringValue = "\"" + StringEscapeUtils.escapeJava(stringValue) + "\"";
        } else if (v instanceof Boolean) {
            stringValue = v + "()";
        }
        return String.valueOf(stringValue);
    }


    public static Text makeStyledText(String text, String cssClass) {
        Text matchLabel = new Text(text);
        matchLabel.getStyleClass().add(cssClass);
        return matchLabel;
    }
}
