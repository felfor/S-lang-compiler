package me.felfor

/**
 * @since 1/19/18
 * @author felfor
 */
fun parseSyntax(rawLine: String, lineNumber: Int): Line {
    val c = ConditionalInstructionAcceptor()
    var s = SuccessorInstructionAcceptor()
    var p = PredecessorInstructionAcceptor()

    if (c.accept(rawLine))
        return c.getOutput().setLineNumber(lineNumber)
    if (s.accept(rawLine))
        return s.getOutput().setLineNumber(lineNumber)
    if (p.accept(rawLine))
        return p.getOutput().setLineNumber(lineNumber)

    val cErrorPosition = c.detectErrorPosition(rawLine)
    val sErrorPosition = s.detectErrorPosition(rawLine)
    val pErrorPosition = p.detectErrorPosition(rawLine)

    var errorPosition = maxOf(cErrorPosition, maxOf(sErrorPosition, pErrorPosition))
    printError(rawLine, lineNumber + 1, errorPosition)
    throw LexicalException(errorPosition, "Invalid token!")
}

fun printError(rawLine: String, lineNumber: Int, errorPosition: Int) {
    var errorCursor: String = ""
    for (i in 1 until errorPosition)
        errorCursor += " "
    errorCursor += "^"
    println("Incorrect instruction token at line:$lineNumber and index:$errorPosition :")
    println("\t$rawLine")
    println("\t$errorCursor")
}

abstract class SyntaxAcceptor<T> {
    object Statics {
        @JvmField
        val VARIABLE_REGEX: String = " *[a-zA-Z]+ *[0-9]*"
        @JvmField
        val LABEL_REGEX: String = " *\\[ *[a-zA-Z]+ *[0-9]* *]"

    }

    var textValue: String = ""
    var endPositionIndex: Int = 0
    private val containingRegex: Regex = (getSyntaxRegex().joinToString("") + ".*").toRegex()
    val regex: Regex = getSyntaxRegex().joinToString("").toRegex()

    fun accept(text: String, startPoint: Int = 0): Boolean {
        val currentText = text.substring(startPoint)
        if (containingRegex.matches(currentText)) {
            currentText.forEachIndexed { index, ch -> if (visit(index + startPoint, ch)) return true }
            return true
        }
        return false
    }

    fun detectErrorPosition(text: String, startPoint: Int = 0): Int {
        val currentText = text.substring(startPoint)
        var errorPosition = 0
        if (getSyntaxRegex().size > 1) {
            var currentRegex = ""
            for (syntaxRegex in getSyntaxRegex()) {
                currentRegex += syntaxRegex
                val currentErrorPosition = getErrorPosition(currentText, (currentRegex).toRegex()) + startPoint
                if (currentErrorPosition > 0 && currentErrorPosition < text.length && currentErrorPosition > errorPosition)
                    errorPosition = currentErrorPosition
            }
        }
        return errorPosition
    }

    private fun getErrorPosition(currentText: String, currentRegex: Regex): Int {
        matched = false
        currentText.forEachIndexed { index, ch -> if (isNotMatchingEvenMore(currentText.substring(0, index), currentRegex)) return index }
        return currentText.length
    }

    private fun isNotMatchingEvenMore(text: String, currentRegex: Regex): Boolean {
        var match = currentRegex.matches(text)
        if (matched && !match)
            return true
        matched = match
        return false
    }

    var matched = false
    private fun visit(charIndex: Int, ch: Char): Boolean {
        var match = regex.matches(textValue + ch)
        if (matched && !match)
            return true
        matched = match
        textValue += ch
        if (match)
            this.endPositionIndex = charIndex
        return false
    }

    abstract fun getOutput(): T

    abstract fun getSyntaxRegex(): List<String>
}

class LabelAcceptor : SyntaxAcceptor<String>() {
    override fun getOutput(): String {
        return textValue.replace("[ \\[\\]]".toRegex(), "")
    }

    override fun getSyntaxRegex(): List<String> {
        return arrayListOf(Statics.LABEL_REGEX)
    }
}

class VariableAcceptor : SyntaxAcceptor<Variable>() {
    override fun getOutput(): Variable {
        return VariableFactory.getVariable(textValue.replace(" ", ""))
    }

    override fun getSyntaxRegex(): List<String> {
        return arrayListOf(Statics.VARIABLE_REGEX)
    }
}

class BranchingLabelAcceptor : SyntaxAcceptor<String>() {
    override fun getOutput(): String {
        return textValue.replace(" ", "")
    }

