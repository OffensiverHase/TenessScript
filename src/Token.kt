import java.io.Serializable

class Token(val type: String, val value: String?, val pos: Position) : Serializable {

    companion object {
        val keywords = arrayOf(
            "VAR", "IF", "THEN", "ELSE", "FOR", "STEP", "WHILE", "FUN", "RETURN", "BREAK", "CONTINUE", "OBJECT"
        )
        const val INT = "int"
        const val FLOAT = "float"
        const val PLUS = "+"
        const val MINUS = "-"
        const val MUL = "*"
        const val DIV = "/"
        const val POW = "^"
        const val LPAREN = "("
        const val RPAREN = ")"
        const val IDENTIFIER = "identifier"
        const val KEYWORD = "keyword"
        const val ASSIGN = "<-"
        const val EQUALS = "="
        const val UNEQUALS = "<>"
        const val LESS = "<"
        const val GREATER = ">"
        const val LESSEQUAL = "<="
        const val GREATEREQUAL = ">="
        const val NOT = "!"
        const val AND = "&"
        const val OR = "|"
        const val COMMA = ","
        const val STRING = "string"
        const val LSQUARE = "["
        const val RSQUARE = "]"
        const val GET = "[...]"
        const val COLON = ":"
        const val NEWLINE = "\\n"
        const val LCURLY = "{"
        const val RCURLY = "}"
        const val DOT = "."
        const val TO = ".."
        const val EOF = "'end of file'"
    }

    override fun toString(): String {
        return if (this.value != null) "$type:$value" else type
    }
}