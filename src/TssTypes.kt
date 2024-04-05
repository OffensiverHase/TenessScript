import TssBool.Companion.bool
import kotlin.math.pow
import kotlin.system.exitProcess

sealed class TssType {
    abstract val value: Any?
    abstract override operator fun equals(other: Any?): Boolean
    override fun toString(): String {
        return "${this.javaClass.canonicalName}: ${this.value}"
    }
}

sealed class TssNumber : TssType() {
    abstract override val value: Number
    abstract operator fun plus(other: TssNumber): TssNumber
    abstract operator fun minus(other: TssNumber): TssNumber
    abstract operator fun times(other: TssNumber): TssNumber
    abstract operator fun div(other: TssNumber): TssNumber
    abstract operator fun unaryMinus(): TssNumber
    abstract fun pow(other: TssNumber): TssNumber
    abstract fun and(other: TssNumber): TssNumber
    abstract fun or(other: TssNumber): TssNumber
    abstract fun xor(other: TssNumber): TssNumber
    abstract fun less(other: TssNumber): TssBool
    abstract fun greater(other: TssNumber): TssBool
    abstract fun lessEquals(other: TssNumber): TssBool
    abstract fun greaterEquals(other: TssNumber): TssBool
}

object Null : TssType() {
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

class TssFloat(override val value: Double) : TssNumber() {
    override fun plus(other: TssNumber): TssNumber {
        val otherVal = other.value.toDouble()
        return TssFloat(this.value + otherVal)
    }

    override fun minus(other: TssNumber): TssNumber {
        val otherVal = other.value.toDouble()
        return TssFloat(this.value - otherVal)
    }

    override fun times(other: TssNumber): TssNumber {
        val otherVal = other.value.toDouble()
        return TssFloat(this.value * otherVal)
    }

    override fun div(other: TssNumber): TssNumber {
        val otherVal = other.value.toDouble()
        if (otherVal == 0.0) {
            fail(RuntimeError("Division by zero!: ${this.value} / $otherVal", Position.unknown))
            return TssFloat(Double.NaN)
        }
        return TssFloat(this.value / otherVal)
    }

    override fun unaryMinus(): TssNumber {
        return TssFloat(-this.value)
    }

    override fun pow(other: TssNumber): TssNumber {
        val otherVal = other.value.toDouble()
        return TssFloat(this.value.pow(otherVal))
    }

    override fun less(other: TssNumber): TssBool {
        val otherVal = other.value.toDouble()
        return bool(this.value < otherVal)
    }

    override fun greater(other: TssNumber): TssBool {
        val otherVal = other.value.toDouble()
        return bool(this.value > otherVal)
    }

    override fun lessEquals(other: TssNumber): TssBool {
        val otherVal = other.value.toDouble()
        return bool(this.value <= otherVal)
    }

    override fun greaterEquals(other: TssNumber): TssBool {
        val otherVal = other.value.toDouble()
        return bool(this.value >= otherVal)
    }

    override fun and(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.toInt().and(otherVal))
    }

    override fun or(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.toInt().or(otherVal))
    }

    override fun xor(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.toInt().xor(otherVal))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TssNumber) return false
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

class TssBool(override val value: Boolean) : TssType() {
    override fun equals(other: Any?): Boolean {
        if (other !is TssBool) return false
        return this.value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    fun and(other: TssBool) : TssBool {
        return bool(this.value && other.value)
    }
    fun or(other: TssBool) : TssBool {
        return bool(this.value || other.value)
    }
    fun xor(other: TssBool) : TssBool {
        return bool(this.value.xor(other.value))
    }
    fun not() : TssBool {
        return bool(!this.value)
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        fun bool(value: Boolean) = TssBool(value)
        val True: TssBool
            get() {
                return bool(true)
            }

        val False: TssBool
            get() {
                return bool(false)
            }
    }
}

class TssInt(override val value: Int) : TssNumber() {

    override fun plus(other: TssNumber): TssNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return TssInt(this.value + otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return TssFloat(this.value.toDouble() + otherVal)
        }
    }

