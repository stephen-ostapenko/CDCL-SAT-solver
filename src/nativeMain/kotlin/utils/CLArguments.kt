package mkn.mathlog.utils

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class CLArguments : CliktCommand() {
    override fun run() = Unit

    val inputFile: String? by option("-i", "--input", help = "Path to DIMACS input file")
    val quiet: Boolean by option("-q", "--quiet").flag()
    val time: Boolean by option("-t", "--time").flag()
}