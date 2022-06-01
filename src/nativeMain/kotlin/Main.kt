data class Literal(val variable: String, val hasNegation: Boolean)
typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>
typealias State = MutableMap<String, VariableInfo>

data class VariableInfo(var value: Boolean, val antecedent: Int?, val level: Int)

fun getAllVars(f: Formula): List<String> {
    val vars: MutableList<String> = mutableListOf<String>()
    for (clause in f) {
        for (literal in clause) {
            vars.add(literal.variable)
        }
    }
    return vars.toList()
}

fun hasUnassignedVars(allVars: List<String>, state: State): Boolean {
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

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    println("Hello, Kotlin/Native!")
    println(isExperimentalMM())
}