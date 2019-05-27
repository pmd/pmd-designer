/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea

import io.kotlintest.matchers.haveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestXmlParser

class TestCaseParsingTest : FunSpec({


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

        val tc = TestXmlParser.parseXmlTests(xmlTest) { throw AssertionError(it) }

        tc.stash.size shouldBe 3
        tc.stash[0].also {
            it.description shouldBe "simple failure case"
            it.expectedViolations should haveSize(1)

            it.expectedViolations[0].also {
                it.exactRange shouldBe false
                it.message shouldBe null
                (PmdCoordinatesSystem.TextPos2D(3, 0) in it.range) shouldBe true
            }

        }

        tc.stash[1].also {
            it.description shouldBe "ok"
            it.expectedViolations should haveSize(0)
        }

        tc.stash[2].also {
            it.description shouldBe "case with two initializers"
            it.expectedViolations should haveSize(2)

            it.expectedViolations[0].also {
                it.exactRange shouldBe false
                it.message shouldBe null
                (PmdCoordinatesSystem.TextPos2D(3, 0) in it.range) shouldBe true
                (PmdCoordinatesSystem.TextPos2D(3, 10) in it.range) shouldBe true
            }


            it.expectedViolations[1].also {
                it.exactRange shouldBe false
                it.message shouldBe null
                (PmdCoordinatesSystem.TextPos2D(3, 10) in it.range) shouldBe true
            }

            it.source shouldBe """public class UseShortArrayExample {
                void foo() {
                    int ar[] = new int[] { 1,2,3}, foo[] = new int[] { 4, 5, 6 };
                }
            }"""
        }


    }


})
