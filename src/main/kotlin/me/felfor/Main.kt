package me.felfor

import java.io.File
import kotlin.system.exitProcess


/**
 * @since 1/19/18
 * @author felfor
 */
fun main(args: Array<String>) {
    val (fileName, programArgs) = parseArgs(args)
    var program = loadApp(fileName as String)
    val returnValue = program.run(programArgs as List<Int>)
    println("=====================\nThe return value is: $returnValue")
}

fun parseArgs(args: Array<String>): List<Any?> {
    try {
        var fileName: String? = null
        var programArgs: List<Int>
        programArgs = mutableListOf()
        var i = 0
        while (i < args.size) {
            if (args[i] == "-p" && i + 1 < args.size) fileName = args[++i]
            else programArgs.add(Integer.valueOf(args[i]))
            i++
        }
        if (fileName == null)
            throw IllegalArgumentException("you must specify the file name of program that you want to compile and run")
        i = 0
        println("user args:%s".format(programArgs.map { "x%s:$it".format(++i) }.toList()))
        return listOf(fileName, programArgs)
    } catch (e: Exception) {
        IllegalArgumentException(e.message).printStackTrace(System.out)
        printUsage()
        exitProcess(1)
    }
}

fun printUsage() {
    println("\nusage:\n\t -p [(require) program name written in S language] [program arg (int)]...[program arg (int)]")
}

fun loadApp(codeFileName: String): App {
    try {
        val lines: List<Line> = File(codeFileName).readLines().mapIndexed { index, l -> parseSyntax(l, index) }.toList()
        val labelsToLines = HashMap<String, Line>()
        lines
                .filterNot { labelsToLines.containsKey(it.label) }
                .forEach { labelsToLines[it.label] = it }
        return App(lines, previousSnapshots = arrayListOf(), currentRunningLine = 0, labelToLine = labelsToLines)
    } catch (e: Exception) {
        print(e.message
        )
        exitProcess(1)
    }
}