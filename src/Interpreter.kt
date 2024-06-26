import java.io.File
import java.io.ObjectInputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess
import context as methodContext

class Interpreter(private var context: Context) {
    private val builtinFunctions = arrayOf(
        "PRINT",
        "PRINTLN",
        "READ",
        "READLINE",
        "ERROR",
        "TYPE",
        "STRING",
        "NUMBER",
        "CHAR",
        "LEN",
        "CLEAR",
        "CMD",
        "RUN",
        "RANDOM",
        "CONTEXT"
    )

    fun visit(node: Node): Result<TssType> {
        methodContext = this.context
        return when (node) {
            is Node.StatementNode -> visitStatementNode(node)

            is Node.NumberNode -> visitNumberNode(node)

            is Node.BinOpNode -> visitBinOpNode(node)

            is Node.UnaryOpNode -> visitUnaryOpNode(node)

            is Node.VarAccessNode -> visitVarAccessNode(node)

            is Node.VarAssignNode -> visitVarAssignNode(node)

            is Node.IfNode -> visitIfThenNode(node)

            is Node.WhileNode -> visitWhileNode(node)

            is Node.ForNode -> visitForNode(node)

            is Node.FunDefNode -> visitFunDefNode(node)

            is Node.FunCallNode -> visitFunCallNode(node)

            is Node.StringNode -> visitStringNode(node)

            is Node.ListNode -> visitListNode(node)

            is Node.ReturnNode -> visitReturnNode(node)

            is Node.ListAssignNode -> visitListAssignNode(node)

            is Node.ObjectNode -> visitObjectNode(node)

            is Node.ObjectAssignNode -> visitObjectAssignNode(node)

            is Node.ObjectReadNode -> visitObjectReadNode(node)

            is Node.BreakNode -> visitBreakNode()

            is Node.ContinueNode -> visitContinueNode()
        }
    }

    private fun visitNumberNode(node: Node.NumberNode): Result<TssType> {
        return if (node.token.type == Token.INT) Result.success(TssInt(node.token.value?.toInt()!!))
        else Result.success(TssFloat(node.token.value?.toDouble()!!))
    }

