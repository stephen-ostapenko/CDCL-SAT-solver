package mkn.mathlog.utils

import mkn.mathlog.satSolver.Clause
import mkn.mathlog.satSolver.Formula
import mkn.mathlog.satSolver.Literal
import kotlin.math.abs
import kotlin.math.max

class DIMACSParser {
    private var varsCount = 0
    private var clCount = 0
    private var formula: Formula = mutableListOf()

    fun getVariablesCount(): Int {
        return varsCount
    }

    fun getClausesCount(): Int {
        return clCount
    }

    fun getFormula(): Formula {
        return formula
    }

    fun parseText(text: String) {
        val lines = text.split("\n", "\r\n")
        var parameters: Pair<Int, Int>? = null
        var variablesCount = 0
        var clausesCount = 0
        val result = mutableListOf<Clause>()

        for (line in lines) {
            val tokens = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            if (tokens.isEmpty() || tokens[0] == "c") {
                continue
            }

            if (when {
                tokens[0] != "p" ->
                    false
                parameters != null ->
                    throw DIMACSException("There can't be more than one DIMACS parameters line", line)
                tokens.size != 4 ->
                    throw DIMACSException("There must be exactly 4 tokens in DIMACS parameters line", line)
                tokens[1] != "cnf" ->
                    throw DIMACSException("Formula format tag is not \"cnf\"", line)
                tokens[2].toIntOrNull() == null ->
                    throw DIMACSException("Number of variables must be a positive integer", line)
                tokens[2].toInt() <= 0 ->
                    throw DIMACSException("Number of variables must be a positive integer", line)
                tokens[3].toIntOrNull() == null ->
                    throw DIMACSException("Number of clauses must be a positive integer", line)
                tokens[3].toInt() <= 0 ->
                    throw DIMACSException("Number of clauses must be a positive integer", line)
                else ->
                    true
            }) {
                parameters = tokens[2].toInt() to tokens[3].toInt()
                continue
            }

            val variables = try {
                tokens.map { it.toInt() }.filter { it != 0 }
            } catch (e: NumberFormatException) {
                throw DIMACSException("Variable tokens must be integers", line)
            }
            if (variables.isEmpty()) {
                throw DIMACSException("Clause can't be empty", line)
            }

            result.add(variables.map { Literal(abs(it), it < 0) })

            variablesCount = max(variablesCount, variables.maxOf { abs(it) })
            clausesCount++
        }

        if (parameters != null && parameters.first < variablesCount) {
            throw DIMACSException(
                "DIMACS parameters don't fit the given file: " +
                        "expected ${parameters.first} variables but got $variablesCount\n"
            )
        }
        if (parameters != null && parameters.second != clausesCount) {
            throw DIMACSException(
                "DIMACS parameters don't fit the given file: " +
                        "expected ${parameters.second} clauses but got $clausesCount\n"
            )
        }

        varsCount = variablesCount
        clCount = clausesCount
        formula = result
    }

    class DIMACSException : IllegalArgumentException {
        constructor(msg: String) : super(msg)
        constructor(msg: String, line: String) : super("$msg\n\terror in line: \"$line\"\n")
    }
}