    override fun minus(other: TssNumber): TssNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return TssInt(this.value - otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return TssFloat(this.value.toDouble() - otherVal)
        }
    }

    override fun times(other: TssNumber): TssNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return TssInt(this.value * otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return TssFloat(this.value.toDouble() * otherVal)
        }
    }

    override fun div(other: TssNumber): TssNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            if (otherVal == 0) {
                fail(RuntimeError("Division by zero!: ${this.value} / 0", Position.unknown))
                return TssFloat(Double.NaN)
            }
            return TssInt(this.value / otherVal)
        } else {
            val otherVal = other.value.toDouble()
            if (otherVal == 0.0) {
                fail(RuntimeError("Division by zero!: ${this.value} / $otherVal", Position.unknown))
                return TssFloat(Double.NaN)
            }
            return TssFloat(this.value.toDouble() / otherVal)
        }
    }

    override fun unaryMinus(): TssNumber {
        return TssInt(-this.value)
    }

    override fun pow(other: TssNumber): TssNumber {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return TssInt(this.value.toDouble().pow(otherVal.toDouble()).toInt())
        } else {
            val otherVal = other.value.toDouble()
            return TssFloat(this.value.toDouble() + otherVal)
        }
    }

    override fun less(other: TssNumber): TssBool {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value < otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() < otherVal)
        }
    }

    override fun greater(other: TssNumber): TssBool {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value > otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() > otherVal)
        }
    }

    override fun lessEquals(other: TssNumber): TssBool {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value <= otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() <= otherVal)
        }
    }

    override fun greaterEquals(other: TssNumber): TssBool {
        if (other.value is Int) {
            val otherVal = other.value.toInt()
            return bool(this.value >= otherVal)
        } else {
            val otherVal = other.value.toDouble()
            return bool(this.value.toDouble() >= otherVal)
        }
    }

    override fun and(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.and(otherVal))
    }

    override fun or(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.or(otherVal))
    }

    override fun xor(other: TssNumber): TssNumber {
        val otherVal = other.value.toInt()
        return TssInt(this.value.xor(otherVal))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TssNumber) return false
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

class TssFunction(val identifier: Token, private val args: Array<Token>, private val bodyNode: Node) : TssType() {
    override val value = "<${identifier.value}(${args.joinToString { it.value!! }})>"

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TssFunction) return false
        return this.identifier.value == other.identifier.value
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    fun execute(args: Array<Node>): Result<TssType> {
        val interpreter =
            Interpreter(Context(context, "fun\t<${identifier.value}>", VarMap(context.varTable), context.fileName))
        if (args.size < this.args.size) return Result.failure(
            InvalidSyntaxError(
                "Passed to few args into <$identifier()>. Expected ${this.args.size}, got ${args.size}", identifier.pos
            )
        )
        else if (args.size > this.args.size) return Result.failure(
            InvalidSyntaxError(
                "Passed to many args into <$identifier()>. Expected ${this.args.size}, got ${args.size}", identifier.pos
            )
        )

        for (i in args.indices) {
            val argName = this.args[i]
            val argValue = args[i]
            val setVarNode = Node.VarAssignNode(argName, argValue)
            interpreter.visit(setVarNode)
        }
        val res = interpreter.visit(this.bodyNode).getOrElse { return Result.failure(it) }
        return if (res is TssReturn) Result.success(res.value ?: Null)
        else Result.success(res)
    }
}

