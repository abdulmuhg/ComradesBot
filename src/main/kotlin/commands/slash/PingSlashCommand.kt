package commands.slash

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.awt.Color
import java.time.Instant

/**
 * A simple ping slash command to check if the bot is running.
 */
class PingSlashCommand : SlashCommand {
    override val name = "ping"
    override val description = "Checks the bot's response time"
    
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // First acknowledge the command to let Discord know we received it
        event.deferReply().queue()
        
        // Calculate the bot latency
        val gatewayPing = event.jda.gatewayPing
        val restPing = event.jda.restPing.complete()
        
        // Build a response with the ping information
        val embed = EmbedBuilder()
            .setTitle("🏓 Pong!")
            .setDescription("Bot is up and running!")
            .addField("Gateway Ping", "${gatewayPing}ms", true)
            .addField("REST API Ping", "${restPing}ms", true)
            .setColor(Color.GREEN)
            .setTimestamp(Instant.now())
            .build()
        
        // Send the response
        event.hook.sendMessageEmbeds(embed).queue()
    }
}