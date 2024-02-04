import kotlin.math.pow

sealed class RjType {
    abstract val value: Any?
    abstract override operator fun equals(other: Any?): Boolean
    override fun toString(): String {
        return "${this.javaClass.canonicalName}: ${this.value}"
    }
}

sealed class RjNumber : RjType() {
    abstract override val value: Number
    abstract operator fun plus(other: RjNumber): RjNumber
    abstract operator fun minus(other: RjNumber): RjNumber
    abstract operator fun times(other: RjNumber): RjNumber
    abstract operator fun div(other: RjNumber): RjNumber
    abstract operator fun unaryMinus(): RjNumber
    abstract fun pow(other: RjNumber): RjNumber
    abstract fun and(other: RjNumber): RjNumber
    abstract fun or(other: RjNumber): RjNumber
    abstract fun less(other: RjNumber): RjNumber
    abstract fun greater(other: RjNumber): RjNumber
    abstract fun lessEquals(other: RjNumber): RjNumber
    abstract fun greaterEquals(other: RjNumber): RjNumber
}

object Null : RjType() {
    override val value = null
    override fun equals(other: Any?): Boolean {
        return other is Null
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun toString(): String {
        return "null"
    }
}

class RjFloat(override val value: Double) : RjNumber() {
    override fun plus(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjFloat(this.value + otherVal)
    }

    override fun minus(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjFloat(this.value - otherVal)
    }

    override fun times(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjFloat(this.value * otherVal)
    }

    override fun div(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        if (otherVal == 0.0)
            fail(RuntimeError("Division by zero!: ${this.value} / $otherVal", Position.unknown), "Running")
        return RjFloat(this.value / otherVal)
    }

    override fun unaryMinus(): RjNumber {
        return RjFloat(-this.value)
    }

    override fun pow(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjFloat(this.value.pow(otherVal))
    }

    override fun less(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjInt.bool(this.value < otherVal)
    }

    override fun greater(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjInt.bool(this.value > otherVal)
    }

    override fun lessEquals(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjInt.bool(this.value <= otherVal)
    }

    override fun greaterEquals(other: RjNumber): RjNumber {
        val otherVal = other.value.toDouble()
        return RjInt.bool(this.value >= otherVal)
    }

    override fun and(other: RjNumber): RjNumber {
        val otherVal = other.value.toInt()
        return RjInt(this.value.toInt().and(otherVal))
    }

    override fun or(other: RjNumber): RjNumber {
        val otherVal = other.value.toInt()
        return RjInt(this.value.toInt().or(otherVal))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RjNumber) return false
        val otherVal = other.value.toDouble()
        return this.value == otherVal
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

}

class RjInt(override val value: Int) : RjNumber() {
    companion object {
        fun bool(value: Boolean) = RjInt(value.toInt())
        val True: RjInt
            get() {
                return bool(true)
            }

        val False: RjInt
            get() {
                return bool(false)
            }
    }

    override fun plus(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return RjInt(this.value + otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return RjFloat(this.value.toDouble() + otherVal)
        }
    }

    override fun minus(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return RjInt(this.value - otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return RjFloat(this.value.toDouble() - otherVal)
        }
    }

    override fun times(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return RjInt(this.value * otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return RjFloat(this.value.toDouble() * otherVal)
        }
    }

    override fun div(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            if (otherVal == 0)
                fail(RuntimeError("Division by zero!: ${this.value} / $otherVal", Position.unknown), "Running")
            return RjInt(this.value / otherVal)
        } else {
            val otherVal = other.value.toDouble()
            if (otherVal == 0.0)
                fail(RuntimeError("Division by zero!: ${this.value} / $otherVal", Position.unknown), "Running")
            return RjFloat(this.value.toDouble() / otherVal)
        }
    }

    override fun unaryMinus(): RjNumber {
        return RjInt(-this.value)
    }

    override fun pow(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return RjInt(this.value.toDouble().pow(otherVal.toDouble()).toInt())
        } else {
            val otherVal = other.value.toDouble()
            return RjFloat(this.value.toDouble() + otherVal)
        }
    }

    override fun less(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value < otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() < otherVal)
        }
    }

    override fun greater(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value > otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() > otherVal)
        }
    }

    override fun lessEquals(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value <= otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() <= otherVal)
        }
    }

    override fun greaterEquals(other: RjNumber): RjNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value >= otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() >= otherVal)
        }
    }

    override fun and(other: RjNumber): RjNumber {
        val otherVal = other.value.toInt()
        return RjInt(this.value.and(otherVal))
    }

    override fun or(other: RjNumber): RjNumber {
        val otherVal = other.value.toInt()
        return RjInt(this.value.or(otherVal))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RjNumber) return false
        val otherVal = other.value.toDouble()
        return this.value.toDouble() == otherVal
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}

