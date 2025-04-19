package event

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Instant

/**
 * EventHandler that listens for various Discord events and responds to them.
 */
class EventHandler : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(EventHandler::class.java)

    /**
     * Called when the bot successfully connects to Discord.
     */
    override fun onReady(event: ReadyEvent) {
        logger.info("Bot is ready! Connected to ${event.guildAvailableCount} guilds")
    }

    /**
     * Called when a guild (server) becomes available to the bot.
     */
    override fun onGuildReady(event: GuildReadyEvent) {
        val guild = event.guild
        logger.info("Guild ready: ${guild.name} (${guild.id}) with ${guild.memberCount} members")
    }

    /**
     * Called when a new member joins a guild.
     */
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val member = event.member
        val guild = event.guild
        logger.info("Member joined: ${member.user.name} (${member.id}) in ${guild.name}")

        // Try to find a welcome channel
        val welcomeChannel = findWelcomeChannel(guild)

        if (welcomeChannel != null) {
            // Create a welcome message embed
            val embed = EmbedBuilder()
                .setTitle("Welcome to ${guild.name}!")
                .setDescription("We're excited to have you join us, ${member.asMention}!")
                .setThumbnail(member.user.effectiveAvatarUrl)
                .setColor(Color.GREEN)
                .addField("Member Count", "You are member #${guild.memberCount}", false)
                .addField("Account Created", member.timeCreated.toString(), false)
                .setTimestamp(Instant.now())
                .setFooter("User ID: ${member.id}", null)
                .build()

            // Send the welcome message
            welcomeChannel.sendMessageEmbeds(embed).queue()
        }
    }

    /**
     * Called when a member leaves a guild.
     */
    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val user = event.user
        val guild = event.guild
        logger.info("Member left: ${user.name} (${user.id}) from ${guild.name}")

        // Try to find a farewell channel
        val farewellChannel = findWelcomeChannel(guild) // Reusing the same channel for simplicity

        if (farewellChannel != null) {
            // Create a farewell message
            val embed = EmbedBuilder()
                .setTitle("A Member Has Left")
                .setDescription("${user.asMention} has left the server.")
                .setThumbnail(user.effectiveAvatarUrl)
                .setColor(Color.RED)
                .addField("Member Count", "We now have ${guild.memberCount} members", false)
                .setTimestamp(Instant.now())
                .setFooter("User ID: ${user.id}", null)
                .build()

            // Send the farewell message
            farewellChannel.sendMessageEmbeds(embed).queue()
        }
    }

    /**
     * Tries to find an appropriate channel for welcome/farewell messages.
     * Looks for channels named "welcome" or "general" or the system channel.
     */
    private fun findWelcomeChannel(guild: Guild): TextChannel? {
        // First try to find a channel named "welcome"
        val welcomeChannel = guild.textChannels.firstOrNull {
            it.name.equals("welcome", ignoreCase = true) ||
            it.name.equals("greetings", ignoreCase = true)
        }

        if (welcomeChannel != null) {
            return welcomeChannel
        }

        // Then try to use the system channel
        val systemChannel = guild.systemChannel
        if (systemChannel != null) {
            return systemChannel
        }

        // Finally, try to find a general channel
        return guild.textChannels.firstOrNull {
            it.name.equals("general", ignoreCase = true) ||
            it.name.equals("chat", ignoreCase = true) ||
            it.name.equals("main", ignoreCase = true)
        }
    }
}