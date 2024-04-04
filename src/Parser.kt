import java.util.concurrent.LinkedBlockingQueue

class Parser(private val tokenQueue: LinkedBlockingQueue<Token>) {
    private lateinit var currentToken: Token

    init {
        advance()
    }

    private fun advance() {
        this.currentToken = this.tokenQueue.take()
    }

    fun parse(): Result<Node> {
        if (this.currentToken.type == Token.EOF) return Result.success(Node.BreakNode)
        val result = this.statement()
        if (this.currentToken.type != Token.EOF && this.currentToken.type != Token.NEWLINE) return Result.failure(
            InvalidSyntaxError(
                "Expected expression, got ${this.currentToken}", this.currentToken.pos
            )
        )
        return result
    }

    private fun atom(): Result<Node> {
        when (this.currentToken.type) {
            Token.INT, Token.FLOAT -> {
                val token = this.currentToken
                this.advance()
                return Result.success(Node.NumberNode(token))
            }

            Token.IDENTIFIER -> {
                val token = this.currentToken
                this.advance()
                if (this.currentToken.type == Token.LPAREN) {
                    this.advance()
                    val argNodeList = arrayListOf<Node>()
                    if (this.currentToken.type == Token.RPAREN) {
                        this.advance()
                        return Result.success(Node.FunCallNode(token, argNodeList.toTypedArray()))
                    } else argNodeList.add(this.opExpr().getOrElse { return Result.failure(it) })
                    while (this.currentToken.type == Token.COMMA) {
                        this.advance()
                        val param = this.opExpr()
                        argNodeList.add(param.getOrElse { return Result.failure(it) })
                    }
                    if (this.currentToken.type != Token.RPAREN) return Result.failure(
                        InvalidSyntaxError(
                            "Expected ')', got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                    this.advance()
                    return Result.success(Node.FunCallNode(token, argNodeList.toTypedArray()))
                } else if (this.currentToken.type == Token.DOT) {
                    this.advance()
                    if (this.currentToken.type != Token.IDENTIFIER) {
                        val e = InvalidSyntaxError(
                            "Expected indetifier after '.', got ${this.currentToken}", this.currentToken.pos
                        )
                        return Result.failure(e)
                    }
                    val key = this.currentToken
                    this.advance()
                    if (this.currentToken.type == Token.ASSIGN) {
                        this.advance()
                        val value = this.atom().getOrElse { return Result.failure(it) }
                        return Result.success(Node.ObjectAssignNode(Node.VarAccessNode(token), key, value))
                    }
                    return Result.success(Node.ObjectReadNode(Node.VarAccessNode(token), key))
                }
                return Result.success(Node.VarAccessNode(token))
            }

            Token.LPAREN -> {
                this.advance()
                val expression = this.expression()
                if (this.currentToken.type == Token.RPAREN) {
                    this.advance()
                    return expression
                } else return Result.failure(
                    InvalidSyntaxError("Expected ')', got ${this.currentToken}", this.currentToken.pos)
                )

            }

            Token.STRING -> {
                val token = this.currentToken
                this.advance()
                return Result.success(Node.StringNode(token))
            }

            Token.LSQUARE -> {
                val list = arrayListOf<Node>()
                this.advance()
                if (this.currentToken.type == Token.RSQUARE) this.advance()
                else {
                    list.add(this.atom().getOrElse { return Result.failure(it) })
                    while (this.currentToken.type == Token.COMMA) {
                        this.advance()
                        list.add(this.atom().getOrElse { return Result.failure(it) })
                    }
                    if (this.currentToken.type != Token.RSQUARE) return Result.failure(
                        InvalidSyntaxError(
                            "Expected ']' or ',', got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                    this.advance()
                }
                return Result.success(Node.ListNode(list.toTypedArray()))
            }

            Token.KEYWORD -> {
                when (this.currentToken.value) {
                    "IF" -> {
                        this.advance()
                        val bool = this.opExpr()

                        val ifExpr = when (this.currentToken.type) {
                            Token.LCURLY -> {
                                this.advance()
                                this.statement()
                            }

                            Token.COLON -> {
                                this.advance()
                                this.expression()
                            }

                            else -> {
                                return Result.failure(
                                    InvalidSyntaxError(
                                        "Expected '{' or ':', got ${this.currentToken}", this.currentToken.pos
                                    )
                                )
                            }
                        }

                        var elseExpr: Node? = null
                        if (this.currentToken.type == Token.KEYWORD && this.currentToken.value == "ELSE") {
                            this.advance()
                            elseExpr = when (this.currentToken.type) {
                                Token.LCURLY -> {
                                    this.advance()
                                    this.statement().getOrElse { return Result.failure(it) }
                                }

                                Token.COLON -> {
                                    this.advance()
                                    this.expression().getOrElse { return Result.failure(it) }
                                }

                                else -> {
                                    return Result.failure(
                                        InvalidSyntaxError(
                                            "Expected '{' or ':', got ${this.currentToken}", this.currentToken.pos
                                        )
                                    )
                                }
                            }
                        }
                        return Result.success(
                            Node.IfNode(bool.getOrElse { return Result.failure(it) },
                                ifExpr.getOrElse { return Result.failure(it) },
                                elseExpr
                            )
                        )
                    }

                    "OBJECT" -> {
                        this.advance()
                        if (this.currentToken.type != Token.LCURLY) {
                            val e = InvalidSyntaxError(
                                "Expected { after keyword object, got ${this.currentToken}", this.currentToken.pos
                            )
                            return Result.failure(e)
                        }
                        this.advance()
                        this.ignoreNewLines()
                        val map = mutableMapOf<Token, Node>()
                        while (this.currentToken.type == Token.IDENTIFIER) {
                            val key = this.currentToken
                            this.advance()
                            if (this.currentToken.type != Token.ASSIGN) {
                                val e =
                                    InvalidSyntaxError("Expected <- , got ${this.currentToken}", this.currentToken.pos)
                                return Result.failure(e)
                            }
                            this.advance()
                            val value = this.atom()
                            map[key] = value.getOrElse { return Result.failure(it) }
                            if (this.currentToken.type != Token.NEWLINE) {
                                val e = InvalidSyntaxError(
                                    "Expected newline, ';' or }, got ${this.currentToken}", this.currentToken.pos
                                )
                                return Result.failure(e)
                            }
                            this.advance()
                            this.ignoreNewLines()
                        }
                        if (this.currentToken.type == Token.RCURLY) this.advance()
                        return Result.success(Node.ObjectNode(map))
                    }

                    else -> return Result.failure(
                        InvalidSyntaxError(
                            "Expected identifier, literal or if, got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                }
            }

            else -> {
                val token = this.currentToken
                return Result.failure(
                    InvalidSyntaxError(
                        "Expected identifier, literal or if got $token", token.pos.copy()
                    )
                )
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun power(): Result<Node> {
        var left = this.atom()
        if (this.currentToken.type == Token.LSQUARE) {
            val operatorToken = Token(Token.GET, null, this.currentToken.pos)
            this.advance()
            val right = this.arithmExpr()
            if (this.currentToken.type != Token.RSQUARE) {
                val e = InvalidSyntaxError("Expected ] after [ with list index", this.currentToken.pos)
                return Result.failure(e)
            }
            this.advance()
            left = Result.success(
                Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                    operatorToken,
                    right.getOrElse { return Result.failure(it) })
            )
            if (this.currentToken.type == Token.ASSIGN) {
                this.advance()
                val right = this.atom()
                val l = left.getOrElse { return Result.failure(it) }
                val listNode =
                    if (l.leftNode is Node.ListNode || l.leftNode is Node.VarAccessNode || l.leftNode is Node.ObjectReadNode) l.leftNode
                    else return Result.failure(
                        TypeError(
                            "Cannot index non List, got ${l.leftNode}", this.currentToken.pos
                        )
                    )
                val index =
                    if (l.rightNode is Node.NumberNode || l.rightNode is Node.VarAccessNode || l.leftNode is Node.ObjectReadNode) l.rightNode
                    else return Result.failure(
                        TypeError(
                            "Cannot index List with non Number, got ${l.rightNode}", this.currentToken.pos
                        )
                    )
                left = Result.success(
                    Node.ListAssignNode(listNode, index, right.getOrElse { return Result.failure(it) })
                )
            }
        }
        while (this.currentToken.type == Token.POW) {
            val operatorToken = this.currentToken
            this.advance()
            val right = this.factor()
            left = Result.success(Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                operatorToken,
                right.getOrElse {
                    return Result.failure(
                        it
                    )
                })
            )
        }
        return left
    }

    private fun factor(): Result<Node> {
        val token = this.currentToken
        while (token.type == Token.PLUS || token.type == Token.MINUS) {
            val unary = this.currentToken
            this.advance()
            val right = this.power()
            return Result.success(Node.UnaryOpNode(unary, right.getOrElse { return Result.failure(it) }))
        }
        return power()
    }

    private fun term(): Result<Node> {
        var left = this.factor()
        while (this.currentToken.type == Token.MUL || this.currentToken.type == Token.DIV) {
            val operatorToken = this.currentToken
            this.advance()
            val right = this.factor()
            left = Result.success(
                Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                    operatorToken,
                    right.getOrElse { return Result.failure(it) })
            )
        }
        return left
    }

    private fun arithmExpr(): Result<Node> {
        var left = this.term()
        while (this.currentToken.type == Token.PLUS || this.currentToken.type == Token.MINUS) {
            val operator = this.currentToken
            this.advance()
            val right = this.term()
            left = Result.success(
                Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                    operator,
                    right.getOrElse { return Result.failure(it) })
            )
        }
        return left
    }

    private fun compExpr(): Result<Node> {
        if (this.currentToken.type == Token.NOT) {
            val operator = this.currentToken
            this.advance()
            return Result.success(Node.UnaryOpNode(operator, this.compExpr().getOrElse { return Result.failure(it) }))
        }

        var left = this.arithmExpr()
        while (this.currentToken.type == Token.EQUALS || this.currentToken.type == Token.UNEQUALS || this.currentToken.type == Token.LESS || this.currentToken.type == Token.GREATER || this.currentToken.type == Token.LESSEQUAL || this.currentToken.type == Token.GREATEREQUAL) {
            val operator = this.currentToken
            this.advance()
            val right = this.arithmExpr()
            left = Result.success(
                Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                    operator,
                    right.getOrElse { return Result.failure(it) })
            )
        }
        return left
    }

    private fun opExpr(): Result<Node> {
        var left = this.compExpr()
        while (this.currentToken.type == Token.AND || this.currentToken.type == Token.OR) {
            val operator = this.currentToken
            this.advance()
            val right = this.compExpr()
            left = Result.success(
                Node.BinOpNode(left.getOrElse { return Result.failure(it) },
                    operator,
                    right.getOrElse { return Result.failure(it) })
            )
        }
        return left
    }

    private fun expression(): Result<Node> {
        ignoreNewLines()
        val token = this.currentToken
        if (token.type == Token.KEYWORD) when (token.value) {
            "WHILE" -> {
                this.advance()
                val bool = this.opExpr()
                val expr = when (this.currentToken.type) {

                    Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    Token.LCURLY -> {
                        this.advance()
                        this.statement()
                    }

                    else -> {
                        return Result.failure(
                            InvalidSyntaxError(
                                "Expected '{' or ':', got ${this.currentToken}", this.currentToken.pos
                            )
                        )
                    }
                }
                return Result.success(Node.WhileNode(bool.getOrElse { return Result.failure(it) },
                    expr.getOrElse { return Result.failure(it) })
                )
            }

            "FOR" -> {
                this.advance()
                if (this.currentToken.type != Token.IDENTIFIER) return Result.failure(
                    InvalidSyntaxError(
                        "Expected identifier, in for, got ${this.currentToken}", this.currentToken.pos
                    )
                )
                val identifier = this.currentToken
                this.advance()
                if (this.currentToken.type != Token.ASSIGN) return Result.failure(
                    InvalidSyntaxError(
                        "Expected <-, got ${this.currentToken}", this.currentToken.pos
                    )
                )
                this.advance()
                val from = this.factor()
                if (this.currentToken.type != Token.TO) return Result.failure(
                    InvalidSyntaxError(
                        "Expected .. in for, got ${this.currentToken}", this.currentToken.pos
                    )
                )
                this.advance()
                val to = this.arithmExpr()
                var step: Node? = null
                if (this.currentToken.type == Token.KEYWORD && this.currentToken.value == "STEP") {
                    this.advance()
                    step = this.factor().getOrElse { return Result.failure(it) }
                }
                val expr = when (this.currentToken.type) {
                    Token.LCURLY -> {
                        this.advance()
                        this.statement()
                    }

                    Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    else -> return Result.failure(
                        InvalidSyntaxError(
                            "Expected { or :, got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                }
                return Result.success(
                    Node.ForNode(identifier,
                        from.getOrElse { return Result.failure(it) },
                        to.getOrElse { return Result.failure(it) },
                        step,
                        expr.getOrElse { return Result.failure(it) })
                )
            }

            "FUN" -> {
                this.advance()
                if (this.currentToken.type != Token.IDENTIFIER) return Result.failure(
                    InvalidSyntaxError(
                        "Expected Identifier, got ${this.currentToken}", this.currentToken.pos
                    )
                )
                val identifier = this.currentToken
                this.advance()
                if (this.currentToken.type != Token.LPAREN) return Result.failure(
                    InvalidSyntaxError(
                        "Expected '(', got ${this.currentToken}", this.currentToken.pos
                    )
                )
                this.advance()
                val argList = arrayListOf<Token>()
                if (this.currentToken.type == Token.IDENTIFIER) {
                    argList.add(this.currentToken)
                    this.advance()
                }
                while (this.currentToken.type == Token.COMMA) {
                    this.advance()
                    if (this.currentToken.type != Token.IDENTIFIER) return Result.failure(
                        InvalidSyntaxError(
                            "Expected Identifier, got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                    argList.add(this.currentToken)
                    this.advance()
                }
                val argNames = argList.toTypedArray()
                if (this.currentToken.type != Token.RPAREN) return Result.failure(
                    InvalidSyntaxError(
                        "Expected ')', got ${this.currentToken}", this.currentToken.pos
                    )
                )
                this.advance()
                val bodyNode = when (this.currentToken.type) {
                    Token.COLON -> {
                        this.advance()
                        this.expression()
                    }

                    Token.LCURLY -> {
                        this.advance()
                        this.statement()
                    }

                    else -> return Result.failure(
                        InvalidSyntaxError(
                            "Expected '{', or ':', got ${this.currentToken}", this.currentToken.pos
                        )
                    )
                }
                return Result.success(
                    Node.FunDefNode(identifier, argNames, bodyNode.getOrElse { return Result.failure(it) })
                )
            }

            "RETURN" -> {
                this.advance()
                if (this.currentToken.type == Token.NEWLINE || this.currentToken.type == Token.EOF) return Result.success(
                    Node.ReturnNode(
                        null
                    )
                )
                return Result.success(Node.ReturnNode(this.opExpr().getOrElse { return Result.failure(it) }))
            }

            "BREAK" -> {
                this.advance()
                return Result.success(Node.BreakNode)
            }

            "CONTINUE" -> {
                this.advance()
                return Result.success(Node.ContinueNode)
            }
        } else if (this.currentToken.type == Token.IDENTIFIER) {
            val varName = this.currentToken
            while (this.tokenQueue.peek() == null) Thread.sleep(0, 1)
            if (this.tokenQueue.peek().type == Token.ASSIGN) {
                this.advance() // Advance to the <-
                this.advance() // Advance past the <-
                val expr = this.expression()
                return Result.success(Node.VarAssignNode(varName, expr.getOrElse { return Result.failure(it) }))
            }
        }
        return opExpr()
    }

    private fun statement(): Result<Node> {
        ignoreNewLines()
        val statements = arrayListOf<Node>()
        statements.add(this.expression().getOrElse { return Result.failure(it) })
        while (this.currentToken.type == Token.NEWLINE) {
            while (this.currentToken.type == Token.NEWLINE) this.advance()
            if (this.currentToken.type == Token.RCURLY || this.currentToken.type == Token.EOF) {
                this.advance()
                break
            }
            statements.add(this.expression().getOrElse { return Result.failure(it) })
        }
        return Result.success(Node.StatementNode(statements.toTypedArray()))
    }

    private fun ignoreNewLines() {
        while (this.currentToken.type == Token.NEWLINE) this.advance()
    }
}