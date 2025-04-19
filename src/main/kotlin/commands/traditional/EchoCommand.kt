package commands.traditional

import commands.Command
import commands.CommandManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.time.Instant


/**
 * A simple echo command that repeats what the user says.
 */
class EchoCommand : Command {
    override val name = "echo"
    override val description = "Repeats what you say"
    override val usage = "echo [message]"
    
    override suspend fun execute(args: List<String>, event: MessageReceivedEvent) {
        if (args.isEmpty()) {
            event.channel.sendMessage("You didn't provide anything to echo!").queue()
            return
        }
        
        val message = args.joinToString(" ")
        event.channel.sendMessage(message).queue()
    }
}