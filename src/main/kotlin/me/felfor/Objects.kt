package me.felfor

import me.felfor.VariableFactory.computeIfAbsent
import kotlin.system.exitProcess

/**
 * @since 1/19/18
 * @author felfor
 */
open class Token(open var beginIndex: Int, open var text: String) {
    fun addChar(ch: Char) {
        text += ch
    }
}

enum class InstructionType {
    Successor, Predecessor, Conditional,

    None
}

data class Line(var lineIndex: Int?, var instructionType: InstructionType, var variable: Variable, var goToLabel: String?, val label: String) {
    fun setLineNumber(lineNumber: Int): Line {
        lineIndex = lineNumber
        return this
    }
}

data class AppSnapshot(val line: Int, val variablesValue: String)

class App(private var lines: List<Line>, private var previousSnapshots: MutableList<AppSnapshot>, private var currentRunningLine: Int, private var labelToLine: Map<String, Line>) {
    var iter = 0
    private val lastLine: Line = Line(lines.size + 1, InstructionType.None, VariableFactory.getOrDefault("y", Variable("y", 0)), "e", "e")
    fun run(args: List<Int>, maxIterCounts: Int = 1000): Int {
        println("max iterations:$maxIterCounts")
        args.forEachIndexed { index, arg -> VariableFactory.getVariable("x%s".format(index + 1)).value = arg }
        previousSnapshots = mutableListOf()
        currentRunningLine = 0
        iter = 0
        var currentSnapshot = AppSnapshot(currentRunningLine, VariableFactory.takeSnapshotFromVariables())
        do {
            previousSnapshots.add(currentSnapshot)
            runCurrentLine(lines[currentRunningLine])
            currentSnapshot = AppSnapshot(currentRunningLine, VariableFactory.takeSnapshotFromVariables())
            if (currentRunningLine >= lines.size)
                return VariableFactory.getOrDefault("y", Variable("y", 0)).value

        } while (iter++ < maxIterCounts && !previousSnapshots.contains(currentSnapshot))
        if (iter > maxIterCounts) throw RuntimeException("Maximum iter counts $maxIterCounts defined by user has been exceeded!")
        else throw RuntimeException("It seems programs goes into the loop so we break the running operation!")
    }

    private fun calculateProgramResult() {
        val programResult = VariableFactory.getOrDefault("y", Variable("y", 0)).value
        println("program result code after $iter iterations is $programResult")
        exitProcess(0)
    }

    private fun runCurrentLine(line: Line) {
        if (line.instructionType == InstructionType.Conditional) {
            if (line.variable.value > 0)
                return calculateBranchingLine(line)
        } else if (line.instructionType == InstructionType.Successor)
            line.variable.incr()
        else
            line.variable.decr()
        currentRunningLine++
    }

    private fun calculateBranchingLine(line: Line) {
        if (line.goToLabel?.toLowerCase() == "e")
            currentRunningLine = lines.size + 1
        else
            currentRunningLine = labelToLine.getOrDefault(line.goToLabel!!, lastLine).lineIndex!!
    }

}

object VariableFactory : HashMap<String, Variable>() {
    fun getVariable(name: String): Variable {
        return computeIfAbsent(name, { name -> Variable(name) })
    }

    fun takeSnapshotFromVariables(): String {
        return values.joinToString("_")
    }
}

data class Variable(val name: String, var value: Int = 0) {
    fun incr() {
        value++
    }

    fun decr() {
        if (value > 0) value--
    }
}