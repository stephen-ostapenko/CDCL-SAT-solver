package mkn.mathlog.utils

import mkn.mathlog.satSolver.*
import kotlin.math.min

data class Edge(val to: Int, val capacity: Int, var flow: Int = 0)
typealias EdgeList = MutableList<Edge>
typealias Graph = List<MutableList<Int>>
data class Net(val gr: Graph, val edgeList: EdgeList, val source: Int, val drain: Int)

const val INF = 1_000_000_000 + 2022

fun analyzeConflictWithMinCut(formula: Formula, state: State, level: Int, variablesCount: Int): Pair<Int, Clause> {
    val (gr, edgeList, source, drain) = buildNet(variablesCount, formula, state)
    ////println("finding flow")
    findFlowDinic(gr.size, source, drain, gr, edgeList)
    ////println("found flow")

    val clause = findEdgeOfMinCut(variablesCount, state, gr.size, source, gr, edgeList)
    val newLevel = clause.map { state[it.variableName]?.level ?: 0 }.filter { it != level }.
        maxByOrNull { it } ?: 0

    return newLevel to clause
}

fun buildNet(variablesCount: Int, formula: Formula, state: State): Net {
    val source = variablesCount * 2
    val drain = variablesCount * 2 + 1

    val gr: Graph = List(variablesCount * 2 + 2) { mutableListOf() }
    val edgeList: EdgeList = mutableListOf()

    val conflictClause = formula.find { !clauseHasUnassignedVars(it, state) && it.all { literal ->
        state[literal.variableName]?.value == literal.hasNegation
    }} ?: throw IllegalArgumentException("Can't find conflict clause")
    //println(conflictClause)

    val addEdge = lambda@{ from: Int, to: Int, capacity: Int ->
        edgeList.add(Edge(to, capacity))
        gr[from].add(edgeList.size - 1)
        edgeList.add(Edge(from, 0))
        gr[to].add(edgeList.size - 1)
        return@lambda
    }

    for (vertex in 1..variablesCount) {
        addEdge((vertex - 1) * 2, (vertex - 1) * 2 + 1, 1)
    }

    for ((name, info) in state) {
        if (info.antecedent == null) {
            addEdge(source, (name - 1) * 2, INF)
            continue
        }

        val clauseInd: Int = info.antecedent
        formula[clauseInd].forEach {
            addEdge((it.variableName - 1) * 2 + 1, (name - 1) * 2, INF)
        }
    }

    conflictClause.forEach {
        addEdge((it.variableName - 1) * 2 + 1, drain, INF)
    }

    return Net(gr, edgeList, source, drain)
}

fun dfsDinic(v: Int, curFlow: Int, drain: Int, minFlow: Int, gr: Graph, edgeList: EdgeList,
             used: MutableList<Boolean>, edgePtr: MutableList<Int>, dist: List<Int>): Int
{
    if (v == drain) {
        return curFlow
    }
    used[v] = true

    while (edgePtr[v] < gr[v].size) {
        val edgeInd = gr[v][edgePtr[v]]
        edgePtr[v]++

        val edge = edgeList[edgeInd]
        if (used[edge.to] || dist[edge.to] != dist[v] + 1) {
            continue
        }

        var nextFlow = min(curFlow, edge.capacity - edge.flow)
        if (nextFlow < minFlow) {
            continue
        }
        
        nextFlow = dfsDinic(edge.to, nextFlow, drain, minFlow, gr, edgeList, used, edgePtr, dist)
        if (nextFlow >= minFlow) {
            edge.flow += nextFlow
            edgeList[edgeInd xor 1].flow -= nextFlow
            return nextFlow
        }
    }
    return 0
}

fun bfsDinic(source: Int, drain: Int, minFlow: Int, gr: Graph, edgeList: EdgeList,
             used: MutableList<Boolean>, edgePtr: MutableList<Int>, dist: MutableList<Int>): Int
{
    edgePtr.fill(0)
    dist.fill(INF)
    val q = ArrayDeque<Int>()

    dist[source] = 0
    q.addLast(source)
    while (q.isNotEmpty()) {
        val v = q.removeFirst()
        for (edgeInd in gr[v]) {
            val edge = edgeList[edgeInd]
            if (dist[edge.to] > dist[v] + 1 && edge.capacity - edge.flow >= minFlow) {
                dist[edge.to] = dist[v] + 1
                q.addLast(edge.to)
            }
        }
    }

    if (dist[drain] == INF) {
        return 0
    }

    var flow = 0
    while (true) {
        used.fill(false)

        ////println("run dfs")
        val res = dfsDinic(source, INF, drain, minFlow, gr, edgeList, used, edgePtr, dist)
        if (res == 0) {
            break
        }
        flow += res
    }

    return flow
}

fun findFlowDinic(nVertex: Int, source: Int, drain: Int, gr: Graph, edgeList: EdgeList) {
    val used = MutableList(nVertex) { false }
    val edgePtr = MutableList(nVertex) { 0 }
    val dist = MutableList(nVertex) { 0 }

    for (iter in 30 downTo 0) {
        ////println(iter)
        while (true) {
            ////println("run bfs")
            val flow = bfsDinic(source, drain, 1 shl iter, gr, edgeList, used, edgePtr, dist)
            if (flow == 0) {
                break
            }
        }
    }
}

fun dfsFindEdgeOfMinCut(v: Int, gr: Graph, edgeList: EdgeList, used: MutableList<Boolean>) {
    used[v] = true
    for (edgeInd in gr[v]) {
        val edge = edgeList[edgeInd]
        if (!used[edge.to] && edge.flow < edge.capacity) {
            dfsFindEdgeOfMinCut(edge.to, gr, edgeList, used)
        }
    }
}

fun findEdgeOfMinCut(variablesCount: Int, state: State,
                     nVertex: Int, source: Int, gr: Graph, edgeList: EdgeList): Clause
{
    val used = MutableList(nVertex) { false }
    dfsFindEdgeOfMinCut(source, gr, edgeList, used)

    val result = mutableListOf<Literal>()
    for (vertex in 1..variablesCount) {
        if (used[(vertex - 1) * 2] && !used[(vertex - 1) * 2 + 1]) {
            val value = state[vertex]?.value ?: error("Can't find value in state")
            result.add(Literal(vertex, value))
        }
    }

    return result
}