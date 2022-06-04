package mkn.mathlog

import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import mkn.mathlog.satSolver.runSolver
import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput
import mkn.mathlog.utils.makeGreedyChoices
import kotlin.system.measureTimeMillis

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

        val timeElapsed = measureTimeMillis {
            val (sat, interp) = runSolver(
                formula, parser.getVariablesCount(), parser.getClausesCount(), !arguments.quiet
            )

            if (sat) {
                println("satisfiable")
            } else {
                println("unsatisfiable")
            }

            if (sat) {
                interp.forEachIndexed { index, info ->
                    val precalculatedValue = values[index]
                    when {
                        precalculatedValue != null -> println("$index <- $precalculatedValue")
                        info != null -> println("$index <- ${info.value}")
                        index > 0 -> println("$index <- true")
                    }
                }
            }
        }

        if (arguments.time) {
            println()
            println("Done in $timeElapsed ms")
        }

    } catch (e: Exception) {
        println(e.message)
    }
}