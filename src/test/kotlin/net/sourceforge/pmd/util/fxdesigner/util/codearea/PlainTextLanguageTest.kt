/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea

import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.FunSpec
import net.sourceforge.pmd.lang.ast.test.matchNode
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil
import net.sourceforge.pmd.util.fxdesigner.util.PlainTextLanguage.PlainTextFile
import java.io.StringReader

class PlainTextLanguagTest : FunSpec({

    test("Test plain text lang is here") {
        LanguageRegistryUtil.plainTextLanguage() shouldNotBe null
    }

    test("Test plain text language parsing") {

        val string = "ohahaha"

        string.parse() should matchNode<PlainTextFile> {
            it.beginLine shouldBe 1
            it.endLine shouldBe 1
            it.beginColumn shouldBe 1
            it.endColumn shouldBe string.length
        }
    }

    test("Test plain text language lines") {

        "abc\nabcd".parse() should matchNode<PlainTextFile> {
            it.beginLine shouldBe 1
            it.endLine shouldBe 2
            it.beginColumn shouldBe 1
            it.endColumn shouldBe 4
        }

        "abc\r\nabcd".parse() should matchNode<PlainTextFile> {
            it.beginLine shouldBe 1
            it.endLine shouldBe 2
            it.beginColumn shouldBe 1
            it.endColumn shouldBe 4
        }
    }

})

private fun String.parse(): PlainTextFile {
    val lang = LanguageRegistryUtil.plainTextLanguage()

    lang.defaultVersion shouldNotBe null

    val parser = lang.defaultVersion.languageVersionHandler.getParser(lang.defaultVersion.languageVersionHandler.defaultParserOptions)
    return parser.parse(":dummy:", StringReader(this)) as PlainTextFile
}
