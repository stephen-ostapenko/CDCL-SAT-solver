package mkn.mathlog.satSolver

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
        if (f[clauseID].size > 0) {
            referencesOnVariable.getOrPut(f[clauseID][0].variableName, { mutableSetOf() }).add(Pair(clauseID, 0))
        }
        if (f[clauseID].size > 1) {
            referencesOnVariable.getOrPut(f[clauseID][1].variableName, { mutableSetOf() }).add(Pair(clauseID, 1))
        }
        var oldVariablesInfo = mutableMapOf<VarNameType, VariableInfo>()
        oldVariablesInfo.putAll(variablesInfo)
        variablesInfo.clear()
        for (variable in updates) {
            val variableInfo = oldVariablesInfo.get(variable) ?: throw IllegalArgumentException("init clause: $variable")
            variablesInfo[variable] = variableInfo
            if (updateClause(variable, variableInfo.value, clauseID) != null) {
                unitClauses.add(clauseID)
            }
        }

        unitClauses.add(clauseID)

        f[clauseID].forEach { literal ->
            val newScore = literalsScore.getOrElse(literal, {0.0} ) + 1.0
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
        val value = variablesInfo[variable]?.value
        if (clauseID == 4) {
            //println("variable status: $variable $hasNegation $value")
        }
        if (value == null) {
            return VariableInClauseStatus.UNRESOLVED
        } else if (value == !hasNegation) {
            return VariableInClauseStatus.CORRECT
        } else {
            return VariableInClauseStatus.INCORRECT
        }
    }

    private fun clauseStatus(clauseID: Int): ClauseStatus {
        var satisfied = false
        var unassignedCount = 0
        fun processWatchedLiteral(index: Int) {
            if (clauseID == 4) {
                //println("process watched literal: $index")
            }
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
        if (watchedIndex1 >= 0 && watchedIndex1 < f[clauseID].size &&
                getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.UNRESOLVED) {
            return Pair(f[clauseID][watchedIndex1].variableName, watchedIndex1)
        }
        return Pair(f[clauseID][watchedIndex2].variableName, watchedIndex2)
    }

    fun updateClause(variable: VarNameType, value: Boolean, clauseID: Int): Int? {
        if (f[clauseID].getVariable(clausesInfo[clauseID].first) != variable && f[clauseID].getVariable(clausesInfo[clauseID].second) != variable) {
            return null
        }
        var (watchedIndex1, watchedIndex2) = clausesInfo[clauseID]
        //println("in $clauseID $watchedIndex1 $watchedIndex2")
        if (f[clauseID].getVariable(watchedIndex1) != variable) {
            watchedIndex1 = watchedIndex2.also { watchedIndex2 = watchedIndex1 }
        }
        //println("${f[clauseID].getVariable(watchedIndex1)} ${f[clauseID].getValue(watchedIndex1)} $value")
        if (f[clauseID].getValue(watchedIndex1) != value) {
            watchedIndex1 = maxOf(watchedIndex1, watchedIndex2) + 1
            while (watchedIndex1 < f[clauseID].size && getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.INCORRECT) {
                watchedIndex1++
            }
            if (watchedIndex1 < f[clauseID].size && getVariableInClauseStatus(clauseID, watchedIndex1) == VariableInClauseStatus.UNRESOLVED) {
                referencesOnVariable.getOrPut(f[clauseID][watchedIndex1].variableName, { mutableSetOf() }).add(Pair(clauseID, watchedIndex1))
            }
            if (watchedIndex1 > watchedIndex2) {
                //swap(watchedIndex1, watchedIndex2)
                watchedIndex1 = watchedIndex2.also { watchedIndex2 = watchedIndex1 }
            }
            clausesInfo[clauseID] = Pair(watchedIndex1, watchedIndex2)
            val newStatus = clauseStatus(clauseID)
            //println("out $clauseID $watchedIndex1 $watchedIndex2 $newStatus")
            if (newStatus == ClauseStatus.UNIT) {
                unitClauses.add(clauseID)
            } else if (newStatus == ClauseStatus.UNSATISFIED) {
                //println("failed assign: $clauseID")
                return clauseID
            }
        }
        return null
    }

    private fun assign(variable: VarNameType, value: Boolean, level: Int, antecedent: Int?): Int? {
        variablesInfo[variable] = VariableInfo(value, antecedent, level)
        updates.add(variable)
        var unsat: Int? = null
        //println("assign: $variable ${referencesOnVariable.getOrElse(variable) { mutableListOf() }}")
        for ((clauseID, _) in referencesOnVariable.getOrElse(variable) { mutableListOf() }) {
            val result = updateClause(variable, value, clauseID)
            if (result != null) {
                unsat = result
            }
        }
        return unsat
    }

    private fun decideNewValue(level: Int) {
        val (decidedLiteral, _) = literalsScore.filter { (literal, _) -> variablesInfo.get(literal.variableName) == null }
                .maxByOrNull { (_, score) -> score } ?: return

        assign(decidedLiteral.variableName, !decidedLiteral.hasNegation, level, null)
    }

    private fun BCP(level: Int): Int? {
        while (unitClauses.isNotEmpty()) {
            val clauseIndex = unitClauses.last()
            unitClauses.removeLast()
            val status = clauseStatus(clauseIndex)
            //println("BCP $clauseIndex $status")
            if (status == ClauseStatus.UNSATISFIED) {
                return clauseIndex
            }
            if (status != ClauseStatus.UNIT) {
                continue
            }
            //println(clauseIndex)
            val (variable, position) = getUnassignedVariable(clauseIndex)
            val value: Boolean = f[clauseIndex].getValue(position) ?: throw IllegalArgumentException("in BCP")
            val conflictClauseID = assign(variable, value, level, clauseIndex) ?: continue
            return conflictClauseID
        }
        return null
    }

    fun oneLiteralAtLevel(clause: Clause, state: VariablesState, level: Int): Boolean =
            clause.count { state[it.variableName]?.level == level } == 1

    fun analyzeConflict(level: Int, conflictClauseID: Int): Pair<Int, Clause> {
        if (level == 0) {
            return Pair(-1, listOf())
        }

        var currentClause = f[conflictClauseID]
        while (!oneLiteralAtLevel(currentClause, variablesInfo, level)) {
            val selectedLiteral = currentClause.find {
                literal -> variablesInfo[literal.variableName]?.level == level && variablesInfo[literal.variableName]?.antecedent != null
            } ?: break
            val previousClauseIndex = variablesInfo[selectedLiteral.variableName]?.antecedent ?: throw IllegalArgumentException("analyze conflict")
            currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variableName)
        }

        val newLevel = currentClause.map { variablesInfo[it.variableName]?.level ?: 0 }.filter { it != level }.
        maxByOrNull { it } ?: 0
        return Pair(newLevel, currentClause)
    }

    private fun goBack(backLevel: Int) {
        //println("go back in")
        while (updates.isNotEmpty()) {
            val variable = updates.last()
            //println("variable $variable")
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
                //println("old values: $variable $clauseID ${clausesInfo[clauseID].first} ${clausesInfo[clauseID].second}")
                if (clausesInfo[clauseID].second < f[clauseID].size) {
                    referencesOnVariable.getOrPut(f[clauseID][clausesInfo[clauseID].second].variableName, { mutableSetOf() }).remove(Pair(clauseID, clausesInfo[clauseID].second))
                }
                //println("new values ${clausesInfo[clauseID].first} $position")
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

    fun run(): Pair<Boolean, VariablesState> {
        if (BCP(0) != null) {
            return Pair(false, mutableMapOf())
        }

        val varsCount = f.flatten().map { it.variableName }.distinct().size
        var level = 0
        while (variablesInfo.size < varsCount) {
            level++
            decideNewValue(level)
            while (true) {
                val conflictClauseID: Int = BCP(level) ?: break
                //println("conflict clause $conflictClauseID")
                val (backLevel, learntClause) = analyzeConflict(level, conflictClauseID)
                //println("analyze conflict result: $backLevel $learntClause")
                if (backLevel < 0) {
                    return Pair(false, mutableMapOf())
                } else {
                    goBack(backLevel)
                    level = backLevel
                    addClause(learntClause)
                }
            }
        }
        return Pair(true, variablesInfo)
    }
}