class RjFunction(val identifier: Token, val args: Array<Token.IDENTIFIER>, val bodyNode: Node) : RjType() {
    override val value = "<${identifier.value}(${args.joinToString { it.value!! }})>"

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RjFunction) return false
        return this.identifier.value == other.identifier.value
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    fun execute(args: Array<Node>): RjType {
        val interpreter = Interpreter(Context(context, "fun\t<${identifier.value}>", VarMap(context.varTable), context.fileName))
        if (args.size < this.args.size) fail(
            InvalidSyntaxError(
                "Passed to few args into <$identifier()>. Expected ${this.args.size}, got ${args.size}",
                identifier.pos
            ), "Interpreting"
        )
        else if (args.size > this.args.size) fail(
            InvalidSyntaxError(
                "Passed to many args into <$identifier()>. Expected ${this.args.size}, got ${args.size}",
                identifier.pos
            ), "Interpreting"
        )

        for (i in args.indices) {
            val argName = this.args[i]
            val argValue = args[i]
            val setVarNode = Node.VarAssignNode(argName, argValue)
            interpreter.visit(setVarNode)
        }
        val res = interpreter.visit(this.bodyNode)
        return if (res is RjReturn) res.value ?: Null
        else res
    }
}

class RjString(node: Node.StringNode) : RjType() {
    override val value: String = node.token.value!!
    private val pos = node.token.pos
    override fun equals(other: Any?): Boolean {
        if (other !is RjString) return false
        return this.value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    operator fun plus(other: RjString): RjString {
        val str = this.value + other.value
        return RjString(Node.StringNode(Token.STRING(str, this.pos)))
    }

    operator fun plus (other: RjNumber): RjString{
        val str = this.value + other.value
        return RjString(Node.StringNode(Token.STRING(str, this.pos)))
    }

    operator fun times(times: RjNumber): RjString {
        var str = ""
        for (i in 0..times.value.toInt()) str += this.value
        return RjString(Node.StringNode(Token.STRING(str, this.pos)))
    }

    override fun toString(): String {
        return "'$value'"
    }

    operator fun get(index: RjNumber): RjType {
        return RjString(Node.StringNode(Token.STRING(this.value[index.value.toInt()].toString(), Position.unknown)))
    }

    fun rem(index: RjNumber): RjString {
        val new = this.value.toMutableList()
        new.removeAt(index.value.toInt())
        return RjString(Node.StringNode(Token.STRING(String(new.toTypedArray().toCharArray()), Position.unknown)))
    }

}

class RjList(array: Array<RjType>): RjType() {
    override val value: Array<RjType> = array

    override fun toString(): String {
        return "[${this.value.joinToString()}]"
    }
    override fun equals(other: Any?): Boolean {
        if (other !is RjList) return false
        return this.value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    operator fun plus(other: RjList): RjList {
        return RjList(this.value + other.value)
    }
    operator fun plus(other: RjType): RjList {
        if (other is RjList)
            return this + other
        val new = this.value.toMutableList()
        new.add(other)
        return RjList(new.toTypedArray())
    }
    operator fun get(index: RjType): RjType {
        if (index !is RjNumber) fail(InvalidSyntaxError("Can only index List with a Number, got $index",Position.unknown), "Interpreting")
        var idx = index.value.toInt()
        if (idx < 0) idx += this.value.size
        else if (idx > this.value.size) fail(NoSuchVarError("Index $idx is out of bounds for size ${this.value.size}",Position.unknown),"Interpreting")
        return this.value[idx]
    }
    operator fun minus(other: RjType): RjList {
        val new = this.value.toMutableList()
        new.remove(other)
        return RjList(new.toTypedArray())
    }

    operator fun rem(index: RjType): RjList {
        if (index !is RjNumber) fail(InvalidSyntaxError("Can only index List with a Number, got $index",Position.unknown), "Interpreting")
        var idx = index.value.toInt()
        if (idx < 0) idx += this.value.size
        else if (idx > this.value.size) fail(NoSuchVarError("Index $idx is out of bounds for size ${this.value.size}",Position.unknown),"Interpreting")
        val new = this.value.toMutableList()
        new.removeAt(idx)
        return RjList(new.toTypedArray())
    }
}

class RjReturn(override val value: RjType?) : RjType() {
    override fun equals(other: Any?): Boolean {
        return false
    }
    override fun hashCode(): Int {
        return value.hashCode()
    }
}
class RjBreak : RjType() {
    override val value = null
    override fun equals(other: Any?): Boolean {
        return false
    }
    override fun hashCode(): Int {
        return value.hashCode()
    }
}
class RjContinue: RjType() {
    override val value = null
    override fun equals(other: Any?): Boolean {
        return false
    }
    override fun hashCode(): Int {
        return value.hashCode()
    }
}