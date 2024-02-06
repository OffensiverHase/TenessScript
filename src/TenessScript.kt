import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess

fun compile(fileName: String) {
    val text = try {
        File(fileName).readText()
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown))
        exitProcess(1)
    }

    val tokenStream = LinkedBlockingQueue<Token>()
    val lexerThread = Thread(Lexer(text, fileName, tokenStream), "Lexer")
    lexerThread.start()

    var ast: Result<Node> = Result.failure(RuntimeError("Parser didn't finish!", Position.unknown))
    val parserThread = Thread({
        ast = Parser(tokenStream).parse()
    }, "Parser")
    parserThread.start()

    val tscFile = File(fileName.removeSuffix(".tss") + ".tsc")

    parserThread.join()

    try {
        val stream = ObjectOutputStream(tscFile.outputStream())
        stream.writeObject(ast.getOrElse {
            fail(it as Error)
            exitProcess(1)
        })
        stream.close()
    } catch (e: Exception) {
        fail(IOError("Could not write to $fileName", Position.unknown))
        exitProcess(1)
    }
}

fun run(fileName: String) {
    val node = try {
        val stream = ObjectInputStream(File(fileName).inputStream())
        val toReturn = stream.readObject() as Node
        stream.close()
        toReturn
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown))
        exitProcess(1)
    }

    val varMap = VarMap()
    addDefaults(varMap)

    val interpreter = Interpreter(Context(null, "\n\t\t<main>", varMap, fileName))
    Thread.currentThread().name = "Interpreter"

    val res = interpreter.visit(node)
    res.getOrElse {
        fail(it as Error)
        exitProcess(1)
    }
}

fun noCompile(fileName: String) {
    val startContext = Context(null, "\n\t\t<main>", VarMap(), fileName)

    val text = try {
        File(fileName).readText()
    } catch (e: Exception) {
        fail(IOError("Could not read file $fileName", Position.unknown))
        exitProcess(1)
    }

    val varMap = startContext.varTable
    addDefaults(varMap)

    val tokenStream = LinkedBlockingQueue<Token>()
    val lexerThread = Thread(Lexer(text, fileName, tokenStream), "Lexer")
    lexerThread.start()

    var ast: Result<Node> = Result.failure(RuntimeError("Parser didn' finish!", Position.unknown))
    val parserThread = Thread({
        ast = Parser(tokenStream).parse()
    }, "Parser")
    parserThread.start()

    val interpreter = Interpreter(startContext)
    Thread.currentThread().name = "Interpreter"


    parserThread.join()

    println()
    val result = interpreter.visit(ast.getOrElse {
        fail(it as Error)
        exitProcess(1)
    })

    result.getOrElse {
        fail(it as Error)
        exitProcess(1)
    }
}

fun shell() {
    val scanner = Scanner(System.`in`)
    val startContext = Context(null, "<main>", VarMap(null), "<stdin>")
    addDefaults(startContext.varTable)
    while (true) {
        print("Teness > ")
        val text = scanner.nextLine()
        if (text.isBlank()) continue
        if (text == "/stop") break
        val res: TssType = shellStart(text, startContext)
        if (!(res is Null || res is TssBreak || res is TssReturn || res is TssContinue)) {
            println(res)
        }
    }
}

fun shellStart(text: String, startContext: Context): TssType {
    val tokenStream = LinkedBlockingQueue<Token>()
    val lexerThread = Thread(Lexer(text, "<stdin>", tokenStream), "Lexer")
    lexerThread.start()

    var ast: Result<Node> = Result.failure(RuntimeError("Parser didn't finish", Position.unknown))
    val parserThread = Thread({
        ast = Parser(tokenStream).parse()
    }, "Parser")
    parserThread.start()

    val interpreter = Interpreter(startContext)
    Thread.currentThread().name = "Interpreter"

    parserThread.join()

    val result = interpreter.visit(ast.getOrElse {
        fail(it as Error)
        return Null
    })

    return result.getOrElse {
        fail(it as Error)
        Null
    }
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
        ), TssInt.True
    )
    varMap.set(
        Node.VarAssignNode(
            Token.IDENTIFIER("false", Position.unknown), Node.NumberNode(Token.INT(0, Position.unknown))
        ), TssInt.False
    )

    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        fail(ExceptionError(throwable, Position.unknown))
        exitProcess(1)
    }
}