    private fun visitBinOpNode(node: Node.BinOpNode): Result<TssType> {
        val left = this.visit(node.leftNode).getOrElse { return Result.failure(it) }
        val right = this.visit(node.rightNode).getOrElse { return Result.failure(it) }

        if (left is Null || right is Null) {
            return when (node.operatorToken.type) {
                Token.EQUALS -> Result.success(TssBool.bool(left == right))
                Token.UNEQUALS -> Result.success(TssBool.bool(left != right))
                else -> Result.success(Null)
            }
        }

        val res = if (left is TssNumber && right is TssNumber) when (node.operatorToken.type) {
            Token.PLUS -> left.plus(right)
            Token.MINUS -> left.minus(right)
            Token.MUL -> left.times(right)
            Token.DIV -> left.div(right)
            Token.POW -> left.pow(right)
            Token.EQUALS -> TssBool.bool(left == right)
            Token.UNEQUALS -> TssBool.bool(left != right)
            Token.LESS -> left.less(right)
            Token.GREATER -> left.greater(right)
            Token.LESSEQUAL -> left.lessEquals(right)
            Token.GREATEREQUAL -> left.greaterEquals(right)
            Token.AND -> TssInt(left.and(right).value.toInt())
            Token.OR -> TssInt(left.or(right).value.toInt())
            Token.XOR -> TssInt(left.xor(right).value.toInt())
            else -> return Result.failure(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on Number and Number", node.operatorToken.pos
                )
            )
        } else if (left is TssString && right is TssString) when (node.operatorToken.type) {
            Token.EQUALS -> TssBool.bool(left == right)
            Token.UNEQUALS -> TssBool.bool(left != right)
            Token.PLUS -> left + right
            else -> return Result.failure(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and String", node.operatorToken.pos
                )
            )
        } else if (left is TssString && right is TssNumber) when (node.operatorToken.type) {
            Token.MUL -> left * right
            Token.PLUS -> left + right
            Token.GET -> left[right]
            Token.DIV -> left.rem(right)
            else -> return Result.failure(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and Number", node.operatorToken.pos
                )
            )
        } else if (left is TssNumber && right is TssString) when (node.operatorToken.type) {
            Token.PLUS -> TssString(
                Node.StringNode(
                    Token(
                        Token.STRING, left.value.toString() + right, node.operatorToken.pos
                    )
                )
            )

            else -> return Result.failure(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and Number", node.operatorToken.pos
                )
            )

        } else if (left is TssBool && right is TssBool) when (node.operatorToken.type) {
            Token.AND -> left.and(right)
            Token.OR -> left.or(right)
            Token.XOR -> left.xor(right)
            else -> return Result.failure(TypeError("Operation ${node.operatorToken} is not applicable to List", node.operatorToken.pos))
        } else if (left is TssList) when (node.operatorToken.type) {
            Token.EQUALS -> TssBool.bool(left == right)
            Token.UNEQUALS -> TssBool.bool(left != right)
            Token.PLUS -> left + right
            Token.GET -> left[right]
            Token.MINUS -> left - right
            Token.DIV -> left.rem(right)
            else -> return Result.failure(
                TypeError(
                    "Operation ${node.operatorToken} is not applicable to List", node.operatorToken.pos
                )
            )
        } else {
            return Result.failure(
                TypeError(
                    "Operation ${node.operatorToken} is not applicable to $left and $right", node.operatorToken.pos
                )
            )
        }
        return Result.success(res)
    }

    private fun visitUnaryOpNode(node: Node.UnaryOpNode): Result<TssType> {
        val number = this.visit(node.node).getOrElse { return Result.failure(it) }
        if (number !is TssNumber && number !is TssBool) return Result.failure(
            TypeError(
                "Expected Number or Bool,got $number",
                node.operatorToken.pos
            )
        )
        if (number is TssNumber) {
            return Result.success(
                when (node.operatorToken.type) {
                    Token.MINUS -> -number
                    Token.PLUS -> number
                    Token.NOT -> number.xor(TssInt(0b11111111))

                    else -> return Result.failure(
                        UnknownNodeError("Found unknown Node $node", node.operatorToken.pos)
                    )
                }
            )
        } else {
            return Result.success(
                when (node.operatorToken.type) {
                    Token.NOT -> (number as TssBool).not()
                    else -> return Result.failure(
                        UnknownNodeError(
                            "Cannot perform ${node.operatorToken} on Bool, got $node",
                            node.operatorToken.pos
                        )
                    )
                }
            )
        }

    }

    private fun visitVarAccessNode(node: Node.VarAccessNode): Result<TssType> {
        return Result.success(this.context.varTable.get(node).getOrElse { return Result.failure(it) })
    }

    private fun visitVarAssignNode(node: Node.VarAssignNode): Result<TssType> {
        val value = this.visit(node.value)
        this.context.varTable.set(node, value.getOrElse { return Result.failure(it) })
        return value
    }

    private fun visitIfThenNode(node: Node.IfNode): Result<TssType> {
        val bool = visit(node.bool).getOrElse { return Result.failure(it) }
        if (bool == TssBool.True) return visit(node.expr)
        if (node.elseExpr != null) return visit(node.elseExpr)
        return Result.success(Null)
    }

    private fun visitWhileNode(node: Node.WhileNode): Result<TssType> {
        var bool = visit(node.bool).getOrElse { return Result.failure(it) }
        while (bool == TssBool.True) {
            val res = visit(node.expr).getOrElse { return Result.failure(it) }
            bool = visit(node.bool).getOrElse { return Result.failure(it) }
            when (res) {
                is TssReturn, is TssBreak -> return Result.success(res)
                else -> {}
            }
        }
        return Result.success(Null)
    }

    private fun visitForNode(node: Node.ForNode): Result<TssType> {
        val from = visit(node.from).getOrElse { return Result.failure(it) }
        val to = visit(node.to).getOrElse { return Result.failure(it) }
        val step = if (node.step != null) visit(node.step).getOrElse { return Result.failure(it) } else TssInt(1)
        if (from !is TssNumber || to !is TssNumber || step !is TssNumber) return Result.failure(
            TypeError(
                "Expected Number ,got $from as from, $to as to and $step as step", node.identifier.pos
            )
        )
        if (step.greater(TssInt(0)) == TssBool.True) {
            for (i in from.value.toInt()..to.value.toInt() step step.value.toInt()) {
                val identifier = Node.VarAssignNode(
                    node.identifier, Node.NumberNode(Token(Token.INT, i.toString(), node.identifier.pos))
                )
                visit(identifier).getOrElse { return Result.failure(it) }
                when (val res = visit(node.expr).getOrElse { return Result.failure(it) }) {
                    is TssReturn, is TssBreak -> {
                        this.context.varTable.remove(Node.VarAccessNode(node.identifier))
                        return Result.success(res)
                    }

                    else -> {}
                }
            }
            this.context.varTable.remove(Node.VarAccessNode(node.identifier))
        }

        return Result.success(Null)
    }

    private fun visitFunDefNode(node: Node.FunDefNode): Result<TssType> {
        val func = TssFunction(node.identifier, node.argTokens, node.bodyNode)
        this.context.varTable.set(
            Node.VarAssignNode(node.identifier, Node.NumberNode(Token(Token.INT, 1.toString(), node.identifier.pos))), func
        )
        return Result.success(func)
    }

    private fun visitFunCallNode(node: Node.FunCallNode): Result<TssType> {
        if (node.identifier.value?.uppercase()!! in this.builtinFunctions) return this.executeBuiltin(node)
        val func =
            this.context.varTable.get(Node.VarAccessNode(node.identifier)).getOrElse { return Result.failure(it) }
        if (func !is TssFunction) return Result.failure(
            TypeError(
                "$func is not a function. Try calling without parenthesis", node.identifier.pos
            )
        )
        return func.execute(node.args)
    }

    private fun visitStringNode(node: Node.StringNode): Result<TssType> {
        return Result.success(TssString(node))
    }

    private fun visitListNode(node: Node.ListNode): Result<TssType> {
        val listContent = arrayListOf<TssType>()
        for (nd in node.content) {
            listContent.add(visit(nd).getOrElse { return Result.failure(it) })
        }
        return Result.success(TssList(listContent.toTypedArray()))
    }

    private fun visitStatementNode(node: Node.StatementNode): Result<TssType> {
        if (node.expressions.size == 1) return visit(node.expressions[0])
        for (nd in node.expressions) {
            when (val res = visit(nd).getOrElse { return Result.failure(it) }) {
                is TssReturn, is TssBreak -> return Result.success(res)
                is TssContinue -> return Result.success(TssBreak())
                else -> {}
            }
        }
        return Result.success(Null)
    }


    private fun visitListAssignNode(node: Node.ListAssignNode): Result<TssType> {
        val list = visit(node.listNode).getOrElse { return Result.failure(it) } as TssList
        val index = visit(node.index).getOrElse { return Result.failure(it) }
        val value = visit(node.value).getOrElse { return Result.failure(it) }
        return Result.success(list.set(index, value))
    }

    private fun visitObjectNode(node: Node.ObjectNode): Result<TssType> {
        val newMap = mutableMapOf<String, TssType>()
        node.map.forEach { (key, value) ->
            newMap[key.value.toString()] = visit(value).getOrElse { return Result.failure(it) }
        }
        return Result.success(TssObject(newMap))
    }

    private fun visitObjectReadNode(node: Node.ObjectReadNode): Result<TssType> {
        val obj = visit(node.obj).getOrElse { return Result.failure(it) } as TssObject
        return Result.success(obj.get(node.key))
    }

    private fun visitObjectAssignNode(node: Node.ObjectAssignNode): Result<TssType> {
        val obj = visit(node.obj).getOrElse { return Result.failure(it) } as TssObject
        val value = visit(node.value).getOrElse { return Result.failure(it) }
        return Result.success(obj.set(node.key, value))
    }

    private fun visitReturnNode(node: Node.ReturnNode): Result<TssType> {
        return if (node.toReturn == null) Result.success(TssReturn(null))
        else Result.success(TssReturn(visit(node.toReturn).getOrElse { return Result.failure(it) }))
    }

    private fun visitContinueNode(): Result<TssType> {
        return Result.success(TssContinue())
    }

    private fun visitBreakNode(): Result<TssType> {
        return Result.success(TssBreak())
    }


    private fun executeBuiltin(node: Node.FunCallNode): Result<TssType> {
        when (node.identifier.value?.uppercase()) {
            "PRINT" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0]).getOrElse { return Result.failure(it) }
                if (value !is TssList) print(value.value)
                else print(value)
            }

            "PRINTLN" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0]).getOrElse { return Result.failure(it) }
                if (value !is TssList && value !is TssObject) println(value.value)
                else if (value is TssObject) println(value.toString())
                else if (value is TssList) println(value.value.joinToString(prefix = "[", postfix = "]"))
            }

            "READ" -> {
                checkArgSize(0, node)
                return Result.success(TssInt(System.`in`.read()))
            }

            "READLINE" -> {
                checkArgSize(0, node)
                return Result.success(
                    TssString(
                        Node.StringNode(
                            Token(
                                Token.STRING, Scanner(System.`in`).nextLine(), node.identifier.pos
                            )
                        )
                    )
                )
            }

            "ERROR" -> {
                checkArgSize(1, node)
                val reason = (node.args[0] as Node.StringNode).token.value!!
                return Result.failure(RuntimeError(reason, node.identifier.pos))
            }

            "TYPE" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0]).getOrElse { return Result.failure(it) }
                val name = value.javaClass.name.removePrefix("Tss")
                return Result.success(TssString(Node.StringNode(Token(Token.STRING, name, node.identifier.pos))))
            }

            "STRING" -> {
                checkArgSize(1, node)
                val type = visit(node.args[0]).getOrElse { return Result.failure(it) }
                val value = if (type is TssList) type.toString() else type.value.toString()
                return Result.success(TssString(Node.StringNode(Token(Token.STRING, value, node.identifier.pos))))
            }

            "NUMBER" -> {
                checkArgSize(1, node)
                return when (val type = visit(node.args[0]).getOrElse { return Result.failure(it) }) {
                    is TssNumber -> Result.success(type)
                    is Null -> Result.success(TssBool.False)
                    is TssString -> if (type.value.contains('.')) Result.success(TssFloat(type.value.toDouble()))
                    else Result.success(TssInt(type.value.toInt()))

                    is TssFunction, is TssList -> return Result.failure(
                        RuntimeError(
                            "Cannot convert FUN or List ${node.args[0]} to Number!", node.identifier.pos
                        )
                    )

                    else -> {
                        println("WTF? $type Interpreter Line 295, Report this message to my github @OffensiverHase")
                        exitProcess(69)
                    }
                }
            }

            "CHAR" -> {
                checkArgSize(1, node)
                val value = executeBuiltin(
                    Node.FunCallNode(
                        Token(Token.IDENTIFIER, "NUMBER", node.identifier.pos), node.args
                    )
                ).getOrElse { return Result.failure(it) }.value!!
                if (value !is Number) return Result.failure(
                    RuntimeError(
                        "WTF??? Value converted to Number is not a Number. Report this error to my github @OffensiverHase",
                        node.identifier.pos
                    )
                )
                val char = Char(value.toInt())
                return Result.success(
                    TssString(
                        Node.StringNode(
                            Token(
                                Token.STRING,
                                char.toString(),
                                node.identifier.pos
                            )
                        )
                    )
                )
            }

            "LEN" -> {
                checkArgSize(1, node)
                return when (val value = visit(node.args[0]).getOrElse { return Result.failure(it) }) {
                    is TssString -> Result.success(TssInt(value.value.length))
                    is TssList -> Result.success(TssInt(value.value.size))
                    else -> return Result.failure(
                        RuntimeError(
                            "Can only get the length of Strings and Lists, tried $value", node.identifier.pos
                        )
                    )
                }
            }

            "CLEAR" -> {
                checkArgSize(0, node)
                if (System.getProperty("os.name").contains("Win") || System.getProperty("os.name")
                        .contains("nt")
                ) Runtime.getRuntime().exec(arrayOf("cls"))
                else Runtime.getRuntime().exec(arrayOf("clear"))
            }

            "CMD" -> {
                val tssStrings = arrayListOf<TssString>()
                for (nd in node.args) {
                    val str = visit(nd).getOrElse { return Result.failure(it) }
                    tssStrings.add(
                        TssString(
                            Node.StringNode(
                                Token(
                                    Token.STRING,
                                    str.value.toString(),
                                    node.identifier.pos
                                )
                            )
                        )
                    )
                }
                val strings = arrayListOf<String>()
                for (str in tssStrings) strings.add(str.value)
                println(strings.joinToString())
                Runtime.getRuntime().exec(strings.toTypedArray())
            }

            "RANDOM" -> {
                checkArgSize(0, node)
                val value = Math.random()
                return Result.success(TssFloat(value))
            }

            "CONTEXT" -> {
                checkArgSize(0, node)
                val value = this.context.name
                return Result.success(TssString(Node.StringNode(Token(Token.STRING, value, node.identifier.pos))))
            }

            "RUN" -> {
                checkArgSize(1, node)
                val value = executeBuiltin(
                    Node.FunCallNode(
                        Token(Token.IDENTIFIER, "STRING", node.identifier.pos), node.args
                    )
                ).getOrElse { return Result.failure(it) }.value!!
                if (value !is String) return Result.failure(
                    RuntimeError(
                        "The language broke!!! Value converted to String is not a String. Report this error to my github @OffensiverHase",
                        node.identifier.pos
                    )
                )
                try {
                    val file = File(value)
                    if (value.endsWith(".tss")) {
                        val content = file.readText()
                        return run(value, content)
                    } else if (value.endsWith(".tsc")) {
                        val nd = ObjectInputStream(file.inputStream()).readObject() as Node
                        return Interpreter(Context(this.context, "<main>", this.context.varTable, value)).visit(nd)
                    } else return Result.failure(
                        RuntimeError(
                            "The specified file doesn't have .ts or .tsc file ending", node.identifier.pos
                        )
                    )
                } catch (e: Exception) {
                    return Result.failure(
                        RuntimeError(
                            "Failed to load script ${node.args[0]}. Reason: $e", node.identifier.pos
                        )
                    )
                }
            }

            else -> println("No builtin fun called ${node.identifier.value}")
        }
        return Result.success(Null)
    }

    private fun checkArgSize(shouldHave: Int, node: Node.FunCallNode): Result<Unit> {
        return if (node.args.size > shouldHave) Result.failure(
            InvalidSyntaxError(
                "Passed in to many args into FUN ${node.identifier}. Expected $shouldHave, got ${node.args.size}",
                node.identifier.pos
            )
        )
        else if (node.args.size < shouldHave) Result.failure(
            InvalidSyntaxError(
                "Passed in to little args into FUN ${node.identifier}. Expected $shouldHave, got ${node.args.size}",
                node.identifier.pos
            )
        )
        else Result.success(run {})
    }
    //run('out/artifacts/TenessScript_jar/src/main.tss')

    private fun run(fileName: String, content: String): Result<TssType> {
        val tokenStream = LinkedBlockingQueue<Token>()
        val lexerThread = Thread(Lexer(content, fileName, tokenStream), "Lexer")
        lexerThread.start()

        var ast: Result<Node> = Result.failure(RuntimeError("Parser didn't finish!", Position.unknown))
        val parserThread = Thread({
            ast = Parser(tokenStream).parse()
        }, "Parser")
        parserThread.start()

        val interpreter = Interpreter(Context(this.context, "<main>", this.context.varTable, fileName))
        Thread.currentThread().name = "Interpreter"
        parserThread.join()

        val res = interpreter.visit(ast.getOrElse { return Result.failure(it) })
        return res
    }
}