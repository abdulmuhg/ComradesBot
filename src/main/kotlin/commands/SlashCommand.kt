package commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/**
 * Interface for slash commands. All slash commands should implement this interface.
 */
interface SlashCommand {
    /**
     * The name of the slash command.
     */
    val name: String

    /**
     * A brief description of what the command does.
     */
    val description: String

    /**
     * Options/arguments for the command.
     */
    val options: List<OptionData>
        get() = listOf() // No options by default

    /**
     * Whether the command should be visible only to the user who invoked it.
     */
    val isGuildOnly: Boolean
        get() = false

    /**
     * Execute the command logic.
     *
     * @param event The slash command interaction event
     */
    suspend fun execute(event: SlashCommandInteractionEvent)

    /**
     * Convert this command to JDA's SlashCommandData.
     */
    fun toCommandData(): SlashCommandData {
        return Commands.slash(name, description).addOptions(options)
    }
}