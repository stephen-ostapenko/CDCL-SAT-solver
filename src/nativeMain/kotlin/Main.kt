package mkn.mathlog

import com.github.ajalt.clikt.core.NoSuchParameter
import com.github.ajalt.clikt.core.PrintHelpMessage
import mkn.mathlog.utils.CLArguments
import mkn.mathlog.utils.DIMACSParser
import mkn.mathlog.utils.FileInput
import mkn.mathlog.satSolver.runCDCLSolver

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