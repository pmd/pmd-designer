/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.sourceforge.pmd.lang.LanguageProcessorRegistry
import net.sourceforge.pmd.lang.PlainTextLanguage.PlainTextFile
import net.sourceforge.pmd.lang.PmdCapableLanguage
import net.sourceforge.pmd.lang.ast.Parser
import net.sourceforge.pmd.lang.ast.SemanticErrorReporter
import net.sourceforge.pmd.lang.test.ast.IntelliMarker
import net.sourceforge.pmd.lang.test.ast.matchNode
import net.sourceforge.pmd.lang.document.TextDocument
import net.sourceforge.pmd.lang.document.TextRange2d
import net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry

class PlainTextLanguageTest : IntelliMarker, FunSpec({

    test("Test plain text lang is here") {
        AuxLanguageRegistry.plainTextLanguage() shouldNotBe null
    }

    test("Test plain text language parsing") {

        val string = "oha"

        string.parse() should matchNode<PlainTextFile> {
            it.reportLocation.toRange2d() shouldBe TextRange2d.range2d(
                1, 1, 1, string.length + 1
            )
        }
    }

    test("Test plain text language lines (LF)") {
        "abc\nabcd".parse() should matchNode<PlainTextFile> {
            it.reportLocation.toRange2d() shouldBe TextRange2d.range2d(
                1, 1, 2, 5
            )
        }
    }

    test("Test plain text language lines (CRLF)") {
        "abc\r\nabcd".parse() should matchNode<PlainTextFile> {
            it.reportLocation.toRange2d() shouldBe TextRange2d.range2d(
                1, 1, 2, 5
            )
        }
    }

})

private fun String.parse(): PlainTextFile {
    val lang : PmdCapableLanguage = AuxLanguageRegistry.plainTextLanguage()

    lang.defaultVersion shouldNotBe null

    val processor = lang.createProcessor(lang.newPropertyBundle())
    val parser = processor.services().parser
    val doc = TextDocument.readOnlyString(this, lang.defaultVersion)
    val task = Parser.ParserTask(
        doc,
        SemanticErrorReporter.noop(),
        LanguageProcessorRegistry.singleton(processor)
    )
    return parser.parse(task) as PlainTextFile
}
