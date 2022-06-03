package mkn.mathlog

import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput
import kotlin.system.measureTimeMillis
import mkn.mathlog.satSolver.CDCLSolver

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

        val formula = parser.getFormula()
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
            val (sat, interp) = CDCLSolver(formula, parser.getVariablesCount(), parser.getClausesCount()).run()
            if (sat) {
                println("satisfiable")
            } else {
                println("unsatisfiable")
            }

            if (sat) {
                interp.forEachIndexed { index, info ->
                    if (info != null) println("$index <- ${info.value}")
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