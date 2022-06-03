package mkn.mathlog.satSolver

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mkn.mathlog.utils.analyzeConflictWithMinCut
import kotlin.math.floor

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

object AnalyzeConflictWatcher {
    var lastLevel = 0
    var triesCount = 0
    var lastResult: Clause = listOf()
    var forceClassic = false
    var triesThreshold = 256
    var learntClausesCountThreshold = 512

    fun nextTriesThreshold() {
        triesThreshold = floor(triesThreshold * 1.24).toInt()
    }

    fun nextLearntClausesCountThreshold() {
        learntClausesCountThreshold = floor(learntClausesCountThreshold * 1.4142).toInt()
    }
}

fun analyzeConflict(f: Formula, state: State, level: Int, variablesCount: Int, clausesCount: Int): Pair<Int, Clause> {
    //println("analyzing on $level with ${f.size} clauses")
    if (level == 0) {
        return Pair(-1, listOf())
    }

    var result: Pair<Int, Clause>? = runBlocking {
        withTimeoutOrNull(50L) {
            var currentClause = f.find { !clauseHasUnassignedVars(it, state) && it.all { literal ->
                state[literal.variableName]?.value == literal.hasNegation
            }} ?: throw IllegalArgumentException()

            while (!oneLiteralAtLevel(currentClause, state, level)) {
                if (!isActive && !AnalyzeConflictWatcher.forceClassic) {
                    return@withTimeoutOrNull null
                }

                val selectedLiteral = currentClause.find {
                        literal -> state[literal.variableName]?.level == level && state[literal.variableName]?.antecedent != null
                } ?: break
                val previousClauseIndex = state[selectedLiteral.variableName]?.antecedent ?: throw IllegalArgumentException()
                currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variableName)
            }

            val newLevel = currentClause.map { state[it.variableName]?.level ?: 0 }.filter { it != level }.
                maxByOrNull { it } ?: 0

            Pair(newLevel, currentClause)
        }
    }
    if (result == null) {
        result = analyzeConflictWithMinCut(f, state, level, variablesCount)

        if (level == AnalyzeConflictWatcher.lastLevel) {
            //AnalyzeConflictWatcher.triesCount++
        } else {
            //AnalyzeConflictWatcher.triesCount = 1
        }
        AnalyzeConflictWatcher.lastLevel = level
        AnalyzeConflictWatcher.forceClassic = (result.second == AnalyzeConflictWatcher.lastResult)
        AnalyzeConflictWatcher.lastResult = result.second

        /*if (AnalyzeConflictWatcher.triesCount >= 5) {
            result = 0 to result.second

            AnalyzeConflictWatcher.lastLevel = 0
            AnalyzeConflictWatcher.triesCount = 0
        }*/

        //println("MinCut")
    } else {
        AnalyzeConflictWatcher.lastLevel = 0
        //AnalyzeConflictWatcher.triesCount = 0
        AnalyzeConflictWatcher.forceClassic = false

        //println("Classic")
    }

    AnalyzeConflictWatcher.triesCount++
    ////println()
    if (AnalyzeConflictWatcher.triesCount == AnalyzeConflictWatcher.triesThreshold) {
        result = 0 to result.second
        AnalyzeConflictWatcher.triesCount = 0
        AnalyzeConflictWatcher.nextTriesThreshold()
        //println("=============================================================================")
    }

    if (f.size - clausesCount >= AnalyzeConflictWatcher.learntClausesCountThreshold) {
        result = 0 to result.second
        shrinkFormula(f, clausesCount)
        AnalyzeConflictWatcher.nextLearntClausesCountThreshold()
        //println("*****************************************************************************")
    }

    return result
}

fun shrinkFormula(formula: Formula, clausesCount: Int) {
    val initFormula: Formula = mutableListOf()
    val learntClauses: Formula = mutableListOf()

    for (i in 0 until clausesCount) {
        initFormula.add(formula[i])
    }
    for (i in clausesCount until formula.size) {
        learntClauses.add(formula[i])
    }
    formula.clear()

    initFormula.shuffle()
    initFormula.map { it.shuffled() }
    formula += initFormula

    formula += learntClauses.filter { it.size <= 5 }.shuffled()
}

fun runCDCLSolver(f: Formula, variablesCount: Int, clausesCount: Int): Pair<Boolean, State> {
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
            val (backLevel, learntClause) = analyzeConflict(f, state, level, variablesCount, clausesCount)
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