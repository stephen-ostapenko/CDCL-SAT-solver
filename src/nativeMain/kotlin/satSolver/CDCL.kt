package mkn.mathlog.satSolver

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)
typealias State = MutableMap<VarNameType, VariableInfo>

fun formulaHasUnassignedVars(allVars: List<VarNameType>, state: State): Boolean =
    state.size < allVars.size

fun clauseHasUnassignedVars(clause: Clause, state: State): Boolean =
    clause.any { !state.containsKey(it.variableName) }

fun BCP(f: Formula, state: State, level: Int): Boolean {
    while (true) {
        var foundUnitClause = false
        for ((id, clause) in f.withIndex()) {
            if (clause.any { literal -> !literal.hasNegation == state[literal.variableName]?.value }) {
                continue
            }
            val unresolvedLiterals = clause.filter { !state.containsKey(it.variableName) }
            if (unresolvedLiterals.size == 1) {
                foundUnitClause = true
                state[unresolvedLiterals.first().variableName] = VariableInfo(!unresolvedLiterals.first().hasNegation, id, level)
            }
        }

        if (!foundUnitClause) {
            break
        }
    }

    return f.all { clauseHasUnassignedVars(it, state) || it.any { literal ->
        state[literal.variableName]?.value != literal.hasNegation
    }}
}

fun decideNewValue(f: Formula, state: State, level: Int) {
    val score = mutableMapOf<Literal, Int>()
    f.filter { clauseHasUnassignedVars(it, state) }.flatten().forEach { literal ->
        if (!state.containsKey(literal.variableName)) {
            val newScore = score.getOrElse(literal) { 0 } + 1
            score[literal] = newScore
        }
    }

    val (decidedLiteral, _) = score.maxByOrNull { (_, score) -> score } ?: return
    state[decidedLiteral.variableName] = VariableInfo(!decidedLiteral.hasNegation, null, level)
}

fun goBack(state: State, newLevel: Int): State =
        state.filter { (_, info) -> (info.level <= newLevel) }.toMutableMap()

fun oneLiteralAtLevel(clause: Clause, state: State, level: Int): Boolean =
        clause.count { state[it.variableName]?.level == level } == 1

fun analyzeConflict(f: Formula, state: State, level: Int): Pair<Int, Clause> {
    if (level == 0) {
        return Pair(-1, listOf())
    }
    var currentClause = f.find { !clauseHasUnassignedVars(it, state) && it.all { literal ->
        state[literal.variableName]?.value == literal.hasNegation
    }} ?: throw IllegalArgumentException()

    while (!oneLiteralAtLevel(currentClause, state, level)) {
        val selectedLiteral = currentClause.find {
            literal -> state[literal.variableName]?.level == level && state[literal.variableName]?.antecedent != null
        } ?: break
        val previousClauseIndex = state[selectedLiteral.variableName]?.antecedent ?: throw IllegalArgumentException()
        currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variableName)
    }

    val newLevel = currentClause.map { state[it.variableName]?.level ?: 0 }.filter { it != level }.
    maxByOrNull { it } ?: 0
    return Pair(newLevel, currentClause)
}

fun runCDCLSolver(f: Formula): Pair<Boolean, State> {
    var state = mutableMapOf<VarNameType, VariableInfo>()
    val allVars = getAllVars(f)
    if (!BCP(f, state, 0)) {
        return Pair(false, mutableMapOf())
    }

    var level = 0
    while (formulaHasUnassignedVars(allVars, state)) {
        level++
        decideNewValue(f, state, level)
        while (!BCP(f, state, level)) {
            val (backLevel, learntClause) = analyzeConflict(f, state, level)
            f.add(learntClause)
            if (backLevel < 0) {
                return Pair(false, mutableMapOf())
            } else {
                state = goBack(state, backLevel)
                level = backLevel
            }
        }
    }
    return Pair(true, state)
}