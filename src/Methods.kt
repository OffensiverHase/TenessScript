import kotlin.experimental.xor
import kotlin.system.exitProcess

fun fail(error: Error, stage: String): Nothing {
    System.err.print("""
Encountered error in stage $stage:
    $error
    Context: ${if (::context.isInitialized) context.toString() else "<main>"}
""")
    showArrowString(error.posStart)
    val name = error.details
    var code: Byte = 0
    for (byte in name.toByteArray())
        code = code.xor(byte)
    exitProcess(code.toInt())
}

fun Boolean.toInt() = if (this) 1 else 0

fun showArrowString(pos: Position) {
    try {
        val line = pos.fileText.lines()[pos.line]
        var arrow = ""
        for (i in 1 .. pos.column) arrow += " "
        arrow += "^"
        System.err.flush()
        System.err.println("${pos.line}\t$line")
        System.err.println("\t$arrow")
    } catch (_: Exception) {}
}

lateinit var context: Context