import mkn.mathlog.satSolver.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFormula {
    private val emptyFormula: Formula = mutableListOf()

    private val f1: Formula = mutableListOf(
        listOf(Literal(1, false)),
        listOf(Literal(1, true)),
        listOf(Literal(1, false), Literal(1, true))
    )

    private val f2: Formula = mutableListOf(
        listOf(Literal(2, false), Literal(3, true)),
        listOf(Literal(2, true), Literal(1, false)),
        listOf(Literal(2, true), Literal(1, true)),
        listOf(Literal(1, false), Literal(2, false))
    )

    @Test
    fun `get all vars for empty formula`() {
        assertEquals(listOf(), getAllVars(emptyFormula))
    }

    @Test
    fun `get all vars for formula with one variable`() {
        assertEquals(listOf(1), getAllVars(f1))
    }

    @Test
    fun `get all vars for formula with > 1 variables`() {
        assertEquals(listOf(2, 3, 1), getAllVars(f2))
    }
}