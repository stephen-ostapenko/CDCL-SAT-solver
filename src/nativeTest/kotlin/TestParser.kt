import mkn.mathlog.satSolver.Formula
import mkn.mathlog.satSolver.Literal
import mkn.mathlog.utils.DIMACSParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestParser {
    @Test
    fun testOk() {
        val input = """
            c sdfasdf fgsdf
            c sdfgs sdfsd
            p      cnf           3       4
            c fgsdfg sdfsdf sdf
            c fdgadf ffff
            1 -3 0
            c sdfsdfdsfsdf dfsdf
            0 0 2      0 3 -1
            c sdfsdfsdf  dsfsdsdf sdfsdf
            1 -2 0
            c sdfsdf
            -1 3 2
            c c    cc
        """.trimIndent()

        val formula: Formula = mutableListOf(
            listOf(Literal(1, false), Literal(3, true)),
            listOf(Literal(2, false), Literal(3, false), Literal(1, true)),
            listOf(Literal(1, false), Literal(2, true)),
            listOf(Literal(1, true), Literal(3, false), Literal(2, false)),
        )

        val parser = DIMACSParser()
        parser.parseText(input)

        assertEquals(formula, parser.getFormula())
    }

    @Test
    fun testOk2() {
        val input = """
            c sdfasdfas
            c asdfasdf adfgagasdg asdgasfg a
            c kek
            
            1                2           3
            
            1               2    -3
            
            
            1               -2           3
                    1       -2           -3
            
                     -1          2          3
            -1          2            -3
            
            
            
                        -1           -2          3
            
              -1            -2           -3
              
              
              c dgadg afgad gadfg adf afg dfg a fga fga fgd
              c dfagafg afgasg asgf asga sdg asdfg asg
              c adsgfadga afga ga fg afg afg a fg afg ag
        """.trimIndent()

        val formula: Formula = mutableListOf(
            listOf(Literal(1, false), Literal(2, false), Literal(3, false)),
            listOf(Literal(1, false), Literal(2, false), Literal(3, true)),
            listOf(Literal(1, false), Literal(2, true), Literal(3, false)),
            listOf(Literal(1, false), Literal(2, true), Literal(3, true)),
            listOf(Literal(1, true), Literal(2, false), Literal(3, false)),
            listOf(Literal(1, true), Literal(2, false), Literal(3, true)),
            listOf(Literal(1, true), Literal(2, true), Literal(3, false)),
            listOf(Literal(1, true), Literal(2, true), Literal(3, true)),
        )

        val parser = DIMACSParser()
        parser.parseText(input)

        assertEquals(formula, parser.getFormula())
    }

    @Test
    fun testFileWithoutParameters() {
        val input = """
            c sdfasdf fgsdf
            c sdfgs sdfsd
            
            c fgsdfg sdfsdf sdf
            c fdgadf ffff
            1 -3 0
            c sdfsdfdsfsdf dfsdf
            
            0 0 2      0 3 -1
            c sdfsdfsdf  dsfsdsdf sdfsdf
            1 -2 0
            c sdfsdf
            
            -1 3 2
            c c    cc
        """.trimIndent()

        val formula: Formula = mutableListOf(
            listOf(Literal(1, false), Literal(3, true)),
            listOf(Literal(2, false), Literal(3, false), Literal(1, true)),
            listOf(Literal(1, false), Literal(2, true)),
            listOf(Literal(1, true), Literal(3, false), Literal(2, false)),
        )

        val parser = DIMACSParser()
        parser.parseText(input)

        assertEquals(formula, parser.getFormula())
    }

    @Test
    fun testEmptyFile() {
        val input = """
            c comment
            c another comment
        """.trimIndent()

        val parser = DIMACSParser()
        parser.parseText(input)

        assertEquals(mutableListOf(), parser.getFormula())
    }

    @Test
    fun testMultipleTags() {
        val input = """
            p cnf 3 2
            1 2 3
            -1 -2 -3
            p cnf 3 2
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testRubbishInput() {
        val input = """
            p cnf 1 1
            sdfsfsdfsd
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testWrongTag() {
        val input = """
            p kek 2 2
            1 2
            -1 -2
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testVariablesCountOutOfBounds() {
        val input = """
            p cnf -1 2
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testClausesCountOutOfBounds() {
        val input = """
            p cnf 2 -1
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testEmptyClause() {
        val input = """
            c sdfasdf fgsdf
            c sdfgs sdfsd
            0 0 0
            c fgsdfg sdfsdf sdf
            c fdgadf ffff
            1 -3 0
            c sdfsdfdsfsdf dfsdf
            0 0 2      0 3 -1
            c sdfsdfsdf  dsfsdsdf sdfsdf
            1 -2 0
            c sdfsdf
            -1 3 2
            c c    cc
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testRubbishClause() {
        val input = """
            p cnf 3 2
            1 2 3
            1 kek 2
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testVariablesCountLess() {
        val input = """
            p cnf 2 2
            1 2 3
            -1 -2 -3
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testVariablesCountGreater() {
        val input = """
            p cnf 5 2
            1 2 3
            -1 -2 -3
        """.trimIndent()

        val formula: Formula = mutableListOf(
            listOf(Literal(1, false), Literal(2, false), Literal(3, false)),
            listOf(Literal(1, true), Literal(2, true), Literal(3, true)),
        )

        val parser = DIMACSParser()
        parser.parseText(input)

        assertEquals(formula, parser.getFormula())
    }

    @Test
    fun testClausesCountLess() {
        val input = """
            p cnf 3 1
            1 2 3
            -1 -2 -3
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }

    @Test
    fun testClausesCountGreater() {
        val input = """
            p cnf 3 4
            1 2 3
            -1 -2 -3
        """.trimIndent()

        val parser = DIMACSParser()
        assertFailsWith(DIMACSParser.DIMACSException::class) {
            parser.parseText(input)
        }
    }
}