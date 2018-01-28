package me.felfor

/**
 * @since 1/19/18
 * @author felfor
 */
data class LexicalException(val errorIndex: Int, override val message: String) : IllegalArgumentException()
