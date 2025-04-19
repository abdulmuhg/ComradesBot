package commands.slash

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.awt.Color
import java.time.Instant

/**
 * An echo slash command that repeats what the user says.
 */
class EchoSlashCommand : SlashCommand {
    override val name = "echo"
    override val description = "Repeats what you say"
    
    // This command has a required message option
    override val options = listOf(
        OptionData(OptionType.STRING, "message", "The message to echo", true)
    )
    
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Get the message option
        val message = event.getOption("message")?.asString
        
        if (message != null) {
            // Echo the message back to the user
            event.reply(message).queue()
        } else {
            // This shouldn't happen as the option is required, but handle it just in case
            event.reply("You didn't provide anything to echo!").setEphemeral(true).queue()
        }
    }
}