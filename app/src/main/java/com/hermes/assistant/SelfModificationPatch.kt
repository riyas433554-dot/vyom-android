package com.hermes.assistant

/**
 * Applies bounded, exact-match edits. Ambiguous or stale anchors fail closed rather
 * than silently replacing multiple occurrences or overwriting a changed source file.
 */
internal object SelfModificationPatch {
    data class Operation(val find: String, val replace: String)

    private const val MAX_OPERATIONS = 20
    private const val MAX_ANCHOR_LENGTH = 12_000
    private const val MAX_REPLACEMENT_LENGTH = 12_000

    fun apply(source: String, operations: List<Operation>): String {
        require(source.isNotBlank()) { "Cannot patch an empty source file" }
        require(operations.isNotEmpty()) { "The model returned no patch operations" }
        require(operations.size <= MAX_OPERATIONS) { "Too many patch operations" }
        var result = source
        operations.forEachIndexed { index, operation ->
            require(operation.find.isNotBlank()) { "Patch $index has an empty find anchor" }
            require(operation.find.length <= MAX_ANCHOR_LENGTH) { "Patch $index find anchor is too large" }
            require(operation.replace.length <= MAX_REPLACEMENT_LENGTH) { "Patch $index replacement is too large" }
            val first = result.indexOf(operation.find)
            require(first >= 0) { "Patch $index anchor no longer exists; regenerate from the latest source" }
            require(result.indexOf(operation.find, first + operation.find.length) < 0) {
                "Patch $index anchor is ambiguous; it must match exactly once"
            }
            result = result.replaceRange(first, first + operation.find.length, operation.replace)
        }
        require(result != source) { "Patch did not change the source" }
        return result
    }
}
