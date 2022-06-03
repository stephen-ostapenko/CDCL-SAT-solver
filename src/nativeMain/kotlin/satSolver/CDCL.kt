package mkn.mathlog.satSolver

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mkn.mathlog.utils.analyzeConflictWithMinCut
import kotlin.math.floor

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)
typealias VariablesState = MutableMap<VarNameType, VariableInfo>
typealias WatchedLiterals = MutableList<Pair<Int, Int>>

class CDCLSolver(formula: Formula) {
    private var f = formula
    private var variablesInfo = mutableMapOf<VarNameType, VariableInfo>()
    private var clausesInfo: WatchedLiterals = mutableListOf()
    private val unitClauses = mutableListOf<Int>()
    private var referencesOnVariable = mutableMapOf<VarNameType, MutableSet<Pair<Int, Int>>>()
    private var literalsScore = mutableMapOf<Literal, Double>()
    private val updates = mutableListOf<VarNameType>()
    private var scoreChangeIteration = 0

    private fun initClause(clauseID: Int) {
        clausesInfo.add(Pair(0, 1))
        if (f[clauseID].isNotEmpty()) {
            referencesOnVariable.getOrPut(f[clauseID][0].variableName) { mutableSetOf() }.add(Pair(clauseID, 0))
        }
        if (f[clauseID].size > 1) {
            referencesOnVariable.getOrPut(f[clauseID][1].variableName) { mutableSetOf() }.add(Pair(clauseID, 1))
        }
        val oldVariablesInfo = mutableMapOf<VarNameType, VariableInfo>()
        oldVariablesInfo.putAll(variablesInfo)
        variablesInfo.clear()
        for (variable in updates) {
            val variableInfo = oldVariablesInfo[variable] ?: throw IllegalArgumentException("error in init clause function: $variable")
            variablesInfo[variable] = variableInfo
            if (updateClause(variable, variableInfo.value, clauseID) != null) {
                unitClauses.add(clauseID)
            }
        }
        unitClauses.add(clauseID)

        unitClauses.add(clauseID)

        f[clauseID].forEach { literal ->
            val newScore = literalsScore.getOrElse(literal) { 0.0 } + 1.0
            literalsScore[literal] = newScore
        }
    }

    init {
        f.indices.forEach {
            initClause(it)
        }
    }

    private fun getVariableInClauseStatus(clauseID: Int, index: Int): VariableInClauseStatus {
        val (variable, hasNegation) = f[clauseID][index]
        return when (variablesInfo[variable]?.value) {
            null -> {
                VariableInClauseStatus.UNRESOLVED
            }
            !hasNegation -> {
                VariableInClauseStatus.CORRECT
            }
            else -> {
                VariableInClauseStatus.INCORRECT
            }
        }
    }

    private fun clauseStatus(clauseID: Int): ClauseStatus {
        var satisfied = false
        var unassignedCount = 0
        fun processWatchedLiteral(index: Int) {
            if (index < 0 || index >= f[clauseID].size) {
                return
            }
            val status = getVariableInClauseStatus(clauseID, index)
            if (status == VariableInClauseStatus.CORRECT) {
                satisfied = true
            } else if (status == VariableInClauseStatus.UNRESOLVED) {
                unassignedCount++
            }
        }
        processWatchedLiteral(clausesInfo[clauseID].first)
        processWatchedLiteral(clausesInfo[clauseID].second)
        if (satisfied) {
            return ClauseStatus.SATISFIED
        }
        return when (unassignedCount) {
            0 -> ClauseStatus.UNSATISFIED
            1 -> ClauseStatus.UNIT
            2 -> ClauseStatus.UNRESOLVED
            else -> throw IllegalArgumentException("wrong value of unassigned count")
        }
    }

    private fun getUnassignedVariable(clauseID: Int): Pair<VarNameType, Int> {
        val (watchedIndex1, watchedIndex2) = clausesInfo[clauseID]
        if (watchedIndex1 >= 0 && watchedIndex1 < f[clauseID].size && getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.UNRESOLVED) {
            return Pair(f[clauseID][watchedIndex1].variableName, watchedIndex1)
        }
        return Pair(f[clauseID][watchedIndex2].variableName, watchedIndex2)
    }

