import mkn.mathlog.satSolver.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestParser {
    private val emptyFormula: Formula = mutableListOf()

    private val f1: Formula = mutableListOf(
        listOf(Literal(1, false)),
        listOf(Literal(1, true)),
        listOf(Literal(1, false), Literal(1, true))
    )

    @Test
    fun `get all vars for empty formula`() {
        assertEquals(listOf(), getAllVars(emptyFormula))
    }

    @Test
    fun `get all vars for formula with one variable`() {
        assertEquals(listOf(1), getAllVars(f1))
    }
}