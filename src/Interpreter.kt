import java.io.File
import java.io.ObjectInputStream
import java.util.*
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
        "LEN",
        "CLEAR",
        "CMD",
        "RUN"
    )

    fun visit(node: Node): RjType {
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

            is Node.BreakNode -> visitBreakNode(node)

            is Node.ContinueNode -> visitContinueNode(node)
        }
    }

    private fun visitNumberNode(node: Node.NumberNode): RjType {
        return if (node.token is Token.INT) RjInt(node.token.value?.toInt()!!)
        else RjFloat(node.token.value?.toDouble()!!)
    }

    private fun visitBinOpNode(node: Node.BinOpNode): RjType {
        val left = this.visit(node.leftNode)
        val right = this.visit(node.rightNode)

        if (left is Null || right is Null) {
            return when (node.operatorToken) {
                is Token.EE -> RjInt.bool(left == right)
                is Token.NE -> RjInt.bool(left != right)
                else -> Null
            }
        }

        return if (left is RjNumber && right is RjNumber) when (node.operatorToken) {
            is Token.PLUS -> left.plus(right)
            is Token.MINUS -> left.minus(right)
            is Token.MUL -> left.times(right)
            is Token.DIV -> left.div(right)
            is Token.POW -> left.pow(right)
            is Token.EE -> RjInt.bool(left == right)
            is Token.NE -> RjInt.bool(left != right)
            is Token.LESS -> left.less(right)
            is Token.GREATER -> left.greater(right)
            is Token.LESSEQUAL -> left.lessEquals(right)
            is Token.GREATEREQUAL -> left.greaterEquals(right)
            is Token.AND -> RjInt(left.and(right).value.toInt())
            is Token.OR -> RjInt(left.and(right).value.toInt())
            else -> fail(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on Number and Number", node.operatorToken.pos
                ), "Interpreting"
            )
        } else if (left is RjString && right is RjString) when (node.operatorToken) {
            is Token.EE -> RjInt.bool(left == right)
            is Token.NE -> RjInt.bool(left != right)
            is Token.PLUS -> left + right
            else -> fail(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and String", node.operatorToken.pos
                ), "Interpreting"
            )
        } else if (left is RjString && right is RjNumber) when (node.operatorToken) {
            is Token.MUL -> left * right
            is Token.PLUS -> left + right
            is Token.GET -> left[right]
            is Token.DIV -> left.rem(right)
            else -> fail(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and Number", node.operatorToken.pos
                ), "Interpreting"
            )
        } else if (left is RjNumber && right is RjString) when (node.operatorToken) {
            is Token.PLUS -> RjString(Node.StringNode(Token.STRING(left.value.toString() + right, Position.unknown)))
            else -> fail(
                TypeError(
                    "Cannot find operation '${node.operatorToken}' on String and Number", node.operatorToken.pos
                ), "Interpreting"
            )
        } else if (left is RjList) when (node.operatorToken) {
            is Token.EE -> RjInt.bool(left == right)
            is Token.NE -> RjInt.bool(left != right)
            is Token.PLUS -> left + right
            is Token.GET -> left[right]
            is Token.MINUS -> left - right
            is Token.DIV -> left.rem(right)
            else -> fail(
                TypeError(
                    "Operation ${node.operatorToken} is not applicable to List", Position.unknown
                ), "Interpreting"
            )
        } else {
            fail(
                TypeError(
                    "Operation ${node.operatorToken} is not applicable to $left and $right", Position.unknown
                ), "Interpreting"
            )
        }
    }

    private fun visitUnaryOpNode(node: Node.UnaryOpNode): RjType {
        val number = this.visit(node.node)
        if (number !is RjNumber) fail(TypeError("Expected Number,got $number", Position.unknown), "Interpreting")
        return when (node.operatorToken) {
            is Token.MINUS -> -number
            is Token.PLUS -> number
            is Token.NOT -> RjInt.bool(number.value == 0)

            else -> fail(
                UnknownNodeError("Found unknown Node $node", node.operatorToken.pos), "Interpreting"
            )
        }
    }

    private fun visitVarAccessNode(node: Node.VarAccessNode): RjType {
        return this.context.varTable.get(node)
    }

    private fun visitVarAssignNode(node: Node.VarAssignNode): RjType {
        val value = this.visit(node.value)
        this.context.varTable.set(node, value)
        return value
    }

    private fun visitIfThenNode(node: Node.IfNode): RjType {
        val bool = visit(node.bool)
        if (bool == RjInt.True) return visit(node.expr)
        if (node.elseExpr != null) return visit(node.elseExpr)
        return Null
    }

    private fun visitWhileNode(node: Node.WhileNode): RjType {
        var bool = visit(node.bool)
        while (bool == RjInt.True) {
            val res = visit(node.expr)
            bool = visit(node.bool)
            when (res) {
                is RjReturn, is RjBreak -> return res
                else -> {}
            }
        }
        return Null
    }

    private fun visitForNode(node: Node.ForNode): RjType {
        val from = visit(node.from)
        val to = visit(node.to)
        val step = if (node.step != null) visit(node.step) else RjInt(1)
        if (from !is RjNumber || to !is RjNumber || step !is RjNumber) fail(
            TypeError(
                "Expected Number ,got $from as from, $to as to and $step as step", Position.unknown
            ), "Interpreting"
        )
        if (step.greater(RjInt(0)) == RjInt.True) {
            for (i in from.value.toInt()..to.value.toInt() step step.value.toInt()) {
                val identifier = Node.VarAssignNode(node.identifier, Node.NumberNode(Token.INT(i, Position.unknown)))
                visit(identifier)
                when (val res = visit(node.expr)) {
                    is RjReturn, is RjBreak -> {
                        this.context.varTable.remove(Node.VarAccessNode(node.identifier))
                        return res
                    }

                    else -> {}
                }
            }
            this.context.varTable.remove(Node.VarAccessNode(node.identifier))
        }

        return Null
    }

    private fun visitFunDefNode(node: Node.FunDefNode): RjType {
        val func = RjFunction(node.identifier, node.argTokens, node.bodyNode)
        this.context.varTable.set(
            Node.VarAssignNode(node.identifier, Node.NumberNode(Token.INT(1, Position.unknown))), func
        )
        return func
    }

    private fun visitFunCallNode(node: Node.FunCallNode): RjType {
        if (node.identifier.value?.uppercase()!! in this.builtinFunctions) return this.executeBuiltin(node)
        val func = this.context.varTable.get(Node.VarAccessNode(node.identifier))
        if (func !is RjFunction) fail(
            InvalidSyntaxError(
                "$func is not a FUN. Try calling without parenthesis", node.identifier.pos
            ), "Interpreting"
        )
        return func.execute(node.args)
    }

    private fun visitStringNode(node: Node.StringNode): RjType {
        return RjString(node)
    }

    private fun visitListNode(node: Node.ListNode): RjType {
        val listContent = arrayListOf<RjType>()
        for (nd in node.content) {
            listContent.add(visit(nd))
        }
        return RjList(listContent.toTypedArray())
    }

    private fun visitStatementNode(node: Node.StatementNode): RjType {
        if (node.expressions.size == 1) return visit(node.expressions[0])
        for (nd in node.expressions) {
            when (val res = visit(nd)) {
                is RjReturn, is RjBreak -> return res
                is RjContinue -> return RjBreak()
                else -> {}
            }
        }
        return Null
    }

    private fun visitReturnNode(node: Node.ReturnNode): RjType {
        return if (node.toReturn == null) RjReturn(null)
        else RjReturn(visit(node.toReturn))
    }

    private fun visitContinueNode(node: Node.ContinueNode): RjType {
        return RjContinue()
    }

    private fun visitBreakNode(node: Node.BreakNode): RjType {
        return RjBreak()
    }


    private fun executeBuiltin(node: Node.FunCallNode): RjType {
        when (node.identifier.value?.uppercase()) {
            "PRINT" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0])
                if(value !is RjList)
                    print(value.value)
                else
                    print(value)
            }

            "PRINTLN" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0])
                if(value !is RjList)
                    println(value.value)
                else
                    println(value.value.joinToString(prefix = "[", postfix = "]"))
            }

            "READ" -> {
                checkArgSize(0, node)
                return RjInt(System.`in`.read())
            }

            "READLINE" -> {
                checkArgSize(0, node)
                return RjString(Node.StringNode(Token.STRING(Scanner(System.`in`).nextLine(), node.identifier.pos)))
            }

            "ERROR" -> {
                checkArgSize(1, node)
                val reason = (node.args[0] as Node.StringNode).token.value!!
                fail(RuntimeError(reason, node.identifier.pos), "Running")
            }

            "TYPE" -> {
                checkArgSize(1, node)
                val value = visit(node.args[0])
                val name = value.javaClass.name.removePrefix("Rj")
                return RjString(Node.StringNode(Token.STRING(name, node.identifier.pos)))
            }

            "STRING" -> {
                checkArgSize(1, node)
                val type = visit(node.args[0])
                val value = if (type is RjList) type.toString() else type.value.toString()
                return RjString(Node.StringNode(Token.STRING(value, node.identifier.pos)))
            }

            "NUMBER" -> {
                checkArgSize(1, node)
                return when (val type = visit(node.args[0])) {
                    is RjNumber -> type
                    is Null -> RjInt.False
                    is RjString -> if (type.value.contains('.')) RjFloat(type.value.toDouble())
                    else RjInt(type.value.toInt())
                    is RjFunction, is RjList -> fail(
                        RuntimeError(
                            "Cannot convert FUN or List ${node.args[0]} to Number!", node.identifier.pos
                        ), "Running"
                    )
                    else -> {
                        println("WTF? $type Interpreter Line 295, Report this message to my github @OffensiverHase")
                        exitProcess(69)
                    }
                }
            }

            "LEN" -> {
                checkArgSize(1, node)
                return when (val value = visit(node.args[0])) {
                    is RjString -> RjInt(value.value.length)
                    is RjList -> RjInt(value.value.size)
                    else -> fail(
                        RuntimeError(
                            "Can only get the length of Strings and Lists, tried $value", node.identifier.pos
                        ), "Running")
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
                val rjStrings = arrayListOf<RjString>()
                for (nd in node.args) {
                    val str = visit(nd)
                    rjStrings.add(RjString(Node.StringNode(Token.STRING(str.value.toString(), node.identifier.pos))))
                }
                val strings = arrayListOf<String>()
                for (str in rjStrings) strings.add(str.value)
                println(strings.joinToString())
                Runtime.getRuntime().exec(strings.toTypedArray())
            }

            "RUN" -> {
                checkArgSize(1, node)
                val value =
                    executeBuiltin(Node.FunCallNode(Token.IDENTIFIER("STRING", node.identifier.pos), node.args)).value!!
                if (value !is String) fail(
                    RuntimeError(
                        "The language broke!!! Value converted to String is not a String. Report this error to my github @OffensiverHase",
                        node.identifier.pos
                    ), "Running"
                )
                try {
                    val file = File(value)
                    if (value.endsWith(".rj")) {
                        val content = file.readText()
                        return run(value, content)
                    } else if (value.endsWith(".rahanji")) {
                        val nd = ObjectInputStream(file.inputStream()).readObject() as Node
                        return Interpreter(Context(this.context,"<main>",this.context.varTable,value)).visit(nd)
                    } else fail(
                        RuntimeError(
                            "The specified file doesn't have .rj or .rahanji file ending",
                            node.identifier.pos
                        ), "Running"
                    )
                } catch (e: Exception) {
                    fail(
                        RuntimeError(
                            "Failed to load script ${node.args[0]}. Reason: $e", node.identifier.pos
                        ), "Running"
                    )
                }
            }

            else -> println("No builtin fun called ${node.identifier.value}")
        }
        return Null
    }

    private fun checkArgSize(shouldHave: Int, node: Node.FunCallNode) {
        if (node.args.size > shouldHave) fail(
            InvalidSyntaxError(
                "Passed in to many args into FUN ${node.identifier}. Expected $shouldHave, got ${node.args.size}",
                node.identifier.pos
            ), "Interpreting"
        )
        if (node.args.size < shouldHave) fail(
            InvalidSyntaxError(
                "Passed in to little args into FUN ${node.identifier}. Expected $shouldHave, got ${node.args.size}",
                node.identifier.pos
            ), "Interpreting"
        )
    }

    private fun run(fileName: String, content: String): RjType {
        val lexer = Lexer(content, fileName)
        val tokens = lexer.makeTokens()
        val parser = Parser(tokens)
        val node = parser.parse()
        val interpreter = Interpreter(Context(this.context,"<main>",this.context.varTable,fileName))
        val res = interpreter.visit(node)
        return res
    }
}