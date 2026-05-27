package com.conkeep.util

object SensitiveLogMasker {
    private val bearerTokenRegex = Regex("""(?i)(Authorization:\s*Bearer\s+)([^\s,;\]]+)""")
    private val jsonSecretRegex =
        Regex(
            """(?i)("?(?:access[_-]?token|refresh[_-]?token|id[_-]?token|fcm[_-]?token|p_fcm_token|master[_-]?key|masterKey)"?\s*[:=]\s*"?)([^"\s,}]+)("?)""",
        )
    private val querySecretRegex =
        Regex("""(?i)((?:access[_-]?token|refresh[_-]?token|id[_-]?token|fcm[_-]?token|master[_-]?key)=)([^&\s]+)""")

    fun mask(message: String): String =
        message
            .replace(bearerTokenRegex) { matchResult ->
                matchResult.groupValues[1] + maskToken(matchResult.groupValues[2])
            }.replace(jsonSecretRegex) { matchResult ->
                matchResult.groupValues[1] + maskToken(matchResult.groupValues[2]) + matchResult.groupValues[3]
            }.replace(querySecretRegex) { matchResult ->
                matchResult.groupValues[1] + maskToken(matchResult.groupValues[2])
            }

    fun maskToken(token: String?): String {
        val normalized = token.orEmpty()
        if (normalized.isBlank()) return "***"

        return when {
            normalized.length <= 8 -> "***"
            normalized.length <= 16 -> "${normalized.take(2)}***${normalized.takeLast(2)}"
            else -> "${normalized.take(4)}...${normalized.takeLast(4)}"
        }
    }
}
