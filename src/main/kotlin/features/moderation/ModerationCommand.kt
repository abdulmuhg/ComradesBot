package features.moderation

import commands.SlashCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A moderation command that provides various moderation actions (kick, ban, mute, etc.)
 */
class ModerationCommand : SlashCommand {
    private val logger = LoggerFactory.getLogger(ModerationCommand::class.java)

    override val name = "mod"
    override val description = "Moderation commands for server management"

    // This command can only be used by users with moderation permissions
    override fun toCommandData() = super.toCommandData()
        .setDefaultPermissions(
            DefaultMemberPermissions.enabledFor(
            Permission.KICK_MEMBERS,
            Permission.BAN_MEMBERS,
            Permission.MODERATE_MEMBERS
        ))
        // Add subcommands for different moderation actions
        .addSubcommands(
            SubcommandData("kick", "Kick a member from the server")
                .addOption(OptionType.USER, "user", "The user to kick", true)
                .addOption(OptionType.STRING, "reason", "Reason for kicking the user", false),

            SubcommandData("ban", "Ban a member from the server")
                .addOption(OptionType.USER, "user", "The user to ban", true)
                .addOption(OptionType.INTEGER, "delete_days", "Number of days of messages to delete (0-7)", false)
                .addOption(OptionType.STRING, "reason", "Reason for banning the user", false),

            SubcommandData("timeout", "Timeout (mute) a member")
                .addOption(OptionType.USER, "user", "The user to timeout", true)
                .addOption(OptionType.INTEGER, "duration", "Duration of the timeout in minutes", true)
                .addOption(OptionType.STRING, "reason", "Reason for the timeout", false),

            SubcommandData("removetimeout", "Remove a timeout from a member")
                .addOption(OptionType.USER, "user", "The user to remove timeout from", true),

            SubcommandData("purge", "Delete a number of messages from a channel")
                .addOption(OptionType.INTEGER, "amount", "Number of messages to delete (1-100)", true)
                .addOption(OptionType.USER, "user", "Only delete messages from this user", false)
        )

    override val isGuildOnly = true

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Check if we're in a guild
        val guild = event.guild
        if (guild == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue()
            return
        }

