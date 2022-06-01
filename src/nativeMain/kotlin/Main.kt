package mkn.mathlog

import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput

typealias VarNameType = Int
data class Literal(val variableName: VarNameType, val hasNegation: Boolean)

typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)
typealias State = MutableMap<VarNameType, VariableInfo>

fun getAllVars(f: Formula): List<VarNameType> =
    f.flatten().map { it.variableName }.distinct()

fun formulaHasUnassignedVars(allVars: List<VarNameType>, state: State): Boolean =
    state.size < allVars.size

fun clauseHasUnassignedVars(clause: Clause, state: State): Boolean =
    clause.any { !state.containsKey(it.variableName) }

fun BCP(f: Formula, state: State, level: Int): Boolean {
    while (true) {
        var foundUnitClause = false
        for ((id, clause) in f.withIndex()) {
            if (clause.any { literal -> !literal.hasNegation == state.get(literal.variableName)?.value }) {
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
            val newScore = score.getOrElse(literal, {0} ) + 1
            score[literal] = newScore
        }
    }

    val (decidedLiteral, _) = score.maxByOrNull { (_, score) -> score } ?: return
    state[decidedLiteral.variableName] = VariableInfo(!decidedLiteral.hasNegation, null, level)
}

fun goBack(f: Formula, state: State, newLevel: Int): State =
    state.filter { (_, info) -> (info.level <= newLevel) }.toMutableMap()

fun oneLiteralAtLevel(clause: Clause, state: State, level: Int): Boolean =
    clause.count { state.get(it.variableName)?.level == level } == 1

fun resolve(clause1: Clause, clause2: Clause, v: VarNameType): Clause =
    (clause1 + clause2).filter { it.variableName != v }.distinct()

fun analyzeConflict(f: Formula, state: State, level: Int): Pair<Int, Clause> {
    if (level == 0) {
        return Pair(-1, listOf())
    }
    var currentClause = f.find { !clauseHasUnassignedVars(it, state) && it.all { literal ->
        state[literal.variableName]?.value == literal.hasNegation
    }} ?: throw IllegalArgumentException()

    while (!oneLiteralAtLevel(currentClause, state, level)) {
        val selectedLiteral = currentClause.find {
            literal -> state.get(literal.variableName)?.level == level && state.get(literal.variableName)?.antecedent != null
        } ?: break
        val previousClauseIndex = state.get(selectedLiteral.variableName)?.antecedent ?: throw IllegalArgumentException()
        currentClause = resolve(f[previousClauseIndex], currentClause, selectedLiteral.variableName)
    }

    val newLevel = currentClause.map { state.get(it.variableName)?.level ?: -1 }.filter {it != level}.
        maxByOrNull { it } ?: -1
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
                state = goBack(f, state, backLevel)
                level = backLevel
            }
        }
    }
    return Pair(true, state)
}

fun main(args: Array<String>) {
    try {
        val arguments = CLArguments()
        try {
            arguments.parse(args)
        } catch (e: PrintHelpMessage) {
            println(arguments.getFormattedHelp())
            return
        } catch (e: NoSuchParameter) {
            println("${e.message}\n")
            println("Usage: sat-solver [OPTIONS]")
            println("Run `sat-solver -h` for more information")
            return
        }

        val inputFilePath = arguments.inputFile

        val inputText = if (inputFilePath != null) {
            FileInput(inputFilePath).readAllText()
        } else {
            var line = readLine()
            val lines = mutableListOf<String>()

            while (line != null) {
                lines.add(line)
                line = readLine()
            }

            lines.joinToString("\n")
        }

        val formula = DIMACSParser.getFormula(inputText)
        println(
            formula.joinToString(" /\\ ") {
                "(" + it.joinToString(" \\/ ") {
                    (if (it.hasNegation) "~" else "") + it.variableName.toString()
                } + ")"
            } + "\n"
        )

        val (sat, interp) = runCDCLSolver(formula)
        if (sat) {
            println("satisfiable")
        } else {
            println("unsatisfiable")
        }

        if (sat) {
            interp.forEach {
                println("${it.key} <- ${it.value.value}")
            }
        }

    } catch (e: Exception) {
        println(e::class.qualifiedName)
        println(e.message)
    }
}