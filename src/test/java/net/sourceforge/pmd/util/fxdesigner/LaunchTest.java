package net.sourceforge.pmd.util.fxdesigner;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;

import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
public class LaunchTest {


    private DesignerRoot root;
    private Scene scene;

    /**
     * Will be called with {@code @Before} semantics, i. e. before each test method.
     *
     * @param stage - Will be injected by the test runner.
     */
    @Start
    private void start(Stage stage) throws IOException {
        Designer designer = new Designer();
        designer.start(stage);
        root = designer.getDesignerRoot();
        scene = root.getMainStage().getScene();
    }

    /**
     * @param robot - Will be injected by the test runner.
     */
    @Test
    public void should_contain_button_with_text(FxRobot robot) {
        FxAssert.verifyThat("#main-horizontal-split-pane", NodeMatchers.isNotNull());
    }
//
//    /**
//     * @param robot - Will be injected by the test runner.
//     */
//    @Test
//    void when_button_is_clicked_text_changes(FxRobot robot) {
//        // when:
//        robot.clickOn(".button");
//
//        // then:
//        FxAssert.verifyThat(button, LabeledMatchers.hasText("clicked!"));
//        // or (lookup by css id):
//        FxAssert.verifyThat("#myButton", LabeledMatchers.hasText("clicked!"));
//        // or (lookup by css class):
//        FxAssert.verifyThat(".button", LabeledMatchers.hasText("clicked!"));
//    }

}