        // Check which subcommand was used
        when (event.subcommandName) {
            "kick" -> handleKick(event)
            "ban" -> handleBan(event)
            "timeout" -> handleTimeout(event)
            "removetimeout" -> handleRemoveTimeout(event)
            "purge" -> handlePurge(event)
            else -> event.reply("Unknown moderation command").setEphemeral(true).queue()
        }
    }

    /**
     * Handle the kick subcommand
     */
    private fun handleKick(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val targetUser = event.getOption("user")?.asUser
        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        if (targetUser == null) {
            event.reply("You must specify a user to kick").setEphemeral(true).queue()
            return
        }

        // Get the member object for the target user
        guild.retrieveMember(targetUser).queue({ member ->
            // Check if the bot can kick this user
            if (!guild.selfMember.canInteract(member)) {
                event.reply("I don't have permission to kick this user. They may have higher roles than me.").setEphemeral(true).queue()
                return@queue
            }

            // Check if the command user can kick this user
            val commandMember = event.member
            if (commandMember != null && !commandMember.canInteract(member)) {
                event.reply("You don't have permission to kick this user. They may have higher roles than you.").setEphemeral(true).queue()
                return@queue
            }

            // Perform the kick
            member.kick(reason).queue(
                {
                    // Success
                    event.reply("**${targetUser.name}** has been kicked. Reason: $reason").queue()
                    logger.info("${event.user.name} kicked ${targetUser.name} for: $reason")
                },
                { error ->
                    // Failure
                    event.reply("Failed to kick **${targetUser.name}**: ${error.message}").setEphemeral(true).queue()
                    logger.error("Failed to kick ${targetUser.name}: ${error.message}")
                }
            )
        }, { error ->
            // User not in server
            event.reply("This user is not a member of this server.").setEphemeral(true).queue()
        })
    }

    /**
     * Handle the ban subcommand
     */
    private fun handleBan(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val targetUser = event.getOption("user")?.asUser
        val deleteDays = event.getOption("delete_days")?.asLong?.toInt() ?: 1
        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        if (targetUser == null) {
            event.reply("You must specify a user to ban").setEphemeral(true).queue()
            return
        }

        // Validate deleteDays (Discord allows 0-7)
        val validDeleteDays = deleteDays.coerceIn(0, 7)

        // Check if the user is a member of the server
        guild.retrieveMember(targetUser).queue({ member ->
            // Check if the bot can ban this user
            if (!guild.selfMember.canInteract(member)) {
                event.reply("I don't have permission to ban this user. They may have higher roles than me.").setEphemeral(true).queue()
                return@queue
            }

            // Check if the command user can ban this user
            val commandMember = event.member
            if (commandMember != null && !commandMember.canInteract(member)) {
                event.reply("You don't have permission to ban this user. They may have higher roles than you.").setEphemeral(true).queue()
                return@queue
            }

            // Perform the ban
            guild.ban(member, validDeleteDays, TimeUnit.DAYS).reason(reason).queue(
                {
                    // Success
                    event.reply("**${targetUser.name}** has been banned. Reason: $reason").queue()
                    logger.info("${event.user.name} banned ${targetUser.name} for: $reason")
                },
                { error ->
                    // Failure
                    event.reply("Failed to ban **${targetUser.name}**: ${error.message}").setEphemeral(true).queue()
                    logger.error("Failed to ban ${targetUser.name}: ${error.message}")
                }
            )
        }, {
            // User not in server, we can still ban by ID
            guild.ban(targetUser, validDeleteDays, TimeUnit.DAYS).reason(reason).queue(
                {
                    // Success
                    event.reply("**${targetUser.name}** has been banned. Reason: $reason").queue()
                    logger.info("${event.user.name} banned ${targetUser.name} (not in server) for: $reason")
                },
                { error ->
                    // Failure
                    event.reply("Failed to ban **${targetUser.name}**: ${error.message}").setEphemeral(true).queue()
                    logger.error("Failed to ban ${targetUser.name}: ${error.message}")
                }
            )
        })
    }

    /**
     * Handle the timeout subcommand
     */
    private fun handleTimeout(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val targetUser = event.getOption("user")?.asUser
        val duration = event.getOption("duration")?.asLong ?: 10 // Default 10 minutes
        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        if (targetUser == null) {
            event.reply("You must specify a user to timeout").setEphemeral(true).queue()
            return
        }

        // Get the member object for the target user
        guild.retrieveMember(targetUser).queue({ member ->
            // Check if the bot can timeout this user
            if (!guild.selfMember.canInteract(member)) {
                event.reply("I don't have permission to timeout this user. They may have higher roles than me.").setEphemeral(true).queue()
                return@queue
            }

            // Check if the command user can timeout this user
            val commandMember = event.member
            if (commandMember != null && !commandMember.canInteract(member)) {
                event.reply("You don't have permission to timeout this user. They may have higher roles than you.").setEphemeral(true).queue()
                return@queue
            }

            // Apply the timeout
            member.timeoutFor(duration, TimeUnit.MINUTES).reason(reason).queue(
                {
                    // Success
                    event.reply("**${targetUser.name}** has been timed out for $duration minutes. Reason: $reason").queue()
                    logger.info("${event.user.name} timed out ${targetUser.name} for $duration minutes. Reason: $reason")
                },
                { error ->
                    // Failure
                    event.reply("Failed to timeout **${targetUser.name}**: ${error.message}").setEphemeral(true).queue()
                    logger.error("Failed to timeout ${targetUser.name}: ${error.message}")
                }
            )
        }, { error ->
            // User not in server
            event.reply("This user is not a member of this server.").setEphemeral(true).queue()
        })
    }

    /**
     * Handle the removetimeout subcommand
     */
    private fun handleRemoveTimeout(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val targetUser = event.getOption("user")?.asUser

        if (targetUser == null) {
            event.reply("You must specify a user to remove timeout from").setEphemeral(true).queue()
            return
        }

        // Get the member object for the target user
        guild.retrieveMember(targetUser).queue({ member ->
            // Check if the bot can modify this user
            if (!guild.selfMember.canInteract(member)) {
                event.reply("I don't have permission to modify this user. They may have higher roles than me.").setEphemeral(true).queue()
                return@queue
            }

            // Check if the command user can modify this user
            val commandMember = event.member
            if (commandMember != null && !commandMember.canInteract(member)) {
                event.reply("You don't have permission to modify this user. They may have higher roles than you.").setEphemeral(true).queue()
                return@queue
            }

            // Remove the timeout
            member.removeTimeout().queue(
                {
                    // Success
                    event.reply("Timeout has been removed from **${targetUser.name}**.").queue()
                    logger.info("${event.user.name} removed timeout from ${targetUser.name}")
                },
                { error ->
                    // Failure
                    event.reply("Failed to remove timeout from **${targetUser.name}**: ${error.message}").setEphemeral(true).queue()
                    logger.error("Failed to remove timeout from ${targetUser.name}: ${error.message}")
                }
            )
        }, { error ->
            // User not in server
            event.reply("This user is not a member of this server.").setEphemeral(true).queue()
        })
    }

    /**
     * Handle the purge subcommand
     */
    private fun handlePurge(event: SlashCommandInteractionEvent) {
        val amount = event.getOption("amount")?.asLong?.toInt() ?: 10
        val targetUser = event.getOption("user")?.asUser

        // Validate amount (Discord allows 1-100)
        val validAmount = amount.coerceIn(1, 100)

        // Acknowledge the command while we perform the purge
        event.deferReply(true).queue()

        // Get message history from the channel
        val textChannel = event.channel.asTextChannel()

        // If a target user is specified, filter messages by that user
        if (targetUser != null) {
            textChannel.iterableHistory.takeAsync(200) // Take more to ensure we find enough by the user
                .thenApply { messages ->
                    messages.filter { it.author.id == targetUser.id }.take(validAmount)
                }
                .thenAccept { messagesToDelete ->
                    if (messagesToDelete.isEmpty()) {
                        event.hook.sendMessage("No messages from ${targetUser.name} found to delete.").queue()
                        return@thenAccept
                    }

                    // Delete the messages
                    if (messagesToDelete.size == 1) {
                        // Can only use purge for 2+ messages
                        messagesToDelete.first().delete().queue(
                            {
                                event.hook.sendMessage("Deleted 1 message from ${targetUser.name}.").queue()
                                logger.info("${event.user.name} deleted 1 message from ${targetUser.name}")
                            },
                            { error ->
                                event.hook.sendMessage("Failed to delete message: ${error.message}").queue()
                                logger.error("Failed to delete message: ${error.message}")
                            }
                        )
                    } else {
                        textChannel.deleteMessages(messagesToDelete).queue(
                            {
                                event.hook.sendMessage("Deleted ${messagesToDelete.size} messages from ${targetUser.name}.").queue()
                                logger.info("${event.user.name} deleted ${messagesToDelete.size} messages from ${targetUser.name}")
                            },
                            { error ->
                                event.hook.sendMessage("Failed to delete messages: ${error.message}").queue()
                                logger.error("Failed to delete messages: ${error.message}")
                            }
                        )
                    }
                }
        } else {
            // Delete messages without filtering by user
            textChannel.history.retrievePast(validAmount).queue(
                { messages ->
                    if (messages.isEmpty()) {
                        event.hook.sendMessage("No messages found to delete.").queue()
                        return@queue
                    }

                    // Delete the messages
                    if (messages.size == 1) {
                        // Can only use purge for 2+ messages
                        messages.first().delete().queue(
                            {
                                event.hook.sendMessage("Deleted 1 message.").queue()
                                logger.info("${event.user.name} deleted 1 message")
                            },
                            { error ->
                                event.hook.sendMessage("Failed to delete message: ${error.message}").queue()
                                logger.error("Failed to delete message: ${error.message}")
                            }
                        )
                    } else {
                        textChannel.deleteMessages(messages).queue(
                            {
                                event.hook.sendMessage("Deleted ${messages.size} messages.").queue()
                                logger.info("${event.user.name} deleted ${messages.size} messages")
                            },
                            { error ->
                                event.hook.sendMessage("Failed to delete messages: ${error.message}").queue()
                                logger.error("Failed to delete messages: ${error.message}")
                            }
                        )
                    }
                },
                { error ->
                    event.hook.sendMessage("Failed to retrieve messages: ${error.message}").queue()
                    logger.error("Failed to retrieve messages: ${error.message}")
                }
            )
        }
    }
}