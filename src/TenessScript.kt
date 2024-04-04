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

    if (parserThread.isAlive)
        parserThread.join()
    else
        println("Parser was not alive when trying to join!")

    try {
        val stream = ObjectOutputStream(tscFile.outputStream())
        stream.writeObject(ast.getOrElse {
            fail(it as Error)
            exitProcess(1)
        })
        stream.close()
    } catch (e: Exception) {
        fail(IOError("Could not write to $fileName, $e", Position.unknown))
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
        fail(IOError("Could not read file $fileName, $e", Position.unknown))
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

    var ast: Result<Node> = Result.failure(RuntimeError("Parser didn't finish!", Position.unknown))
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
    println("Welcome to Teness REPL (Read Evaluate Print Loop). Pass in -help for help")
    val scanner = Scanner(System.`in`)
    val startContext = Context(null, "<main>", VarMap(null), "<stdin>")
    addDefaults(startContext.varTable)
    while (true) {
        Thread.sleep(10)
        print("Teness > ")
        val text = scanner.nextLine()
        if (text.isBlank()) continue
        if (text == "-stop") break
        if (text == "-help") {
            printHelp()
            continue
        }
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
            Token(Token.IDENTIFIER,"null", Position.unknown), Node.NumberNode(Token(Token.INT,0.toString(), Position.unknown))
        ), Null
    )
    varMap.set(
        Node.VarAssignNode(
            Token(Token.IDENTIFIER,"true", Position.unknown), Node.NumberNode(Token(Token.INT,1.toString(), Position.unknown))
        ), TssInt.True
    )
    varMap.set(
        Node.VarAssignNode(
            Token(Token.IDENTIFIER,"false", Position.unknown), Node.NumberNode(Token(Token.INT,0.toString(), Position.unknown))
        ), TssInt.False
    )

    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        fail(ExceptionError(throwable, Position.unknown))
        exitProcess(1)
    }
}

fun printHelp() {
    println("""
        The current Interpreter implementation of TenessScript.
        
        Executes the given src file or compiles it into tsc bytecode
        
        Options:
            <nothing>       enter the REPL 
            <file>.tss      compile the file to tsc bytecode
            <file>.tsc      execute the tsc bytecode
            -direct         flag for running the given tss file without compiling it
            -help           shows this help
            
        Exit Status:
            Returns success unless a error occurs
    """.trimIndent())
}
