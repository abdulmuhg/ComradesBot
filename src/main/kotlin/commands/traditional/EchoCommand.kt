package commands.traditional

import commands.Command
import error.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory


/**
 * A simple echo command that repeats what the user says.
 */
class EchoCommand : Command {
    private val logger = LoggerFactory.getLogger(EchoCommand::class.java)

    override val name = "echo"
    override val description = "Repeats what you say"
    override val usage = "echo [message]"

    override suspend fun execute(args: List<String>, event: MessageReceivedEvent) {
        try {
            if (args.isEmpty()) {
                throw InvalidInputException("You need to provide a message to echo", "message")
            }

            val message = args.joinToString(" ")

            // Check for potentially unsafe content
            if (message.contains("@everyone") || message.contains("@here")) {
                event.member?.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)?.let {
                    if (!(it)) {
                        throw PermissionException(
                            "You don't have permission to mention everyone/here",
                            event.author.id,
                            "MENTION_EVERYONE"
                        )
                    }
                }
            }

            event.channel.sendMessage(message).queue(
                { logger.debug("Echo command executed successfully") },
                { error ->
                    logger.error("Failed to send echo message", error)
                    throw DiscordApiException("Failed to send message", error)
                }
            )
        } catch (e: BotException) {
            // Let these propagate up to the command manager
            throw e
        } catch (e: Exception) {
            // Wrap unexpected exceptions
            throw CommandExecutionException("Error executing echo command", e, "echo")
        }
    }
}