    fun updateClause(variable: VarNameType, value: Boolean, clauseID: Int): Int? {
        if (f[clauseID].getVariable(clausesInfo[clauseID].first) != variable && f[clauseID].getVariable(clausesInfo[clauseID].second) != variable) {
            return null
        }
        var (watchedIndex1, watchedIndex2) = clausesInfo[clauseID]
        if (f[clauseID].getVariable(watchedIndex1) != variable) {
            watchedIndex1 = watchedIndex2.also { watchedIndex2 = watchedIndex1 }
        }
        if (f[clauseID].getValue(watchedIndex1) != value) {
            watchedIndex1 = maxOf(watchedIndex1, watchedIndex2) + 1
            while (watchedIndex1 < f[clauseID].size && getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.INCORRECT) {
                watchedIndex1++
            }
            if (watchedIndex1 < f[clauseID].size && getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.UNRESOLVED) {
                referencesOnVariable.getOrPut(f[clauseID][watchedIndex1].variableName) { mutableSetOf() }
                    .add(Pair(clauseID, watchedIndex1))
            }
            if (watchedIndex1 > watchedIndex2) {
                watchedIndex1 = watchedIndex2.also { watchedIndex2 = watchedIndex1 }
            }
            clausesInfo[clauseID] = Pair(watchedIndex1, watchedIndex2)
            val newStatus = clauseStatus(clauseID)
            if (newStatus == ClauseStatus.UNIT) {
                unitClauses.add(clauseID)
            } else if (newStatus == ClauseStatus.UNSATISFIED) {
                return clauseID
            }
        }
        return null
    }

    private fun assign(variable: VarNameType, value: Boolean, level: Int, antecedent: Int?): Int? {
        variablesInfo[variable] = VariableInfo(value, antecedent, level)
        updates.add(variable)
        var unsat: Int? = null
        for ((clauseID, _) in referencesOnVariable.getOrElse(variable) { mutableListOf() }) {
            val result = updateClause(variable, value, clauseID)
            if (result != null) {
                unsat = result
            }
        }
        return unsat
    }

    private fun decideNewValue(level: Int) {
        val (decidedLiteral, _) = literalsScore.filter { (literal, _) -> variablesInfo[literal.variableName] == null }
            .maxByOrNull { (_, score) -> score } ?: return

        assign(decidedLiteral.variableName, !decidedLiteral.hasNegation, level, null)
    }

    private fun BCP(level: Int): Int? {
        while (unitClauses.isNotEmpty()) {
            val clauseIndex = unitClauses.last()
            unitClauses.removeLast()
            val status = clauseStatus(clauseIndex)
            if (status == ClauseStatus.UNSATISFIED) {
                return clauseIndex
            }
            if (status != ClauseStatus.UNIT) {
                continue
            }
            val (variable, position) = getUnassignedVariable(clauseIndex)
            val value: Boolean = f[clauseIndex].getValue(position) ?: throw IllegalArgumentException("in BCP")
            return assign(variable, value, level, clauseIndex) ?: continue
        }
        return null
    }

    fun oneLiteralAtLevel(clause: Clause, state: VariablesState, level: Int): Boolean =
        clause.count { state[it.variableName]?.level == level } == 1

    object AnalyzeConflictWatcher {
        var forceClassic = false
        var analyzesCount = 0
        var analyzesCountThreshold = 256
        var learntClausesCountThreshold = 512

        fun nextAnalyzesCountThreshold() {
            analyzesCountThreshold = floor(analyzesCountThreshold * 1.24).toInt()
        }

        fun nextLearntClausesCountThreshold() {
            learntClausesCountThreshold = floor(learntClausesCountThreshold * 1.4142).toInt()
        }
    }


