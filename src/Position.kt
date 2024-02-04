import java.io.Serializable

class Position(var index: Int, var line: Int, var column: Int, val fileName: String, val fileText: String): Serializable {
    fun advance(currentChar: Char) {
        index++
        column++
        if (currentChar == '\n') {
            line++
            column = 0
        }
    }

    companion object {
        val unknown = Position(-1,-1,-1,"unknown", "unknown")
    }

    fun copy(): Position {
        return Position(index, line, column, fileName, fileText)
    }
}