package features.roles

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

/**
 * A slash command to create reaction role messages.
 * Users can get roles by reacting to these messages.
 */
class ReactionRolesCommand : SlashCommand {
    override val name = "reactionrole"
    override val description = "Create a reaction role message"
    
    // This command requires specific options
    override val options = listOf(
        OptionData(OptionType.STRING, "title", "Title for the reaction roles message", true),
        OptionData(OptionType.STRING, "description", "Description explaining how to use the reaction roles", true),
        OptionData(OptionType.ROLE, "role1", "First role to assign", true),
        OptionData(OptionType.STRING, "emoji1", "Emoji for the first role (Unicode or custom emoji name)", true),
        OptionData(OptionType.ROLE, "role2", "Second role to assign", false),
        OptionData(OptionType.STRING, "emoji2", "Emoji for the second role", false),
        OptionData(OptionType.ROLE, "role3", "Third role to assign", false),
        OptionData(OptionType.STRING, "emoji3", "Emoji for the third role", false),
        OptionData(OptionType.ROLE, "role4", "Fourth role to assign", false),
        OptionData(OptionType.STRING, "emoji4", "Emoji for the fourth role", false),
        OptionData(OptionType.ROLE, "role5", "Fifth role to assign", false),
        OptionData(OptionType.STRING, "emoji5", "Emoji for the fifth role", false)
    )
    
    // This command is guild-only and requires manage roles permission
    override val isGuildOnly = true
    
    override fun toCommandData() = super.toCommandData()
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
    
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Check if we're in a guild context
        val guild = event.guild
        if (guild == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue()
            return
        }
        
        // Check if the bot has permission to manage roles
        val selfMember = guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            event.reply("I don't have permission to manage roles in this server.").setEphemeral(true).queue()
            return
        }
        
        // Get title and description
        val title = event.getOption("title")?.asString ?: "Reaction Roles"
        val description = event.getOption("description")?.asString 
            ?: "React to this message to get the corresponding role."
        
        // Collect role-emoji pairs
        val roleEmojiPairs = mutableListOf<Pair<String, String>>()
        
        // Process up to 5 role-emoji pairs
        for (i in 1..5) {
            val role = event.getOption("role$i")?.asRole
            val emojiStr = event.getOption("emoji$i")?.asString
            
            if (role != null && emojiStr != null) {
                // Add the pair
                roleEmojiPairs.add(Pair(role.id, emojiStr))
            }
        }
        
        // Create the embed
        val embed = EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(Color.BLUE)
        
        // Add fields for each role-emoji pair
        roleEmojiPairs.forEach { (roleId, emojiStr) ->
            val role = guild.getRoleById(roleId)
            if (role != null) {
                embed.addField(role.name, "$emojiStr - ${role.name}", false)
            }
        }
        
        // Send the message with the embed
        event.reply("Creating reaction roles message...").setEphemeral(true).queue()
        
        event.channel.sendMessageEmbeds(embed.build()).queue { message ->
            // Add reactions to the message
            roleEmojiPairs.forEach { (roleId, emojiStr) ->
                try {
                    // Try to parse as custom emoji first
                    val emoji = if (emojiStr.matches(Regex("<a?:\\w+:\\d+>"))) {
                        // Custom emoji format: <:name:id> or <a:name:id>
                        val emojiId = emojiStr.substringAfterLast(":").dropLast(1)
                        Emoji.fromCustom(emojiStr.substringAfter(":").substringBefore(":"), emojiId.toLong(), emojiStr.startsWith("<a"))
                    } else {
                        // Unicode emoji
                        Emoji.fromUnicode(emojiStr)
                    }
                    
                    message.addReaction(emoji).queue()
                    
                    // Register this message for reaction roles
                    ReactionRoleManager.registerReactionRole(message.id, emojiStr, roleId)
                    
                } catch (e: Exception) {
                    // If adding the reaction fails, log an error
                    ReactionRoleManager.logger.error("Failed to add emoji $emojiStr: ${e.message}")
                }
            }
        }
    }
}

/**
 * Manages reaction role messages and handles reactions.
 */
object ReactionRoleManager : ListenerAdapter() {
    val logger = LoggerFactory.getLogger(ReactionRoleManager::class.java)
    
    // Maps messageId -> Map<emojiString, roleId>
    private val reactionRoles = ConcurrentHashMap<String, MutableMap<String, String>>()
    
    /**
     * Register a new reaction-role mapping.
     */
    fun registerReactionRole(messageId: String, emojiStr: String, roleId: String) {
        val messageRoles = reactionRoles.computeIfAbsent(messageId) { mutableMapOf() }
        messageRoles[emojiStr] = roleId
        logger.info("Registered reaction role: Message $messageId, Emoji $emojiStr, Role $roleId")
    }
    
    /**
     * Handle reaction add events.
     */
    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        // Ignore bot reactions
        if (event.user?.isBot == true) return
        
        // Check if this is a reaction role message
        val messageId = event.messageId
        val messageRoles = reactionRoles[messageId] ?: return
        
        // Get the emoji
        val emoji = event.reaction.emoji
        val emojiStr = if (emoji.type == Emoji.Type.CUSTOM) {
            emoji.asCustom().name
        } else {
            emoji.asUnicode().name
        }
        
        // Find the associated role
        val roleId = messageRoles[emojiStr] ?: return
        
        // Get the guild and role
        val guild = event.guild
        val role = guild.getRoleById(roleId)
        val user = event.user ?: return
        val member = guild.getMember(user) ?: return
        
        // Add the role
        if (role != null && !member.roles.contains(role)) {
            guild.addRoleToMember(member, role).queue(
                { logger.info("Added role ${role.name} to ${member.effectiveName}") },
                { logger.error("Failed to add role ${role.name} to ${member.effectiveName}: ${it.message}") }
            )
        }
    }
    
    /**
     * Handle reaction remove events.
     */
    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        // Check if this is a reaction role message
        val messageId = event.messageId
        val messageRoles = reactionRoles[messageId] ?: return
        
        // Get the emoji
        val emoji = event.reaction.emoji
        val emojiStr = if (emoji.type == Emoji.Type.CUSTOM) {
            emoji.asCustom().name
        } else {
            emoji.asUnicode().name
        }
        
        // Find the associated role
        val roleId = messageRoles[emojiStr] ?: return
        
        // Get the guild and role
        val guild = event.guild
        val role = guild.getRoleById(roleId)
        val user = event.user ?: return
        val member = guild.getMember(user) ?: return
        
        // Remove the role
        if (role != null && member.roles.contains(role)) {
            guild.removeRoleFromMember(member, role).queue(
                { logger.info("Removed role ${role.name} from ${member.effectiveName}") },
                { logger.error("Failed to remove role ${role.name} from ${member.effectiveName}: ${it.message}") }
            )
        }
    }
    
    /**
     * Save reaction roles data (this would ideally be stored in a database).
     * For simplicity, this example doesn't implement persistence.
     */
    fun saveReactionRoles() {
        // In a real implementation, this would save to a database or file
        logger.info("Would save ${reactionRoles.size} reaction role configurations")
    }
    
    /**
     * Load reaction roles data (this would ideally be loaded from a database).
     * For simplicity, this example doesn't implement persistence.
     */
    fun loadReactionRoles() {
        // In a real implementation, this would load from a database or file
        logger.info("Would load reaction role configurations")
    }
}