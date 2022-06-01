data class Literal(val variable: String, val hasNegation: Boolean)
typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>
typealias State = MutableMap<String, VariableInfo>

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)

fun getAllVars(f: Formula): List<String> {
    return f.flatten().map { it.variable }.distinct()
}

fun hasUnassignedVars(allVars: List<String>, state: State): Boolean {
    return state.size < allVars.size
}

fun BCP(f: Formula, state: State): Boolean {
    TODO()
}

fun hasUnresolvedVariables(clause: Clause, state: State): Boolean {
    return clause.any { literal -> !state.containsKey(literal.variable) }
}

fun decideNewValue(f: Formula, state: State, level: Int) {
    val score = mutableMapOf<Literal, Int>()
    f.filter { hasUnresolvedVariables(it, state) }.flatten().forEach { literal ->
        if (!state.containsKey(literal.variable)) {
            val newScore = score.getOrElse(literal, {0} ) + 1
            score[literal] = newScore
        }
    }
    val (decidedLiteral, _) = score.maxByOrNull { (_, score) -> score } ?: return
    state[decidedLiteral.variable] = VariableInfo(!decidedLiteral.hasNegation, null, level)
}

fun CDCL(f: Formula): Pair<Boolean, State> {
    val values = mutableMapOf<String, VariableInfo>()
    val allVars = getAllVars(f)
    if (!BCP(f, values)) {
        return Pair(false, mutableMapOf<String, VariableInfo>())
    }

    var level = 0
    while (hasUnassignedVars(allVars, values)) {
        level++
        decideNewValue(f, values, level)
        while (BCP(f, values) == false) {
            /*(backLevel, learntClause) = analyzeConflict()
            f.add(learntClause)
            if (backLevel < 0) {
                return Pair(false, mutableMapOf<String, VariableInfo>())
            } else {
                goBack(f, values, backLevel)
                level = backLevel
            }*/
        }
    }
    return Pair(true, values)
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Hello, Kotlin/Native!")
    println(isExperimentalMM())
}