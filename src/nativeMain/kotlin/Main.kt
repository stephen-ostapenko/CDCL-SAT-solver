import platform.posix.fopen

data class Literal(val variable: String, val hasNegation: Boolean)
typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>
typealias State = MutableMap<String, VariableInfo>

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)

fun getAllVars(f: Formula): List<String> {
    return f.flatten().map { it.variable }.distinct()
}

fun formulaHasUnassignedVars(allVars: List<String>, state: State): Boolean {
    return state.size < allVars.size
}

fun clauseHasUnassignedVars(clause: Clause, state: State): Boolean {
    return clause.any { !state.containsKey(it.variable) }
}

fun BCP(f: Formula, state: State, level: Int): Boolean {
    while (true) {
        var foundUnitClause = false
        for ((id, clause) in f.withIndex()) {
            val unresolvedLiterals = clause.filter { !state.containsKey(it.variable) }
            if (unresolvedLiterals.size == 1) {
                foundUnitClause = true
                state[unresolvedLiterals.first().variable] = VariableInfo(unresolvedLiterals.first().hasNegation, id, level)
            }
        }

        if (!foundUnitClause) {
            break
        }
    }

    return f.all { clauseHasUnassignedVars(it, state) || it.any { literal ->
        state.get(literal.variable)?.value == literal.hasNegation
    }}
}

fun decideNewValue(f: Formula, state: State, level: Int) {
    val score = mutableMapOf<Literal, Int>()
    f.filter { clauseHasUnassignedVars(it, state) }.flatten().forEach { literal ->
        if (!state.containsKey(literal.variable)) {
            val newScore = score.getOrElse(literal, {0} ) + 1
            score[literal] = newScore
        }
    }

    val (decidedLiteral, _) = score.maxByOrNull { (_, score) -> score } ?: return
    state[decidedLiteral.variable] = VariableInfo(!decidedLiteral.hasNegation, null, level)
}

fun goBack(f: Formula, state: State, newLevel: Int): State {
    return state.filter { (_, info) -> (info.level <= newLevel) }.toMutableMap()
}

fun oneLiteralAtLevel(clause: Clause, state: State, level: Int): Boolean {
    return clause.count { state.get(it.variable)?.level == level } == 1
}

fun resolve(clause1: Clause, clause2: Clause, v: String): Clause {
    return (clause1 + clause2).filter { it.variable != v }
}

fun analyzeConflict(f: Formula, state: State, level: Int): Pair<Int, Clause> {
    if (level == 0) {
        return Pair(-1, listOf())
    }
    var currentClause = f.find { !clauseHasUnassignedVars(it, state) && it.all { literal ->
        state.get(literal.variable)?.value != literal.hasNegation
    }} ?: throw IllegalArgumentException()

    while (!oneLiteralAtLevel(currentClause, state, level)) {
        val selectedLiteral = currentClause.find { literal -> state.get(literal.variable)?.level == level } ?: throw IllegalArgumentException()
        val previousClauseIndex = state.get(selectedLiteral.variable)?.antecedent ?: throw IllegalArgumentException()
        currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variable)
    }

    val newLevel = currentClause.map { state.get(it.variable)?.level ?: 0 }.maxByOrNull {
        if (it == level) 0 else it
    } ?: -1
    return Pair(newLevel, currentClause)
}

fun CDCL(f: Formula): Pair<Boolean, State> {
    var state = mutableMapOf<String, VariableInfo>()
    val allVars = getAllVars(f)
    if (!BCP(f, state, 0)) {
        return Pair(false, mutableMapOf())
    }

    var level = 0
    while (formulaHasUnassignedVars(allVars, state)) {
        level++
        decideNewValue(f, state, level)
        while (BCP(f, state, level) == false) {
            val (backLevel, learntClause) = analyzeConflict(f, state, level)
            f.add(learntClause)
            if (backLevel < 0) {
                return Pair(false, mutableMapOf())
            } else {
                state = goBack(f, state, backLevel)
                level = backLevel
            }
        }
    }
    return Pair(true, state)
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Hello, Kotlin/Native!")
    println(isExperimentalMM())
}