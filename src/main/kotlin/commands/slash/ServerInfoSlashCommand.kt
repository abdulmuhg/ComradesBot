package commands.slash

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.awt.Color
import java.time.format.DateTimeFormatter

/**
 * A slash command that displays detailed information about the current Discord server.
 */
class ServerInfoSlashCommand : SlashCommand {
    override val name = "serverinfo"
    override val description = "Displays information about this Discord server"

    // This command only works in a guild (server) context
    override val isGuildOnly = true

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // This command only makes sense in a guild context
        val guild = event.guild

        if (guild == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue()
            return
        }

        // Defer the reply since we'll be doing some calculations that might take a moment
        event.deferReply().queue()

        // Calculate member status counts
        val members = guild.loadMembers().get()
        val totalMembers = members.size
        val onlineMembers = members.count { it.onlineStatus == OnlineStatus.ONLINE }
        val idleMembers = members.count { it.onlineStatus == OnlineStatus.IDLE }
        val dndMembers = members.count { it.onlineStatus == OnlineStatus.DO_NOT_DISTURB }
        val offlineMembers = members.count { it.onlineStatus == OnlineStatus.OFFLINE }

        // Count bots
        val botCount = members.count { it.user.isBot }

        // Count channels by type
        val textChannels = guild.textChannels.size
        val voiceChannels = guild.voiceChannels.size
        val categoryChannels = guild.categories.size
        val totalChannels = textChannels + voiceChannels + categoryChannels

        // Count emojis
        val emojiCount = guild.emojis.size

        // Get creation date with a nicer format
        val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
        val creationDate = guild.timeCreated.format(dateFormatter)

        // Get server features
        val features = guild.features.joinToString(", ") { it.replace('_', ' ').lowercase() }

        // Create the embed
        val embed = EmbedBuilder()
            .setTitle("Server Information: ${guild.name}")
            .setThumbnail(guild.iconUrl)
            .setColor(Color.BLUE)

            // General info
            .addField("Owner", guild.owner?.effectiveName ?: "Unknown", true)
            .addField("Server ID", guild.id, true)
            .addField("Created On", creationDate, true)

            // Member counts
            .addField("Members", "Total: $totalMembers", false)
            .addField("Member Status",
                "🟢 Online: $onlineMembers\n" +
                "🟠 Idle: $idleMembers\n" +
                "🔴 Do Not Disturb: $dndMembers\n" +
                "⚫ Offline: $offlineMembers\n" +
                "🤖 Bots: $botCount",
                true)

            // Channel counts
            .addField("Channels",
                "💬 Text: $textChannels\n" +
                "🔊 Voice: $voiceChannels\n" +
                "📁 Categories: $categoryChannels\n" +
                "📊 Total: $totalChannels",
                true)

            // Other stats
            .addField("Roles", guild.roles.size.toString(), true)
            .addField("Emojis", emojiCount.toString(), true)
            .addField("Boost Tier", "Level ${guild.boostTier.key}", true)
            .addField("Boost Count", guild.boostCount.toString(), true)

        // Add server features if any exist
        if (features.isNotEmpty()) {
            embed.addField("Server Features", features, false)
        }

        // Set verification level
        embed.addField("Verification Level", guild.verificationLevel.name, true)

        // Send the embed
        event.hook.sendMessageEmbeds(embed.build()).queue()
    }
}