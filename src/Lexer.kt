import java.util.*
import java.util.concurrent.BlockingQueue

class Lexer(private val text: String, fileName: String, private val tokenQueue: BlockingQueue<Token>): Runnable {
    private var pos = Position(-1, 0, -1, fileName, text)
    private var currentChar: Char? = null
    private val digits = "1234567890"
    private val letters: String
    private val lettersDigits: String
    private val escapeChars = mapOf(Pair('n', '\n'), Pair('t', '\t'))

    init {
        val s = StringBuilder()
        for (i in 64..91) s.append(i.toChar())
        for (i in 97..122) s.append(i.toChar())
        s.append('_')
        s.append('$')
        letters = s.toString()

        lettersDigits = digits + letters

        this.advance()
    }

    private fun advance() {
        this.pos.advance(this.currentChar ?: ' ')
        this.currentChar = if (this.pos.index < this.text.length) this.text[pos.index] else null
    }

    private fun makeTokens() {
        while (this.currentChar != null) {
            when (this.currentChar!!) {
                in " \t\r" -> this.advance()

                '#' -> {
                    removeComment()
                }

                '\n', ';' -> {
                    tokenQueue.put(Token.NEWLINE(this.pos))
                    this.advance()
                }

                '+' -> {
                    tokenQueue.put(Token.PLUS(this.pos))
                    this.advance()
                }

                '-' -> {
                    tokenQueue.put(Token.MINUS(this.pos))
                    this.advance()
                }

                '*' -> {
                    tokenQueue.put(Token.MUL(this.pos))
                    this.advance()
                }

                '/' -> {
                    tokenQueue.put(Token.DIV(this.pos))
                    this.advance()
                }

                '^' -> {
                    tokenQueue.put(Token.POW(this.pos))
                    this.advance()
                }

                '(' -> {
                    tokenQueue.put(Token.LPAREN(this.pos))
                    this.advance()
                }

                ')' -> {
                    tokenQueue.put(Token.RPAREN(this.pos))
                    this.advance()
                }

                '!' -> {
                    tokenQueue.put(Token.NOT(this.pos))
                    this.advance()
                }

                '=' -> {
                    tokenQueue.put(Token.EE(this.pos))
                    this.advance()
                }

                '&' -> {
                    tokenQueue.put(Token.AND(this.pos))
                    this.advance()
                }

                '|' -> {
                    tokenQueue.put(Token.OR(this.pos))
                    this.advance()
                }

                ',' -> {
                    tokenQueue.put(Token.COMMA(this.pos))
                    this.advance()
                }

                '[' -> {
                    tokenQueue.put(Token.LSB(this.pos))
                    this.advance()
                }

                ']' -> {
                    tokenQueue.put(Token.RSB(this.pos))
                    this.advance()
                }

                '~' -> {
                    tokenQueue.put(Token.GET(this.pos))
                    this.advance()
                }

                ':' -> {
                    tokenQueue.put(Token.COLON(this.pos))
                    this.advance()
                }

                '{' -> {
                    tokenQueue.put(Token.CURLYLEFT(this.pos))
                    this.advance()
                }

                '}' -> {
                    tokenQueue.put(Token.CURLYRIGHT(this.pos))
                    this.advance()
                }

                '<' -> {
                    tokenQueue.put(makeSmallerThings())
                }

                '>' -> {
                    tokenQueue.put(makeBiggerThings())
                }

                '\'' -> {
                    tokenQueue.put(makeString())
                }

                in letters -> {
                    tokenQueue.put(makeIdentifier())
                }

                in digits -> {
                    tokenQueue.put(makeNumber())
                }

                else -> {
                    fail(IllegalCharError("Unknown char ${this.currentChar}", this.pos))
                    tokenQueue.put(Token.EOF(this.pos))
                    break
                }
            }
        }
        tokenQueue.put(Token.EOF(this.pos))
    }

    private fun makeIdentifier(): Token {
        var idString = ""
        val startPos = this.pos.copy()

        while (this.currentChar != null && this.currentChar!! in lettersDigits) {
            idString += this.currentChar!!
            this.advance()
        }

        return if (idString.uppercase(Locale.getDefault()) in Token.keywords) Token.KEYWORD(
            idString.uppercase(Locale.getDefault()), startPos
        )
        else Token.IDENTIFIER(idString, startPos)
    }

    private fun makeNumber(): Token {
        var numberString = ""
        var dotCount = 0
        while (this.currentChar != null && this.currentChar!! in "$digits.") {
            if (this.currentChar == '.') {
                if (dotCount == 0) dotCount++
                else break
            }
            numberString += currentChar
            this.advance()
        }
        return if (dotCount == 0) Token.INT(numberString.toInt(), this.pos)
        else Token.FLOAT(numberString.toFloat(), this.pos)
    }

    private fun makeString(): Token {
        val pos = this.pos
        this.advance()
        var string = ""
        var escape = false
        while (this.currentChar != null && this.currentChar != '\'' || escape) {
            if (escape) {
                escape = false
                string += this.escapeChars.getOrElse(this.currentChar!!) { this.currentChar }
            } else if (this.currentChar == '\\') escape = true
            else string += this.currentChar!!
            this.advance()
        }
        if (this.currentChar == null) fail(InvalidSyntaxError("Unclosed String Literal", pos))
        this.advance()
        return Token.STRING(string, pos)
    }

    private fun removeComment() {
        while (this.currentChar != null && this.currentChar != '\n') {
            this.advance()
        }
        this.advance()
    }

    private fun makeSmallerThings(): Token {
        this.advance()
        return when (this.currentChar) {
            '=' -> {
                val pos = this.pos
                this.advance()
                Token.LESSEQUAL(pos)
            }

            '>' -> {
                val pos = this.pos
                this.advance()
                Token.NE(pos)
            }

            '-' -> {
                val pos = this.pos
                this.advance()
                Token.ASSIGN(pos)
            }

            else -> Token.LESS(this.pos)
        }
    }

    private fun makeBiggerThings(): Token {
        this.advance()
        return if (this.currentChar == '=') {
            val pos = this.pos
            this.advance()
            Token.GREATEREQUAL(pos)
        } else Token.GREATER(this.pos)
    }

    override fun run() {
        this.makeTokens()
    }
}
