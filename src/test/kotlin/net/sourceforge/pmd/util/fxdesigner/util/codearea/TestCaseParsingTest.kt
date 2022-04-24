/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import net.sourceforge.pmd.lang.ast.test.IntelliMarker
import net.sourceforge.pmd.lang.document.TextRegion
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestXmlParser

class TestCaseParsingTest : IntelliMarker, FunSpec({


    test("Map normal text") {

        val xmlTest = """<?xml version="1.0" encoding="UTF-8"?>
            <test-data
                xmlns="http://pmd.sourceforge.net/rule-tests"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://pmd.sourceforge.net/rule-tests http://pmd.sourceforge.net/rule-tests_1_0_0.xsd">

                <test-code>
                    <description>simple failure case</description>
                    <expected-problems>1</expected-problems>
                    <expected-linenumbers>3</expected-linenumbers>
                    <code><![CDATA[
            public class UseShortArrayExample {
                void foo() {
                    int[] x = new int[] {1,2,3};
                }
            }
                    ]]></code>
                </test-code>

                <test-code>
                    <description>ok</description>
                    <expected-problems>0</expected-problems>
                    <code><![CDATA[
                        public class UseShortArrayExample {
                            void foo() {
                                int[] x = {1,2,3};
                            }
                        }
                    ]]></code>
                </test-code>

                <test-code>
                    <description>case with two initializers</description>
                    <expected-problems>2</expected-problems>
                    <expected-linenumbers>3,3</expected-linenumbers>
                    <expected-messages>
                        <message>a</message>
                        <message>b</message>
                    </expected-messages>
                    <code><![CDATA[
            public class UseShortArrayExample {
                void foo() {
                    int ar[] = new int[] { 1,2,3}, foo[] = new int[] { 4, 5, 6 };
                }
            }
                    ]]></code>
                </test-code>

            </test-data>

        """.trimIndent()

        val tc = TestXmlParser.parseXmlTests(xmlTest, ObservableRuleBuilder())

        tc.stash.size shouldBe 3
        tc.stash[0].apply {
            description shouldBe "simple failure case"
            expectedViolations should haveSize(1)

            expectedViolations[0].apply {
                message shouldBe null
                // this is the third line
                region shouldBe TextRegion.fromBothOffsets(77, 148)
            }

        }

        tc.stash[1].apply {
            description shouldBe "ok"
            expectedViolations should haveSize(0)
        }

        tc.stash[2].apply {
            description shouldBe "case with two initializers"
            expectedViolations should haveSize(2)

            expectedViolations[0].apply {
                message shouldBe "a"
                // this is the third line
                region shouldBe TextRegion.fromBothOffsets(77, 148)
            }


            expectedViolations[1].apply {
                message shouldBe "b"
                // this is the third line
                region shouldBe TextRegion.fromBothOffsets(77, 148)
            }

            source shouldBe """public class UseShortArrayExample {
                void foo() {
                    int ar[] = new int[] { 1,2,3}, foo[] = new int[] { 4, 5, 6 };
                }
            }"""
        }


    }


})
