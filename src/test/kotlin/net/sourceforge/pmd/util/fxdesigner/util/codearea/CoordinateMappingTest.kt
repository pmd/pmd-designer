/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.*
import org.fxmisc.richtext.CodeArea

class CoordinateMappingTest : FunSpec({

    fun CodeArea.toPmd(offset: Int): TextPos2D =
            getPmdLineAndColumnFromOffset(this, offset)

    fun CodeArea.fromPmd(line: Int, column: Int): Int =
            getOffsetFromPmdPosition(this, line, column)

    test("Map normal text") {

        val codeArea = CodeArea("fof foo")

        codeArea.fromPmd(1, 1) shouldBe 0
        codeArea.fromPmd(1, 2) shouldBe 1

        codeArea.toPmd(0) shouldBe TextPos2D(1, 1)
        codeArea.toPmd(1) shouldBe TextPos2D(1, 2)

    }

    test("Map multiline text from pmd") {

        val codeArea = CodeArea("123456\n12345")

        codeArea.fromPmd(2, 1) shouldBe 7
        codeArea.fromPmd(2, 2) shouldBe 8
    }

    test("Map multiline text to pmd") {

        val codeArea = CodeArea("123456\n12345")

        codeArea.toPmd(6) shouldBe TextPos2D(1, 7)
        codeArea.toPmd(7) shouldBe TextPos2D(2, 1)
        codeArea.toPmd(8) shouldBe TextPos2D(2, 2)
    }

    test("Map tabs") {

        val codeArea = CodeArea("\tfof foo")
        //--------

        codeArea.fromPmd(1, 1) shouldBe 0
        codeArea.fromPmd(1, 2) shouldBe 0
        codeArea.fromPmd(1, 7) shouldBe 0
        codeArea.fromPmd(1, 8) shouldBe 0
        codeArea.fromPmd(1, 9) shouldBe 1
        codeArea.fromPmd(1, 10) shouldBe 2

        codeArea.toPmd(0) shouldBe TextPos2D(1, 1)
        codeArea.toPmd(1) shouldBe TextPos2D(1, 9)
        codeArea.toPmd(2) shouldBe TextPos2D(1, 10)
    }

    test("All tabs corner case") {

        val codeArea = CodeArea("\t\t\t")

        codeArea.fromPmd(1, 1) shouldBe 0
        codeArea.fromPmd(1, 2) shouldBe 0

        codeArea.toPmd(0) shouldBe TextPos2D(1, 1)
        codeArea.toPmd(1) shouldBe TextPos2D(1, 9)
        codeArea.toPmd(2) shouldBe TextPos2D(1, 17)
    }

    test("Empty corner case") {

        val codeArea = CodeArea("")

        codeArea.fromPmd(1, 1) shouldBe 0

        shouldThrow<IndexOutOfBoundsException> {
            codeArea.fromPmd(1, 2)
        }

        codeArea.toPmd(0) shouldBe TextPos2D(1, 1)

        shouldThrow<IndexOutOfBoundsException> {
            codeArea.toPmd(2)
        }
    }

})