class TssString(node: Node.StringNode) : TssType() {
    override val value: String = node.token.value!!
    private val pos = node.token.pos
    override fun equals(other: Any?): Boolean {
        if (other !is TssString) return false
        return this.value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    operator fun plus(other: TssString): TssString {
        val str = this.value + other.value
        return TssString(Node.StringNode(Token(Token.STRING, str, this.pos)))
    }

    operator fun plus(other: TssNumber): TssString {
        val str = this.value + other.value
        return TssString(Node.StringNode(Token(Token.STRING, str, this.pos)))
    }

    operator fun times(times: TssNumber): TssString {
        var str = ""
        for (i in 0..times.value.toInt()) str += this.value
        return TssString(Node.StringNode(Token(Token.STRING, str, this.pos)))
    }

    override fun toString(): String {
        return "'$value'"
    }

    operator fun get(index: TssNumber): TssType {
        return TssString(
            Node.StringNode(
                Token(
                    Token.STRING,
                    this.value[index.value.toInt()].toString(),
                    Position.unknown
                )
            )
        )
    }

    fun rem(index: TssNumber): TssString {
        val new = this.value.toMutableList()
        new.removeAt(index.value.toInt())
        return TssString(
            Node.StringNode(
                Token(
                    Token.STRING,
                    String(new.toTypedArray().toCharArray()),
                    Position.unknown
                )
            )
        )
    }

}

class TssList(array: Array<TssType>) : TssType() {
    override val value: Array<TssType> = array

    override fun toString(): String {
        return "[${this.value.joinToString()}]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TssList) return false
        return this.value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    operator fun plus(other: TssList): TssList {
        return TssList(this.value + other.value)
    }

    operator fun plus(other: TssType): TssList {
        if (other is TssList) return this + other
        val new = this.value.toMutableList()
        new.add(other)
        return TssList(new.toTypedArray())
    }

    operator fun get(index: TssType): TssType {
        if (index !is TssNumber) {
            fail(InvalidSyntaxError("Can only index List with a Number, got $index", Position.unknown))
            exitProcess(1)
        }
        var idx = index.value.toInt()
        if (idx < 0) idx += this.value.size
        else if (idx > this.value.size) {
            fail(
                NoSuchVarError(
                    "Index $idx is out of bounds for size ${this.value.size}", Position.unknown
                )
            )
            exitProcess(1)
        }
        return this.value[idx]
    }

    operator fun minus(other: TssType): TssList {
        val new = this.value.toMutableList()
        new.remove(other)
        return TssList(new.toTypedArray())
    }

    operator fun rem(index: TssType): TssList {
        if (index !is TssNumber) {
            fail(
                InvalidSyntaxError(
                    "Can only index List with a Number, got $index", Position.unknown
                )
            )
            exitProcess(1)
        }
        var idx = index.value.toInt()
        if (idx < 0) idx += this.value.size
        else if (idx > this.value.size) {
            fail(
                NoSuchVarError(
                    "Index $idx is out of bounds for size ${this.value.size}", Position.unknown
                )
            )
            exitProcess(1)
        }
        val new = this.value.toMutableList()
        new.removeAt(idx)
        return TssList(new.toTypedArray())
    }

    operator fun set(index: TssType, value: TssType): TssList {
        if (index !is TssNumber) {
            fail(
                InvalidSyntaxError(
                    "Can only index List with a Number, got $index", Position.unknown
                )
            )
            exitProcess(1)
        }
        var idx = index.value.toInt()
        if (idx < 0) idx += this.value.size
        else if (idx > this.value.size) {
            fail(
                NoSuchVarError(
                    "Index $idx is out of bounds for size ${this.value.size}", Position.unknown
                )
            )
            exitProcess(1)
        }
        val new = this.value.toMutableList()
        new[idx] = value
        return TssList(new.toTypedArray())
    }
}

class TssObject(override val value: Map<String, TssType>) : TssType() {
    override fun equals(other: Any?): Boolean {
        if (other !is TssObject) return false
        return this.value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        val sb = StringBuilder("object {\n")
        value.forEach { (key, value) ->
            sb.append("\t$key <- $value\n")
        }
        sb.append('}')
        return sb.toString()
    }

    fun get(key: Token): TssType {
        return this.value[key.value.toString()] ?: Null
    }

    fun set(key: Token, value: TssType): TssType {
        val new = this.value.toMutableMap()
        new[key.value.toString()] = value
        return TssObject(new)
    }

}

class TssReturn(override val value: TssType?) : TssType() {
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class TssBreak : TssType() {
    override val value = null
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class TssContinue : TssType() {
    override val value = null
    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}