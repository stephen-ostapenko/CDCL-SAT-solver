package mkn.mathlog.utils

import mkn.mathlog.satSolver.*

fun makeGreedyChoices(formula: Formula): Pair<Formula, MutableMap<VarNameType, Boolean>> {
    val formulaWithoutDuplicates = formula.map { it.distinct() }
    val formulaWithoutClausesWithOppositeLiterals = formulaWithoutDuplicates.filter { clause ->
        clause.size == clause.distinctBy { it.variableName }.size
    }
    var currentFormula = formulaWithoutClausesWithOppositeLiterals
    var precalculatedValues = mutableMapOf<VarNameType, Boolean>()
    while (true) {
        var literals = currentFormula.flatten().toSet()
        for (literal in literals) {
            val oppositeLiteral = Literal(literal.variableName, !literal.hasNegation)
            if (!literals.contains(oppositeLiteral)) {
                precalculatedValues[literal.variableName] = oppositeLiteral.hasNegation
            }
        }
        val cuttedFormula = currentFormula.filter { clause ->
            !clause.any {
                precalculatedValues.containsKey(it.variableName)
            }
        }
        if (currentFormula.size == cuttedFormula.size) {
            break
        }
        currentFormula = cuttedFormula
    }
    return Pair(currentFormula.toMutableList(), precalculatedValues)
}