package commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 * Interface for bot commands.
 * All commands should implement this interface.
 */
interface Command {
    /**
     * The name of the command (what users will type after the prefix).
     */
    val name: String
    
    /**
     * A brief description of what the command does.
     */
    val description: String
    
    /**
     * Usage information, showing how to properly use the command.
     */
    val usage: String
    
    /**
     * Execute the command logic.
     * 
     * @param args The command arguments (split by spaces)
     * @param event The original message event
     */
    suspend fun execute(args: List<String>, event: MessageReceivedEvent)
}