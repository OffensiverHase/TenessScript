import java.io.Serializable

sealed class Node : Serializable {
    class NumberNode(val token: Token) : Node() {
        override fun toString(): String {
            return token.toString()
        }
    }

    class BinOpNode(val leftNode: Node, val operatorToken: Token, val rightNode: Node) : Node() {
        override fun toString(): String {
            return "($leftNode, $operatorToken, $rightNode)"
        }
    }

    class UnaryOpNode(val operatorToken: Token, val node: Node) : Node() {
        override fun toString(): String {
            return "($operatorToken, $node)"
        }
    }

    class VarAccessNode(val name: Token) : Node() {
        override fun toString(): String {
            return "(${name})"
        }
    }

    class VarAssignNode(val name: Token, val value: Node) : Node() {
        override fun toString(): String {
            return "($name < $value)"
        }
    }

    class IfNode(val bool: Node, val expr: Node, val elseExpr: Node?) : Node() {
        override fun toString(): String {
            return if (elseExpr == null) "IF $bool THEN $expr"
            else "IF $bool THEN $expr ELSE $elseExpr"
        }
    }

    class WhileNode(val bool: Node, val expr: Node) : Node() {
        override fun toString(): String {
            return "WHILE $bool THEN $expr"
        }
    }

    class ForNode(val identifier: Token, val from: Node, val to: Node, val step: Node?, val expr: Node) : Node() {
        override fun toString(): String {
            return if (step == null) {
                "FOR $identifier < $from TO $to THEN $expr"
            } else {
                "FOR $identifier < $from TO $to STEP $step THEN $expr"
            }
        }
    }

    class FunCallNode(val identifier: Token, val args: Array<Node>) : Node() {
        override fun toString(): String {
            return "$identifier(${args.joinToString()})"
        }
    }

    class FunDefNode(val identifier: Token, val argTokens: Array<Token.IDENTIFIER>, val bodyNode: Node) : Node() {
        override fun toString(): String {
            return "FUN $identifier (${argTokens.joinToString()}) $bodyNode"
        }
    }

    class StringNode(val token: Token) : Node() {
        override fun toString(): String {
            return "#${token.value}#"
        }
    }

    class ListNode(val content: Array<Node>) : Node() {
        override fun toString(): String {
            return "[${content.joinToString()}]"
        }
    }

    class StatementNode(val expressions: Array<Node>) : Node() {
        override fun toString(): String {
            return expressions.joinToString("\n")
        }
    }

    class ReturnNode(val toReturn: Node?) : Node() {
        override fun toString(): String {
            return if (toReturn == null) "return"
            else "return $toReturn"
        }
    }

    class BreakNode : Node() {
        override fun toString(): String {
            return "break "
        }
    }

    class ContinueNode : Node() {
        override fun toString(): String {
            return "continue "
        }
    }

}