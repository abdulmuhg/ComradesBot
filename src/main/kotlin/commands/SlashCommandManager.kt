package commands

import error.BotException
import error.CommandExecutionException
import error.DiscordApiException
import error.InvalidInputException
import error.PermissionException
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory

/**
 * Manages and registers slash commands.
 */
class SlashCommandManager {
    private val logger = LoggerFactory.getLogger(SlashCommandManager::class.java)
    private val commands = mutableMapOf<String, SlashCommand>()
    
    /**
     * Register a new slash command.
     */
    fun registerCommand(command: SlashCommand) {
        commands[command.name] = command
        logger.info("Registered slash command: ${command.name}")
    }
    
    /**
     * Get all registered slash commands.
     */
    fun getCommands(): Map<String, SlashCommand> = commands
    
    /**
     * Get a command by its name.
     */
    fun getCommand(name: String): SlashCommand? = commands[name]
    
    /**
     * Register all commands with Discord.
     * 
     * @param jda The JDA instance
     * @param guildId Optional guild ID to register commands to a specific server (faster updates but limited to one server)
     */
    fun registerCommandsWithDiscord(jda: JDA, guildId: String? = null) {
        logger.info("Registering ${commands.size} slash commands with Discord...")
        
        val commandData = commands.values.map { it.toCommandData() }
        
        if (guildId != null) {
            // Register to a specific guild (server) - updates instantly but only works on that server
            val guild = jda.getGuildById(guildId)
            if (guild != null) {
                guild.updateCommands().addCommands(commandData).queue { 
                    logger.info("Successfully registered ${commandData.size} guild commands to ${guild.name}")
                }
            } else {
                logger.error("Could not find guild with ID: $guildId")
            }
        } else {
            // Register globally - takes up to an hour to update but works on all servers
            jda.updateCommands().addCommands(commandData).queue {
                logger.info("Successfully registered ${commandData.size} global commands")
            }
        }
    }
    
    /**
     * Handle a slash command interaction.
     * 
     * @param event The slash command interaction event
     */
    suspend fun handleSlashCommand(event: SlashCommandInteractionEvent) {
        val commandName = event.name
        val command = commands[commandName]

        if (command != null) {
            logger.info("Executing slash command: $commandName, User: ${event.user.name}")
            try {
                command.execute(event)
            } catch (e: InvalidInputException) {
                logger.info("Invalid input for slash command $commandName: ${e.message}")
                sendErrorResponse(event, e.getUserFriendlyMessage())
            } catch (e: PermissionException) {
                logger.info("Permission denied for user ${event.user.id} on slash command $commandName: ${e.message}")
                sendErrorResponse(event, e.getUserFriendlyMessage())
            } catch (e: CommandExecutionException) {
                logger.error("Error executing slash command $commandName", e)
                sendErrorResponse(event, e.getUserFriendlyMessage())
            } catch (e: DiscordApiException) {
                logger.error("Discord API error in slash command $commandName", e)
                sendErrorResponse(event, e.getUserFriendlyMessage())
            } catch (e: BotException) {
                logger.error("Bot exception in slash command $commandName", e)
                sendErrorResponse(event, e.getUserFriendlyMessage())
            } catch (e: Exception) {
                logger.error("Unexpected error executing slash command $commandName", e)
                val exception = CommandExecutionException("An unexpected error occurred", e, commandName)
                sendErrorResponse(event, exception.getUserFriendlyMessage())
            }
        } else {
            logger.warn("Received unknown slash command: $commandName")
            if (!event.isAcknowledged) {
                event.reply("Unknown command: $commandName").setEphemeral(true).queue()
            }
        }
    }

    /**
     * Helper function to send error responses for slash commands.
     * Handles the complexity of whether the interaction has already been acknowledged.
     */
    private fun sendErrorResponse(event: SlashCommandInteractionEvent, message: String) {
        if (event.isAcknowledged) {
            event.hook.sendMessage(message).setEphemeral(true).queue()
        } else {
            event.reply(message).setEphemeral(true).queue()
        }
    }
}