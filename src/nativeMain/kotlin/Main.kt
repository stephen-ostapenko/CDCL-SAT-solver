package mkn.mathlog

import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput
import kotlin.system.measureTimeMillis
import mkn.mathlog.satSolver.CDCLSolver
import mkn.mathlog.utils.makeGreedyChoices
import mkn.mathlog.satSolver.runSolver

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

        val parser = DIMACSParser()
        parser.parseText(inputText)

        val initialFormula = parser.getFormula()
        val (formula, values) = makeGreedyChoices(initialFormula)
        println(formula)
        println(values)
        if (!arguments.quiet) {
            println(
                formula.joinToString(" /\\ ") {
                    "(" + it.joinToString(" \\/ ") {
                        (if (it.hasNegation) "~" else "") + it.variableName.toString()
                    } + ")"
                } + "\n"
            )
        }

        val timeElapsed = measureTimeMillis {
            val (sat, interp) = runSolver(formula, parser.getVariablesCount(), parser.getClausesCount())

            if (sat) {
                println("satisfiable")
            } else {
                println("unsatisfiable")
            }

            if (sat) {
                interp.forEachIndexed { index, info ->
                    val precalculatedValue = values[index]
                    if (precalculatedValue != null) println("$index <- $precalculatedValue")
                    else if (info != null) println("$index <- ${info.value}")
                    else if (index > 0) println("$index <- true")
                }
            }
        }

        if (arguments.time) {
            println()
            println("Done in $timeElapsed ms")
        }

    } catch (e: Exception) {
        println(e::class.qualifiedName)
        println(e.message)
    }
}