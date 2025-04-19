package error

class CommandExecutionException(message: String, cause: Throwable? = null) : BotException(message, cause)