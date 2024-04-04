class VarMap(private val parent: VarMap? = null) {
    private val vars: MutableMap<String, TssType> = mutableMapOf()

    fun get(access: Node.VarAccessNode): Result<TssType> {
        val value = vars[(access.name).value]
        return if (value == null && parent != null) parent.get(access)
        else if (value == null) {
            Result.failure(
                NoSuchVarError("No VAR or FUN called '${access.name.value}' defined", access.name.pos)
            )
        } else Result.success(value)
    }

    fun set(access: Node.VarAssignNode, value: TssType) {
        vars[(access.name).value!!] = value
    }

    fun remove(access: Node.VarAccessNode) {
        vars.remove((access.name).value)
    }
}