    override fun getSyntaxRegex(): List<String> {
        return arrayListOf(" *[a-zA-Z]+ *[0-9]*")
    }
}

class ConditionalInstructionAcceptor : SyntaxAcceptor<Line>() {
    var labelAcceptor: LabelAcceptor = LabelAcceptor()
    var branchingLabelAcceptor: BranchingLabelAcceptor = BranchingLabelAcceptor()
    var variableAcceptor: VariableAcceptor = VariableAcceptor()
    private var contentExtractionRegex: Regex = (".*[iI][Ff] +([a-zA-Z]+ *[0-9]*) *! *= *0 +[gG][oO] *[Tt][oO] +(%s)".format(Statics.VARIABLE_REGEX).toRegex())
    override fun getOutput(): Line {
        labelAcceptor.accept(textValue)
        val matchEntire = contentExtractionRegex.matchEntire(textValue)!!
        variableAcceptor.accept(matchEntire.groups[1]!!.value)
        branchingLabelAcceptor.accept(matchEntire.groups[2]!!.value)
        return Line(null, InstructionType.Conditional, variableAcceptor.getOutput(), branchingLabelAcceptor.getOutput(), labelAcceptor.getOutput())
    }

    override fun getSyntaxRegex(): List<String> {

        return arrayListOf("(%s)? *[iI][Ff] +".format(Statics.LABEL_REGEX), "[a-zA-Z]+ *", "[0-9]* *", "! *", "= *", "0 +", "[gG][oO] *", "[Tt][oO] +", Statics.VARIABLE_REGEX)
    }
}

class SuccessorInstructionAcceptor : SyntaxAcceptor<Line>() {
    var labelAcceptor: LabelAcceptor = LabelAcceptor()
    var variableAcceptor: VariableAcceptor = VariableAcceptor()
    override fun getOutput(): Line {
        var nextPos = 0
        if (labelAcceptor.accept(textValue))
            nextPos = labelAcceptor.endPositionIndex + 1
        variableAcceptor.accept(textValue, nextPos)
        val matchEntire = regex.matchEntire(textValue)!!
        if (matchEntire.groups[matchEntire.groups.size - 2]!!.value != matchEntire.groups[matchEntire.groups.size - 1]!!.value)
            throw IllegalArgumentException("Assignment is only allowed for incrementing or reducing value. value %s is incorrect and just can be %s".format(matchEntire.groups[matchEntire.groups.size - 2]!!.value, matchEntire.groups[matchEntire.groups.size - 1]!!.value))
        return Line(null, InstructionType.Successor, variableAcceptor.getOutput(), null, labelAcceptor.getOutput())
    }

    override fun getSyntaxRegex(): List<String> {
        return arrayListOf("(?<var1>%s)? *(%s) *".format(Statics.LABEL_REGEX, Statics.VARIABLE_REGEX), "< *", "- *", "(?<var2>%s) *".format(Statics.VARIABLE_REGEX), "\\+ *", "1 *")
    }
}

class PredecessorInstructionAcceptor : SyntaxAcceptor<Line>() {
    var labelAcceptor: LabelAcceptor = LabelAcceptor()
    var variableAcceptor: VariableAcceptor = VariableAcceptor()
    override fun getOutput(): Line {
        var nextPos = 0
        if (labelAcceptor.accept(textValue))
            nextPos = labelAcceptor.endPositionIndex + 1
        variableAcceptor.accept(textValue, nextPos)
        val matchEntire = regex.matchEntire(textValue)!!
        if (matchEntire.groups[matchEntire.groups.size - 2]!!.value != matchEntire.groups[matchEntire.groups.size - 1]!!.value)
            throw IllegalArgumentException("Assignment is only allowed for incrementing or reducing value. value %s is incorrect and just can be %s".format(matchEntire.groups[matchEntire.groups.size - 2]!!.value, matchEntire.groups[matchEntire.groups.size - 1]!!.value))
        return Line(null, InstructionType.Predecessor, variableAcceptor.getOutput(), null, labelAcceptor.getOutput())
    }

    override fun getSyntaxRegex(): List<String> {
        return arrayListOf("(%s)? *(?<var1>%s) *".format(Statics.LABEL_REGEX, Statics.VARIABLE_REGEX), "< *", "- *", "(?<var2>%s) *".format(Statics.VARIABLE_REGEX), "- *", "1 *")
    }
}