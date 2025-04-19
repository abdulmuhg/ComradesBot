package commands.slash

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.awt.Color
import java.time.Instant

/**
 * A user info slash command that displays information about a user.
 */
class UserInfoSlashCommand : SlashCommand {
    override val name = "userinfo"
    override val description = "Displays information about a user"
    
    // This command has an optional user option, defaulting to the command user
    override val options = listOf(
        OptionData(OptionType.USER, "user", "The user to get info about", false)
    )
    
    override val isGuildOnly = true
    
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Get the target user, or use the command user if not specified
        val targetUser = event.getOption("user")?.asUser ?: event.user
        val member = event.guild?.getMember(targetUser)
        
        // Create an embed with user information
        val embed = EmbedBuilder()
            .setTitle("User Information")
            .setColor(member?.color ?: Color.BLUE)
            .setThumbnail(targetUser.effectiveAvatarUrl)
            .addField("Username", targetUser.name, true)
            .addField("Display Name", member?.effectiveName ?: targetUser.name, true)
            .addField("User ID", targetUser.id, true)
            .addField("Account Created", targetUser.timeCreated.toString(), false)
            
        // Add server-specific information if available
        if (member != null) {
            embed.addField("Joined Server", member.timeJoined.toString(), false)
            embed.addField("Roles", member.roles.joinToString(", ") { it.name }, false)
        }
        
        embed.setTimestamp(Instant.now())
        
        // Send the response
        event.replyEmbeds(embed.build()).queue()
    }
}