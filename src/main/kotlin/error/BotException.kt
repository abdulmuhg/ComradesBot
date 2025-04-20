package error

/**
 * Base exception class for all bot-related exceptions.
 * Provides a common type for catching exceptions from the bot.
 */
sealed class BotException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    /**
     * Creates a user-friendly error message that can be shown to Discord users.
     */
    open fun getUserFriendlyMessage(): String = message ?: "An error occurred"
}

/**
 * Exception thrown when there's an issue with the bot's configuration.
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : BotException(message, cause) {
    override fun getUserFriendlyMessage(): String =
        "There was a configuration problem with the bot. Please contact the bot administrator."
}

/**
 * Exception thrown when there's an issue executing a command.
 */
class CommandExecutionException(
    message: String,
    cause: Throwable? = null,
    val commandName: String? = null
) : BotException(message, cause) {
    override fun getUserFriendlyMessage(): String =
        if (commandName != null) {
            "Error executing command '$commandName': $message"
        } else {
            "Error executing command: $message"
        }
}

/**
 * Exception thrown when a user doesn't have permission to use a command.
 */
class PermissionException(
    message: String,
    val userId: String,
    val requiredPermission: String
) : BotException(message) {
    override fun getUserFriendlyMessage(): String =
        "You don't have permission to use this command. Required permission: $requiredPermission"
}

/**
 * Exception thrown when there's a problem with Discord API communication.
 */
class DiscordApiException(
    message: String,
    cause: Throwable? = null
) : BotException(message, cause) {
    override fun getUserFriendlyMessage(): String =
        "There was a problem communicating with Discord. Please try again later."
}

/**
 * Exception thrown when user input is invalid.
 */
class InvalidInputException(
    message: String,
    val paramName: String? = null
) : BotException(message) {
    override fun getUserFriendlyMessage(): String =
        if (paramName != null) {
            "Invalid input for '$paramName': $message"
        } else {
            "Invalid input: $message"
        }
}