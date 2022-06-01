package mkn.mathlog.satSolver

typealias VarNameType = Int
data class Literal(val variableName: VarNameType, val hasNegation: Boolean)

typealias Clause = List<Literal>
typealias Formula = MutableList<Clause>

fun getAllVars(f: Formula): List<VarNameType> =
        f.flatten().map { it.variableName }.distinct()

fun formulaHasUnassignedVars(allVars: List<VarNameType>, state: State): Boolean =
        state.size < allVars.size

fun clauseHasUnassignedVars(clause: Clause, state: State): Boolean =
        clause.any { !state.containsKey(it.variableName) }

fun resolve(clause1: Clause, clause2: Clause, v: VarNameType): Clause =
        (clause1 + clause2).filter { it.variableName != v }.distinct()