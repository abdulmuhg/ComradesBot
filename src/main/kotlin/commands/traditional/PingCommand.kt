package commands.traditional

import commands.Command
import commands.CommandManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.time.Instant

/**
 * A simple ping command to check if the bot is running.
 */
class PingCommand : Command {
    override val name = "ping"
    override val description = "Checks the bot's response time"
    override val usage = "ping"
    
    override suspend fun execute(args: List<String>, event: MessageReceivedEvent) {
        val time = System.currentTimeMillis()
        event.channel.sendMessage("Pinging...").queue { message ->
            val ping = System.currentTimeMillis() - time
            message.editMessage("Pong! Response time: ${ping}ms").queue()
        }
    }
}