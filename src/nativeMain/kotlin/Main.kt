package mkn.mathlog

import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput

typealias VarNameType = Int
data class Literal(val variable: VarNameType, val hasNegation: Boolean)
typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>
typealias State = MutableMap<String, VariableInfo>

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)

fun getAllVars(f: Formula): List<VarNameType> {
    val vars = mutableListOf<VarNameType>()
    for (clause in f) {
        for (literal in clause) {
            vars.add(literal.variable)
        }
    }
    return vars.toList()
}

fun hasUnassignedVars(allVars: List<VarNameType>, state: State): Boolean {
    return state.size < allVars.size
}

fun BCP(f: Formula, state: State): Boolean {
    TODO()
}

fun decideNewValue(f: Formula, state: State) {
    TODO()
}

fun CDCL(f: Formula): Pair<Boolean, State> {
    val values: MutableMap<String, VariableInfo> = mutableMapOf<String, VariableInfo>()
    val allVars = getAllVars(f)
    if (!BCP(f, values)) {
        return Pair(false, mutableMapOf<String, VariableInfo>())
    }

    var level = 0
    while (hasUnassignedVars(allVars, values)) {
        level++
        decideNewValue(f, values)
        while (BCP(f, values) == false) {
            /*(backLevel, learntClause) = analyzeConflict()
            f.add(learntClause)
            if (backLevel < 0) {
                return Pair(false, mutableMapOf<String, Boolean>())
            } else {
                goBack(f, values, backLevel)
                level = backLevel
            }*/
        }
    }
    return Pair(true, values)
}

fun main(args: Array<String>) {
    try {
        val arguments = CLArguments()
        arguments.parse(args)
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

        formula.forEach {
            println(it)
        }

    } catch (e: Exception) {
        println(e.message)
    }
}