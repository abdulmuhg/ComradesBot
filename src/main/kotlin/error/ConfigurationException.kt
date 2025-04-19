package error

class ConfigurationException(message: String, cause: Throwable? = null) : BotException(message, cause)