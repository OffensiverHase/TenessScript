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
        s.deleteCharAt(27) //Delete the '['
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
                    tokenQueue.put(Token(Token.NEWLINE, null,this.pos.copy()))
                    this.advance()
                }

                '+' -> {
                    tokenQueue.put(Token(Token.PLUS,null,this.pos.copy()))
                    this.advance()
                }

                '-' -> {
                    tokenQueue.put(Token(Token.MINUS,null,this.pos.copy()))
                    this.advance()
                }

                '*' -> {
                    tokenQueue.put(Token(Token.MUL,null,this.pos.copy()))
                    this.advance()
                }

                '/' -> {
                    tokenQueue.put(Token(Token.DIV,null,this.pos.copy()))
                    this.advance()
                }

                '^' -> {
                    tokenQueue.put(Token(Token.POW,null,this.pos.copy()))
                    this.advance()
                }

                '(' -> {
                    tokenQueue.put(Token(Token.LPAREN,null,this.pos.copy()))
                    this.advance()
                }

                ')' -> {
                    tokenQueue.put(Token(Token.RPAREN,null,this.pos.copy()))
                    this.advance()
                }

                '!' -> {
                    tokenQueue.put(Token(Token.NOT,null,this.pos.copy()))
                    this.advance()
                }

                '=' -> {
                    tokenQueue.put(Token(Token.EQUALS,null,this.pos.copy()))
                    this.advance()
                }

                '&' -> {
                    tokenQueue.put(Token(Token.AND,null,this.pos.copy()))
                    this.advance()
                }

                '|' -> {
                    tokenQueue.put(Token(Token.OR,null,this.pos.copy()))
                    this.advance()
                }

                ',' -> {
                    tokenQueue.put(Token(Token.COMMA,null,this.pos.copy()))
                    this.advance()
                }

                '[' -> {
                    tokenQueue.put(Token(Token.LSQUARE,null,this.pos.copy()))
                    this.advance()
                }

                ']' -> {
                    tokenQueue.put(Token(Token.RSQUARE,null,this.pos.copy()))
                    this.advance()
                }

                '~' -> {
                    tokenQueue.put(Token(Token.GET,null,this.pos.copy()))
                    this.advance()
                }

                ':' -> {
                    tokenQueue.put(Token(Token.COLON,null,this.pos.copy()))
                    this.advance()
                }

                '{' -> {
                    tokenQueue.put(Token(Token.LCURLY,null,this.pos.copy()))
                    this.advance()
                }

                '}' -> {
                    tokenQueue.put(Token(Token.RCURLY,null,this.pos.copy()))
                    this.advance()
                }

                '.' -> {
                    tokenQueue.put(makeDotThings())
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
                    fail(IllegalCharError("Unknown char ${this.currentChar}", this.pos.copy()))
                    tokenQueue.put(Token(Token.EOF,null,this.pos.copy()))
                    break
                }
            }
        }
        tokenQueue.put(Token(Token.EOF,null,this.pos.copy()))
    }

    private fun makeIdentifier(): Token {
        var idString = ""
        val startPos = this.pos.copy()

        while (this.currentChar != null && this.currentChar!! in lettersDigits) {
            idString += this.currentChar!!
            this.advance()
        }

        return if (idString.uppercase(Locale.getDefault()) in Token.keywords) Token(Token.KEYWORD,
            idString.uppercase(Locale.getDefault()), startPos
        )
        else Token(Token.IDENTIFIER,idString, startPos)
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
        return if (dotCount == 0) Token(Token.INT,numberString.toInt().toString(), this.pos.copy())
        else Token(Token.FLOAT,numberString.toFloat().toString(), this.pos.copy())
    }

    private fun makeString(): Token {
        val pos = this.pos.copy()
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
        return Token(Token.STRING,string, pos)
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
                val pos = this.pos.copy()
                this.advance()
                Token(Token.LESSEQUAL,null,pos)
            }

            '>' -> {
                val pos = this.pos.copy()
                this.advance()
                Token(Token.UNEQUALS,null,pos)
            }

            '-' -> {
                val pos = this.pos.copy()
                this.advance()
                Token(Token.ASSIGN,null,pos)
            }

            else -> Token(Token.LESS,null,this.pos.copy())
        }
    }

    private fun makeBiggerThings(): Token {
        this.advance()
        return if (this.currentChar == '=') {
            val pos = this.pos.copy()
            this.advance()
            Token(Token.GREATEREQUAL,null,pos)
        } else Token(Token.GREATER,null,this.pos.copy())
    }

    private fun makeDotThings(): Token {
        val pos = this.pos.copy()
        this.advance()
        return if (this.currentChar != '.') Token(Token.DOT,null,pos)
        else Token(Token.TO,null,pos)
    }

    override fun run() {
        this.makeTokens()
    }
}