    fun analyzeConflict(level: Int, conflictClauseID: Int, variablesCount: Int, clausesCount: Int): Pair<Int, Clause?> {
        println("analyze on level $level")

        if (level == 0) {
            return Pair(-1, null)
        }

        var result: Pair<Int, Clause?>? = runBlocking {
            withTimeoutOrNull(50L) {
                var currentClause = f[conflictClauseID]
                while (!oneLiteralAtLevel(currentClause, variablesInfo, level)) {
                    if (!isActive && !AnalyzeConflictWatcher.forceClassic) {
                        return@withTimeoutOrNull null
                    }

                    val selectedLiteral = currentClause.find {
                            literal -> variablesInfo[literal.variableName]?.level == level && variablesInfo[literal.variableName]?.antecedent != null
                    } ?: break
                    val previousClauseIndex = variablesInfo[selectedLiteral.variableName]?.antecedent ?: throw IllegalArgumentException("analyze conflict")
                    currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variableName)
                }

                val newLevel = currentClause.map { variablesInfo[it.variableName]?.level ?: 0 }.filter { it != level }.
                    maxByOrNull { it } ?: 0

                Pair(newLevel, currentClause)
            }
        }
        if (result == null) {
            result = analyzeConflictWithMinCut(variablesCount, f, variablesInfo, level, conflictClauseID)
            if (result.second in f) {
                result = result.first to null
                AnalyzeConflictWatcher.forceClassic = true
            }
            println("MinCut")
        } else {
            AnalyzeConflictWatcher.forceClassic = false
            println("Classic")
        }

        AnalyzeConflictWatcher.analyzesCount++
        if (AnalyzeConflictWatcher.analyzesCount == AnalyzeConflictWatcher.analyzesCountThreshold) {
            result = 0 to result.second
            AnalyzeConflictWatcher.analyzesCount = 0
            AnalyzeConflictWatcher.nextAnalyzesCountThreshold()
            println("=============================================================================")
        }

        /*if (f.size - clausesCount >= AnalyzeConflictWatcher.learntClausesCountThreshold) {
            //result = 0 to result.second
            //shrinkFormula(f, clausesCount)
            AnalyzeConflictWatcher.nextLearntClausesCountThreshold()
            println("*****************************************************************************")
        }*/

        return result
    }

    private fun goBack(backLevel: Int) {
        while (updates.isNotEmpty()) {
            val variable = updates.last()
            val variableInfo: VariableInfo = variablesInfo[variable] ?: throw IllegalArgumentException("go back")
            if (variableInfo.level <= backLevel) {
                break
            }
            variablesInfo.remove(variable)
            updates.removeLast()
            for ((clauseID, position) in referencesOnVariable.getOrElse(variable) { mutableListOf() }) {
                if (f[clauseID].getVariable(clausesInfo[clauseID].first) == variable || f[clauseID].getVariable(clausesInfo[clauseID].second) == variable) {
                    continue
                }
                if (clausesInfo[clauseID].second < f[clauseID].size) {
                    referencesOnVariable.getOrPut(f[clauseID][clausesInfo[clauseID].second].variableName) { mutableSetOf() }
                        .remove(Pair(clauseID, clausesInfo[clauseID].second))
                }
                clausesInfo[clauseID] = Pair(
                    minOf(position, clausesInfo[clauseID].first),
                    maxOf(position, clausesInfo[clauseID].first)
                )
                if (clauseStatus(clauseID) == ClauseStatus.UNIT) {
                    unitClauses.add(clauseID)
                }
            }
        }
    }

    private fun addClause(clause: Clause) {
        f.add(clause)
        initClause(f.lastIndex)
        scoreChangeIteration++
        if (scoreChangeIteration % 1000 == 0) {
            literalsScore.mapValues { (_, value) -> value / 2.0 }
        }
    }

    fun run(variablesCount: Int, clausesCount: Int): Pair<Boolean, VariablesState> {
        if (BCP(0) != null) {
            return Pair(false, mutableMapOf())
        }

        val distinctVarsCount = f.flatten().map { it.variableName }.distinct().size
        var level = 0
        while (variablesInfo.size < distinctVarsCount) {
            level++
            decideNewValue(level)
            while (true) {
                val conflictClauseID: Int = BCP(level) ?: break
                val (backLevel, learntClause) = analyzeConflict(level, conflictClauseID, variablesCount, clausesCount)
                if (backLevel < 0) {
                    return Pair(false, mutableMapOf())
                } else {
                    goBack(backLevel)
                    level = backLevel
                    if (learntClause != null) {
                        addClause(learntClause)
                    }
                }
            }
        }
        return Pair(true, variablesInfo)
    }
}