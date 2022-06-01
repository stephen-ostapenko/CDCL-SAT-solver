package mkn.mathlog.utils

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option

class CLArguments : CliktCommand() {
    override fun run() = Unit

    val inputFile by option("-i", "--input", help = "Path to DIMACS input file")
}