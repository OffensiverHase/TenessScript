class VarMap(private val parent: VarMap? = null) {
    private val vars: MutableMap<String, TssType> = mutableMapOf()

    fun get(access: Node.VarAccessNode): TssType {
        val value = vars[(access.name as Token.IDENTIFIER).value]
        return if (value == null && parent != null)
            parent.get(access)
        else value ?: fail(
            NoSuchVarError("No VAR or FUN called '${access.name.value}' defined", access.name.pos),
            "Interpreting"
        )
    }

    fun set(access: Node.VarAssignNode, value: TssType) {
        vars[(access.name as Token.IDENTIFIER).value!!] = value
    }

    fun remove(access: Node.VarAccessNode) {
        vars.remove((access.name as Token.IDENTIFIER).value)
    }
}