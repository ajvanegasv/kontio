package dev.ajvanegasv.kontio.domain.model

data class StatementFile(
    val name: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    val isPdf: Boolean
        get() = mimeType.contains("pdf", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true)

    val isCsv: Boolean
        get() = mimeType.contains("csv", ignoreCase = true) ||
                mimeType.contains("comma-separated", ignoreCase = true) ||
                name.endsWith(".csv", ignoreCase = true) ||
                name.endsWith(".txt", ignoreCase = true)

    fun readAsText(): String = bytes.decodeToString()
}
