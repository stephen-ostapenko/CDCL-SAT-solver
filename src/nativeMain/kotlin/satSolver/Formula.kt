package mkn.mathlog.satSolver

typealias VarNameType = Int
data class Literal(val variableName: VarNameType, val hasNegation: Boolean)

typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>

fun getAllVars(f: Formula): List<VarNameType> =
    f.flatten().map { it.variableName }.distinct()

fun resolve(clause1: Clause, clause2: Clause, v: VarNameType): Clause =
    (clause1 + clause2).filter { it.variableName != v }.distinct()

fun Clause.getVariable(index: Int): VarNameType? {
    if (index < 0 || index >= this.size) {
        return null
    }
    return this[index].variableName
}

fun Clause.getValue(index: Int): Boolean? {
    if (index < 0 || index >= this.size) {
        return null
    }
    return !this[index].hasNegation
}

enum class ClauseStatus {
    SATISFIED, UNSATISFIED, UNIT, UNRESOLVED
}

enum class VariableInClauseStatus {
    CORRECT, INCORRECT, UNRESOLVED
}