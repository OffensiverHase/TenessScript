import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

fun compile(fileName: String) {
    val text = try {
        File(fileName).readText()
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown), "Starting")
    }

    val lexer = Lexer(text, fileName)
    val tokens = lexer.makeTokens()

    val parser = Parser(tokens)
    val ast = parser.parse()

    val rahanjiFile = File(fileName.removeSuffix(".rj") + ".rahanji")

    try {
        val stream = ObjectOutputStream(rahanjiFile.outputStream())
        stream.writeObject(ast)
        stream.close()
    } catch (e: Exception) {
        fail(IOError("Could not write to $fileName", Position.unknown), "Starting")
    }
}

fun run(fileName: String) {
    val node = try {
        val stream = ObjectInputStream(File(fileName).inputStream())
        val toReturn = stream.readObject() as Node
        stream.close()
        toReturn
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown), "Starting")
    }

    val varMap = VarMap()
    addDefaults(varMap)

    val interpreter = Interpreter(Context(null, "\n\t\t<main>", varMap, fileName))
    interpreter.visit(node)
}

fun noCompile(fileName: String): RjType {
    val startContext = Context(null, "\n\t\t<main>", VarMap(), fileName)

    val text = try {
        File(fileName).readText()
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown), "Starting")
    }

    val varMap = startContext.varTable
    addDefaults(varMap)

    val lexer = Lexer(text, fileName)
    val tokens = lexer.makeTokens()

    val parser = Parser(tokens)
    val ast = parser.parse()

    val interpreter = Interpreter(startContext)
    val result = interpreter.visit(ast)

    return (result)
}

fun shell() {
    val scanner = Scanner(System.`in`)
    val startContext = Context(null, "<main>", VarMap(null), "<stdin>")
    addDefaults(startContext.varTable)
    while (true) {
        print("rahanji > ")
        val text = scanner.nextLine()
        if (text.isBlank()) continue
        if (text == "/stop") break
        val res: RjType = shellStart(text, startContext)
        if (!(res is Null || res is RjBreak || res is RjReturn || res is RjContinue)) {
            println(res)
        }
    }
}

fun shellStart(text: String, startContext: Context): RjType {
    val varMap = startContext.varTable
    addDefaults(varMap)

    val lexer = Lexer(text, "<stdin>")
    val tokens = lexer.makeTokens()

    val parser = Parser(tokens)
    val ast = parser.parse()

    val interpreter = Interpreter(startContext)
    val result = interpreter.visit(ast)

    return (result)
}


fun addDefaults(varMap: VarMap) {
    varMap.set(
        Node.VarAssignNode(
            Token.IDENTIFIER("null", Position.unknown), Node.NumberNode(Token.INT(0, Position.unknown))
        ), Null
    )
    varMap.set(
        Node.VarAssignNode(
            Token.IDENTIFIER("true", Position.unknown), Node.NumberNode(Token.INT(1, Position.unknown))
        ), RjInt.True
    )
    varMap.set(
        Node.VarAssignNode(
            Token.IDENTIFIER("false", Position.unknown), Node.NumberNode(Token.INT(0, Position.unknown))
        ), RjInt.False
    )

    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        fail(ExceptionError(throwable, Position.unknown), "Unknown")
    }
}
