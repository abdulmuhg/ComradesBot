package error

sealed class BotException(message: String, cause: Throwable? = null) : Exception(message, cause)