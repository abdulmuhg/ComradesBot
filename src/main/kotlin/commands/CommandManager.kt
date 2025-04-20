package commands

import error.BotException
import error.CommandExecutionException
import error.DiscordApiException
import error.InvalidInputException
import error.PermissionException
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory

/**
 * Manages and dispatches commands.
 */
class CommandManager {
    private val logger = LoggerFactory.getLogger(CommandManager::class.java)
    private val commands = mutableMapOf<String, Command>()

    /**
     * Register a new command.
     */
    fun registerCommand(command: Command) {
        commands[command.name] = command
        logger.info("Registered command: ${command.name}")
    }

    /**
     * Get all registered commands.
     */
    fun getCommands(): Map<String, Command> = commands

    /**
     * Process a message and execute a command if it matches.
     */
    /**
     * Process a message and execute a command if it matches.
     */
    suspend fun handleMessage(prefix: String, event: MessageReceivedEvent) {
        val message = event.message.contentRaw

        // Ignore messages from bots or messages that don't start with the prefix
        if (event.author.isBot || !message.startsWith(prefix)) {
            return
        }

        // Parse the command and arguments
        val parts = message.substring(prefix.length).trim().split("\\s+".toRegex())
        if (parts.isEmpty() || parts[0].isEmpty()) {
            // Just the prefix was sent, ignore
            return
        }

        val commandName = parts[0].lowercase()
        val args = if (parts.size > 1) parts.subList(1, parts.size) else listOf()

        // Find and execute the command
        val command = commands[commandName]
        if (command != null) {
            logger.info("Executing command: $commandName, User: ${event.author.name}, Args: ${args.joinToString(" ")}")
            try {
                command.execute(args, event)
            } catch (e: InvalidInputException) {
                logger.info("Invalid input for command $commandName: ${e.message}")
                event.channel.sendMessage(e.getUserFriendlyMessage()).queue()
            } catch (e: PermissionException) {
                logger.info("Permission denied for user ${event.author.id} on command $commandName: ${e.message}")
                event.channel.sendMessage(e.getUserFriendlyMessage()).queue()
            } catch (e: CommandExecutionException) {
                logger.error("Error executing command $commandName", e)
                event.channel.sendMessage(e.getUserFriendlyMessage()).queue()
            } catch (e: DiscordApiException) {
                logger.error("Discord API error in command $commandName", e)
                event.channel.sendMessage(e.getUserFriendlyMessage()).queue()
            } catch (e: BotException) {
                logger.error("Bot exception in command $commandName", e)
                event.channel.sendMessage(e.getUserFriendlyMessage()).queue()
            } catch (e: Exception) {
                logger.error("Unexpected error executing command $commandName", e)
                val exception = CommandExecutionException("An unexpected error occurred", e, commandName)
                event.channel.sendMessage(exception.getUserFriendlyMessage()).queue()
            }
        }
    }
}