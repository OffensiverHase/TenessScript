open class Error(
    private val name: String,
    internal val details: String,
    internal val posStart: Position,
): Throwable() {
    override fun toString(): String {
        return "$name: $details, File ${posStart.fileName}, line ${posStart.line + 1}, pos ${posStart.column}"
    }
}

class IllegalCharError(details: String, posStart: Position) : Error("Illegal Character", details, posStart)

class InvalidSyntaxError(details: String, posStart: Position) : Error("Invalid Syntax", details, posStart)

class UnknownNodeError(details: String, posStart: Position) : Error("Unknown Node", details, posStart)

class NoSuchVarError(details: String, posStart: Position) : Error("No such Variable", details, posStart)

class ExceptionError(exception: Throwable, posStart: Position) :
    Error("Compiler Exception", "Compiler threw Exception $exception!", posStart)

class TypeError(details: String, posStart: Position) : Error("Type Exception", details, posStart)

class RuntimeError(details: String, posStart: Position) : Error("Runtime Exception", details, posStart)

class IOError(details: String, posStart: Position) : Error("Input-Output Exception", details, posStart)