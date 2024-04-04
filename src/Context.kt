class Context(private val parent: Context? = null, val name: String, val varTable: VarMap, val fileName: String) {
    override fun toString(): String {
        return if (parent == null) this.name
        else """
        ${this.name}${"\t\t:"}$fileName$parent"""
    }
}