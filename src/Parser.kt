class Parser(private val tokens: Array<Token>) {
    private var tokenIndex = -1
    private var currentToken: Token? = null

    init {
        advance()
    }

    private fun advance() {
        tokenIndex++
        this.currentToken = if (this.tokenIndex < this.tokens.size) this.tokens[this.tokenIndex]
        else null
    }

    fun parse(): Node {
        if (this.tokens[0] is Token.EOF) return Node.BreakNode()
        val result = this.statement()
        if (this.currentToken !is Token.EOF && this.currentToken != null) fail(
            InvalidSyntaxError(
                "Expected expression, got ${this.currentToken}", this.currentToken!!.pos
            ), "Parsing"
        )
        return result
    }

    private fun atom(): Node {
        when (this.currentToken) {
            is Token.INT, is Token.FLOAT -> {
                val token = this.currentToken!!
                this.advance()
                return Node.NumberNode(token)
            }

            is Token.IDENTIFIER -> {
                val token = this.currentToken!!
                this.advance()
                if (this.currentToken is Token.LPAREN) {
                    this.advance()
                    val argNodeList = arrayListOf<Node>()
                    if (this.currentToken is Token.RPAREN) {
                        this.advance()
                        return Node.FunCallNode(token, argNodeList.toTypedArray())
                    } else argNodeList.add(this.opExpr())
                    while (this.currentToken!! is Token.COMMA) {
                        this.advance()
                        val param = this.opExpr()
                        argNodeList.add(param)
                    }
                    if (this.currentToken!! !is Token.RPAREN) fail(
                        InvalidSyntaxError(
                            "Expected ')', got ${this.currentToken}", this.currentToken?.pos!!
                        ), "Parsing"
                    )
                    this.advance()
                    return Node.FunCallNode(token, argNodeList.toTypedArray())
                }
                return Node.VarAccessNode(token)
            }

            is Token.LPAREN -> {
                this.advance()
                val expression = this.expression()
                if (this.currentToken is Token.RPAREN) {
                    this.advance()
                    return expression
                } else fail(
                    InvalidSyntaxError("Expected ')', got ${this.currentToken}", this.currentToken!!.pos), "Parsing"
                )

            }

            is Token.STRING -> {
                val token = this.currentToken!!
                this.advance()
                return Node.StringNode(token)
            }

            is Token.LSB -> {
                val list = arrayListOf<Node>()
                this.advance()
                if (this.currentToken is Token.RSB) this.advance()
                else {
                    list.add(this.atom())
                    while (this.currentToken is Token.COMMA) {
                        this.advance()
                        list.add(this.atom())
                    }
                    if (this.currentToken !is Token.RSB) fail(
                        InvalidSyntaxError(
                            "Expected ']' or ',', got ${this.currentToken}", this.currentToken!!.pos
                        ), "Parsing"
                    )
                    this.advance()
                }
                return Node.ListNode(list.toTypedArray())
            }

            is Token.KEYWORD -> {
                when (this.currentToken?.value) {
                    "IF" -> {
                        this.advance()
                        val bool = this.opExpr()

                        val ifExpr = when (this.currentToken) {
                            is Token.CURLYLEFT -> {
                                this.advance()
                                this.statement()
                            }

                            is Token.COLON -> {
                                this.advance()
                                this.expression()
                            }

                            else -> {
                                fail(
                                    InvalidSyntaxError(
                                        "Expected '{' or ':', got ${this.currentToken}", this.currentToken!!.pos
                                    ), "Parsing"
                                )
                            }
                        }

                        var elseExpr: Node? = null
                        if (this.currentToken is Token.KEYWORD && this.currentToken?.value == "ELSE") {
                            this.advance()
                            elseExpr = when (this.currentToken) {
                                is Token.CURLYLEFT -> {
                                    this.advance()
                                    this.statement()
                                }

                                is Token.COLON -> {
                                    this.advance()
                                    this.expression()
                                }

                                else -> {
                                    fail(
                                        InvalidSyntaxError(
                                            "Expected '{' or ':', got ${this.currentToken}", this.currentToken!!.pos
                                        ), "Parsing"
                                    )
                                }
                            }
                        }
                        return Node.IfNode(bool, ifExpr, elseExpr)
                    }

                    else -> fail(
                        InvalidSyntaxError(
                            "Expected Value or if, got ${this.currentToken}", this.currentToken!!.pos
                        ), "Parsing"
                    )
                }
            }

            else -> {
                val token = this.currentToken!!
                fail(
                    InvalidSyntaxError(
                        "Expected Value, got $token", token.pos.copy()
                    ), "Parsing"
                )
            }
        }
    }


    private fun power(): Node {
        var left = this.atom()
        if (this.currentToken is Token.GET) {
            val operatorToken = this.currentToken!!
            this.advance()
            val right = this.expression()
            left = Node.BinOpNode(left, operatorToken, right)
        }
        while (this.currentToken is Token.POW) {
            val operatorToken = this.currentToken!!
            this.advance()
            val right = this.factor()
            left = Node.BinOpNode(left, operatorToken, right)
        }
        return left
    }

    private fun factor(): Node {
        val token = this.currentToken
        while (token is Token.PLUS || token is Token.MINUS) {
            val unary = this.currentToken!!
            this.advance()
            val right = this.power()
            return Node.UnaryOpNode(unary, right)
        }
        return power()
    }

    private fun term(): Node {
        var left: Node = this.factor()
        while (this.currentToken is Token.MUL || this.currentToken is Token.DIV) {
            val operatorToken = this.currentToken!!
            this.advance()
            val right = this.factor()
            left = Node.BinOpNode(left, operatorToken, right)
        }
        return left
    }

    private fun arithmExpr(): Node {
        var left: Node = this.term()
        while (this.currentToken is Token.PLUS || this.currentToken is Token.MINUS) {
            val operator = this.currentToken!!
            this.advance()
            val right = this.term()
            left = Node.BinOpNode(left, operator, right)
        }
        return left
    }

    private fun compExpr(): Node {
        if (this.currentToken is Token.NOT) {
            val operator = this.currentToken!!
            this.advance()
            return Node.UnaryOpNode(operator, this.compExpr())
        }

        var left: Node = this.arithmExpr()
        while (this.currentToken is Token.EE || this.currentToken is Token.NE || this.currentToken is Token.LESS || this.currentToken is Token.GREATER || this.currentToken is Token.LESSEQUAL || this.currentToken is Token.GREATEREQUAL) {
            val operator = this.currentToken!!
            this.advance()
            val right = this.arithmExpr()
            left = Node.BinOpNode(left, operator, right)
        }
        return left
    }

    private fun opExpr(): Node {
        var left: Node = this.compExpr()
        while (this.currentToken is Token.AND || this.currentToken is Token.OR) {
            val operator = this.currentToken!!
            this.advance()
            val right = this.compExpr()
            left = Node.BinOpNode(left, operator, right)
        }
        return left
    }

    private fun expression(): Node {
        ignoreNewLines()
        val token = this.currentToken
        if (token is Token.KEYWORD) when (token.value) {
            "VAR" -> {
                this.advance()
                if (this.currentToken !is Token.IDENTIFIER) fail(
                    InvalidSyntaxError(
                        "Expected identifier, got ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                val varName = this.currentToken
                this.advance()
                if (this.currentToken !is Token.ASSIGN) fail(
                    InvalidSyntaxError("Expected <, got ${this.currentToken}", this.currentToken!!.pos), "Parsing"
                )
                this.advance()
                val expr = this.expression()
                return Node.VarAssignNode(varName!!, expr)
            }

            "WHILE" -> {
                this.advance()
                val bool = this.opExpr()
                val expr = when (this.currentToken) {

                    is Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    is Token.CURLYLEFT -> {
                        this.advance()
                        this.statement()
                    }

                    else -> {
                        fail(
                            InvalidSyntaxError(
                                "Expected '{' or ':', got ${this.currentToken}", this.currentToken!!.pos
                            ), "Parsing"
                        )
                    }
                }
                return Node.WhileNode(bool, expr)
            }

            "FOR" -> {
                this.advance()
                if (this.currentToken !is Token.IDENTIFIER) fail(
                    InvalidSyntaxError(
                        "Expected identifier, for ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                val identifier = this.currentToken!!
                this.advance()
                if (this.currentToken !is Token.ASSIGN) fail(
                    InvalidSyntaxError(
                        "Expected <, got ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                this.advance()
                val from = this.factor()
                if (this.currentToken !is Token.KEYWORD || this.currentToken?.value != "TO") fail(
                    InvalidSyntaxError(
                        "Expected TO, for ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                this.advance()
                val to = this.arithmExpr()
                var step: Node? = null
                if (this.currentToken is Token.KEYWORD && this.currentToken?.value == "STEP") {
                    this.advance()
                    step = this.factor()
                }
                val expr = when (this.currentToken) {
                    is Token.CURLYLEFT -> {
                        this.advance()
                        this.statement()
                    }

                    is Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    else -> fail(
                        InvalidSyntaxError(
                            "Expected THEN, for ${this.currentToken}", this.currentToken!!.pos
                        ), "Parsing"
                    )
                }
                return Node.ForNode(identifier, from, to, step, expr)
            }

            "FUN" -> {
                this.advance()
                if (this.currentToken!! !is Token.IDENTIFIER) fail(
                    InvalidSyntaxError(
                        "Expected Identifier, got ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                val identifier = this.currentToken!!
                this.advance()
                if (this.currentToken!! !is Token.LPAREN) fail(
                    InvalidSyntaxError(
                        "Expected '(', got ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                this.advance()
                val argList = arrayListOf<Token.IDENTIFIER>()
                if (this.currentToken!! is Token.IDENTIFIER) {
                    argList.add(this.currentToken!! as Token.IDENTIFIER)
                    this.advance()
                }
                while (this.currentToken!! is Token.COMMA) {
                    this.advance()
                    if (this.currentToken!! !is Token.IDENTIFIER) fail(
                        InvalidSyntaxError(
                            "Expected Identifier, got ${this.currentToken}", this.currentToken!!.pos
                        ), "Parsing"
                    )
                    argList.add(this.currentToken!! as Token.IDENTIFIER)
                    this.advance()
                }
                val argNames = argList.toTypedArray()
                if (this.currentToken!! !is Token.RPAREN) fail(
                    InvalidSyntaxError(
                        "Expected ')', got ${this.currentToken}", this.currentToken!!.pos
                    ), "Parsing"
                )
                this.advance()
                val bodyNode = when (this.currentToken) {
                    is Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    is Token.CURLYLEFT -> {
                        this.advance()
                        this.statement()
                    }

                    else -> fail(
                        InvalidSyntaxError(
                            "Expected '{', or ':', got ${this.currentToken}", this.currentToken!!.pos
                        ), "Parsing"
                    )
                }
                return Node.FunDefNode(identifier, argNames, bodyNode)
            }

            "RETURN" -> {
                this.advance()
                if (this.currentToken is Token.NEWLINE || this.currentToken is Token.EOF) return Node.ReturnNode(null)
                return Node.ReturnNode(this.opExpr())
            }

            "BREAK" -> {
                this.advance()
                return Node.BreakNode()
            }

            "CONTINUE" -> {
                this.advance()
                return Node.ContinueNode()
            }
        }
        return opExpr()
    }

    private fun statement(): Node {
        ignoreNewLines()
        val statements = arrayListOf<Node>()
        statements.add(this.expression())
        while (this.currentToken is Token.NEWLINE) {
            while (this.currentToken is Token.NEWLINE) this.advance()
            if (this.currentToken is Token.CURLYRIGHT || this.currentToken!! is Token.EOF) {
                this.advance()
                break
            }
            statements.add(this.expression())
        }
        return Node.StatementNode(statements.toTypedArray())
    }

    private fun ignoreNewLines() {
        while (this.currentToken is Token.NEWLINE) this.advance()
    }
}