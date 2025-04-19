package features.poll

import commands.SlashCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * A slash command that creates interactive polls with buttons.
 *
 * Note: This command requires a ButtonListener to handle the button interactions.
 */
class PollSlashCommand : SlashCommand {
    override val name = "poll"
    override val description = "Create a poll with up to 5 options"

    // Define options for the poll command
    override val options = listOf(
        OptionData(OptionType.STRING, "question", "The poll question", true),
        OptionData(OptionType.STRING, "option1", "First option", true),
        OptionData(OptionType.STRING, "option2", "Second option", true),
        OptionData(OptionType.STRING, "option3", "Third option (optional)", false),
        OptionData(OptionType.STRING, "option4", "Fourth option (optional)", false),
        OptionData(OptionType.STRING, "option5", "Fifth option (optional)", false),
        OptionData(OptionType.INTEGER, "duration", "Poll duration in minutes (default: 60)", false)
    )

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Get the poll question and options
        val question = event.getOption("question")?.asString ?: return
        val option1 = event.getOption("option1")?.asString ?: return
        val option2 = event.getOption("option2")?.asString ?: return
        val option3 = event.getOption("option3")?.asString
        val option4 = event.getOption("option4")?.asString
        val option5 = event.getOption("option5")?.asString

        // Get the poll duration (default: 60 minutes)
        val duration = event.getOption("duration")?.asLong ?: 60L

        // Create a unique ID for this poll
        val pollId = "poll-${Random.nextInt(100000)}"

        // Create an embed for the poll
        val embed = EmbedBuilder()
            .setTitle("📊 Poll: $question")
            .setDescription("Click the buttons below to vote. Poll ends in $duration minutes.")
            .setColor(Color.CYAN)
            .setFooter("Poll created by ${event.user.name}", event.user.effectiveAvatarUrl)
            .build()

        // Create buttons for each option
        val buttons = mutableListOf<Button>()

        // Always add the first two required options
        buttons.add(Button.primary("$pollId:1", "1️⃣ $option1"))
        buttons.add(Button.primary("$pollId:2", "2️⃣ $option2"))

        // Add optional options if provided
        if (option3 != null) {
            buttons.add(Button.primary("$pollId:3", "3️⃣ $option3"))
        }
        if (option4 != null) {
            buttons.add(Button.primary("$pollId:4", "4️⃣ $option4"))
        }
        if (option5 != null) {
            buttons.add(Button.primary("$pollId:5", "5️⃣ $option5"))
        }

        // Register this poll with the button listener
        PollManager.createPoll(
            pollId,
            question,
            listOfNotNull(option1, option2, option3, option4, option5)
        )

        // Send the poll message with buttons
        event.reply("Poll created! Voting ends in $duration minutes.").setEmbeds(embed).addActionRow(buttons).queue { hook ->
            // Schedule poll end after the specified duration
            hook.retrieveOriginal().queue { message ->
                PollManager.schedulePollEnd(pollId, message, duration, TimeUnit.MINUTES)
            }
        }
    }
}

/**
 * Manages active polls and handles button interactions.
 */
object PollManager : ListenerAdapter() {
    // Maps poll IDs to poll data
    private val activePolls = ConcurrentHashMap<String, PollData>()

    // Create a scheduler for ending polls
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    /**
     * Data class to hold poll information
     */
    data class PollData(
        val question: String,
        val options: List<String>,
        val votes: MutableMap<String, MutableSet<String>> = mutableMapOf()
    )

    /**
     * Create a new poll
     */
    fun createPoll(id: String, question: String, options: List<String>) {
        val pollData = PollData(question, options)

        // Initialize vote counters for each option
        options.forEachIndexed { index, _ ->
            pollData.votes["${index + 1}"] = mutableSetOf()
        }

        activePolls[id] = pollData
    }

    /**
     * Schedule a poll to end after the specified duration
     */
    fun schedulePollEnd(pollId: String, message: Message, duration: Long, unit: TimeUnit) {
        scheduler.schedule({
            endPoll(pollId, message)
        }, duration, unit)
    }

    /**
     * End a poll and display the results
     */
    private fun endPoll(pollId: String, message: Message) {
        val pollData = activePolls[pollId] ?: return

        // Build results embed
        val embed = EmbedBuilder()
            .setTitle("📊 Poll Results: ${pollData.question}")
            .setColor(Color.GREEN)
            .setDescription("The poll has ended. Here are the results:")

        // Add results for each option
        pollData.options.forEachIndexed { index, option ->
            val optionKey = "${index + 1}"
            val votes = pollData.votes[optionKey]?.size ?: 0

            // Add emoji based on option number
            val emoji = when (index) {
                0 -> "1️⃣"
                1 -> "2️⃣"
                2 -> "3️⃣"
                3 -> "4️⃣"
                4 -> "5️⃣"
                else -> "🔹"
            }

            embed.addField("$emoji $option", "$votes votes", false)
        }

        // Find the winning option(s)
        val maxVotes = pollData.votes.values.maxOfOrNull { it.size } ?: 0
        val winners = pollData.votes.filter { it.value.size == maxVotes }.keys

        // Only display winner if there are votes
        if (maxVotes > 0) {
            val winnerOptions = winners.map { key ->
                val index = key.toInt() - 1
                pollData.options[index]
            }

            val winnerText = if (winnerOptions.size == 1) {
                "Winner: ${winnerOptions.first()} with $maxVotes votes"
            } else {
                "Tie between: ${winnerOptions.joinToString(", ")} with $maxVotes votes each"
            }

            embed.addField("📣 Results", winnerText, false)
        } else {
            embed.addField("📣 Results", "No votes were cast", false)
        }

        // Update the message with the results
        message.editMessageEmbeds(embed.build()).setComponents().queue()

        // Remove the poll from active polls
        activePolls.remove(pollId)
    }

    /**
     * Handle button interactions for polls
     */
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val buttonId = event.componentId

        // Check if this is a poll button
        if (!buttonId.startsWith("poll-")) {
            return
        }

        // Extract poll ID and option
        val parts = buttonId.split(":")
        if (parts.size != 2) {
            return
        }

        val pollId = parts[0]
        val optionKey = parts[1]

        // Get the poll data
        val pollData = activePolls[pollId] ?: run {
            event.reply("This poll has ended or is no longer active.").setEphemeral(true).queue()
            return
        }

        // Get the user ID
        val userId = event.user.id

        // Remove previous vote from this user if any
        pollData.votes.values.forEach { voters ->
            voters.remove(userId)
        }

        // Add the new vote
        pollData.votes[optionKey]?.add(userId)

        // Acknowledge the vote
        val optionIndex = optionKey.toInt() - 1
        val optionText = pollData.options.getOrNull(optionIndex) ?: "Unknown option"
        event.reply("You voted for: $optionText").setEphemeral(true).queue()
    }

    /**
     * Shutdown the scheduler when the bot is shutting down
     */
    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}