import java.io.Serializable

sealed class Token(private val type: String, val value: String? = null, val pos: Position) : Serializable {
    class INT(value: Int, pos: Position) : Token("int", value.toString(), pos)
    class FLOAT(value: Float, pos: Position) : Token("float", value.toString(), pos)
    class PLUS(pos: Position) : Token("+", pos = pos)
    class MINUS(pos: Position) : Token("-", pos = pos)
    class MUL(pos: Position) : Token("*", pos = pos)
    class DIV(pos: Position) : Token("/", pos = pos)
    class POW(pos: Position) : Token("^", pos = pos)
    class LPAREN(pos: Position) : Token("(", pos = pos)
    class RPAREN(pos: Position) : Token(")", pos = pos)
    class IDENTIFIER(name: String, pos: Position) : Token("identifier", name, pos)
    class KEYWORD(name: String, pos: Position) : Token("keyword", name, pos)
    class ASSIGN(pos: Position) : Token("<-", pos = pos)
    class EE(pos: Position) : Token("=", pos = pos)
    class NE(pos: Position) : Token("<>", pos = pos)
    class LESS(pos: Position) : Token("<", pos = pos)
    class GREATER(pos: Position) : Token(">", pos = pos)
    class LESSEQUAL(pos: Position) : Token("<=", pos = pos)
    class GREATEREQUAL(pos: Position) : Token(">=", pos = pos)
    class NOT(pos: Position) : Token("!", pos = pos)
    class AND(pos: Position) : Token("&", pos = pos)
    class OR(pos: Position) : Token("|", pos = pos)
    class COMMA(pos: Position) : Token(",", pos = pos)
    class STRING(value: String, pos: Position) : Token("string", value, pos)
    class LSB(pos: Position) : Token("[", pos = pos)
    class RSB(pos: Position) : Token("]", pos = pos)
    class GET(pos: Position) : Token("~", pos = pos)
    class COLON(pos: Position) : Token(":", pos = pos)
    class NEWLINE(pos: Position) : Token("\\n", pos = pos)
    class CURLYLEFT(pos: Position) : Token("{", pos = pos)
    class CURLYRIGHT(pos: Position) : Token("}", pos = pos)
    class EOF(pos: Position) : Token("'end of file'", pos = pos)

    companion object {
        val keywords = arrayOf(
            "VAR", "IF", "THEN", "ELSE", "FOR", "TO", "STEP", "WHILE", "FUN", "END", "RETURN", "BREAK", "CONTINUE"
        )
    }

    override fun toString(): String {
        return if (this.value != null) "$type:$value" else type
    }
}