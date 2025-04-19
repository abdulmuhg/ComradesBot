package commands.traditional

import commands.Command
import commands.CommandManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.time.Instant

/**
 * A help command that lists all available commands.
 */
class HelpCommand(private val commandManager: CommandManager) : Command {
    override val name = "help"
    override val description = "Shows a list of all commands"
    override val usage = "help [command]"
    
    override suspend fun execute(args: List<String>, event: MessageReceivedEvent) {
        val embed = EmbedBuilder()
            .setTitle("Bot Commands")
            .setColor(Color.BLUE)
            .setTimestamp(Instant.now())
        
        if (args.isEmpty()) {
            // List all commands
            commandManager.getCommands().forEach { (name, command) ->
                embed.addField(name, command.description, false)
            }
            embed.setFooter("Type !help [command] for detailed usage information")
        } else {
            // Show details for a specific command
            val commandName = args[0].lowercase()
            val command = commandManager.getCommands()[commandName]
            
            if (command != null) {
                embed.addField("commands.Command", command.name, false)
                embed.addField("Description", command.description, false)
                embed.addField("Usage", command.usage, false)
            } else {
                embed.setTitle("Unknown commands.Command")
                embed.setDescription("The command `$commandName` does not exist.")
                embed.setColor(Color.RED)
            }
        }
        
        event.channel.sendMessageEmbeds(embed.build()).queue()
    }
}