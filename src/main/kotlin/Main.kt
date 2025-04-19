import commands.CommandManager
import commands.SlashCommandManager
import commands.slash.EchoSlashCommand
import commands.slash.PingSlashCommand
import commands.slash.ServerInfoSlashCommand
import commands.slash.UserInfoSlashCommand
import commands.traditional.EchoCommand
import commands.traditional.HelpCommand
import commands.traditional.PingCommand
import config.ConfigLoader
import event.EventHandler
import features.poll.PollManager
import features.poll.PollSlashCommand
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.slf4j.LoggerFactory

/**
 * Main entry point for the Discord bot.
 *
 * If you're having trouble with the config file, you can temporarily
 * pass your bot token directly as a command-line argument:
 *
 * ./gradlew run --args="YOUR_BOT_TOKEN"
 *
 * You can also specify a specific guild ID to register slash commands to:
 * ./gradlew run --args="YOUR_BOT_TOKEN YOUR_GUILD_ID"
 */
fun main(args: Array<String> = emptyArray()) {
    // Check if a token was provided as a command-line argument
    val directToken = if (args.isNotEmpty()) args[0] else null

    // Check if a guild ID was provided for slash command registration
    val guildId = if (args.size > 1) args[1] else null

    // Initialize the bot with optional direct token and guild ID
    DiscordBot.initialize(directToken, guildId)
}

/**
 * Main bot class that handles initialization and event listening.
 */
object DiscordBot : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(DiscordBot::class.java)
    private val commandManager = CommandManager()
    private val slashCommandManager = SlashCommandManager()
    private val prefix by lazy { ConfigLoader.getCommandPrefix() }

    /**
     * Initialize the bot and connect to Discord.
     *
     * @param directToken Optional token to use instead of loading from config
     * @param guildId Optional guild ID to register slash commands to
     */
    fun initialize(directToken: String? = null, guildId: String? = null) {
        logger.info("Starting bot initialization...")

        // Get the bot token either from the parameter or from config
        val token = directToken ?: run {
            try {
                ConfigLoader.getBotToken()
            } catch (e: Exception) {
                logger.error("Failed to load token from config: ${e.message}")
                logger.info("Tip: You can provide the token directly by running: ./gradlew run --args=\"YOUR_BOT_TOKEN\"")
                throw e
            }
        }

        if (token == "YOUR_BOT_TOKEN_HERE") {
            logger.error("Invalid token: You need to replace 'YOUR_BOT_TOKEN_HERE' with your actual bot token")
            throw IllegalArgumentException("Invalid token: Please update config.properties with your actual bot token")
        }

        // Log token length for debugging (don't log the actual token)
        logger.info("Using token with length: ${token.length}")

        // Register commands
        registerCommands()
        registerSlashCommands()

        try {
            // Create the event handler
            val eventHandler = EventHandler()

            // Build the JDA instance
            logger.info("Building JDA instance...")
            val jda = JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_PRESENCES  // Added for member status in ServerInfoCommand
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.playing("Type / for commands"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this, eventHandler, PollManager)  // Added features.poll.PollManager as an event listener
                .build()

            // Block until it's connected
            logger.info("Waiting for connection...")
            jda.awaitReady()
            logger.info("Bot has successfully connected to Discord!")

            // Register slash commands with Discord
            slashCommandManager.registerCommandsWithDiscord(jda, guildId)
            logger.info("Slash commands ${if (guildId != null) "registered to guild $guildId" else "registered globally"}")

        } catch (e: Exception) {
            logger.error("Failed to initialize bot: ${e.message}", e)
            logger.error("Please check that your token is correct and that you have proper internet connectivity")
            throw e
        }
    }

    /**
     * Register all traditional command handlers.
     */
    private fun registerCommands() {
        // Register basic commands
        commandManager.registerCommand(PingCommand())
        commandManager.registerCommand(EchoCommand())
        // Add the help command last so it can see all other commands
        commandManager.registerCommand(HelpCommand(commandManager))

        logger.info("Registered all traditional commands")
    }

    /**
     * Register all slash commands.
     */
    private fun registerSlashCommands() {
        // Register slash commands
        slashCommandManager.registerCommand(PingSlashCommand())
        slashCommandManager.registerCommand(EchoSlashCommand())
        slashCommandManager.registerCommand(UserInfoSlashCommand())
        slashCommandManager.registerCommand(ServerInfoSlashCommand())  // Added ServerInfo command
        slashCommandManager.registerCommand(PollSlashCommand())  // Added Poll command

        logger.info("Registered all slash commands")
    }

    /**
     * Handle the ready event when the bot connects to Discord.
     */
    override fun onReady(event: ReadyEvent) {
        logger.info("Bot is ready! Connected to ${event.guildAvailableCount} guilds")
    }

    /**
     * Handle incoming messages for traditional commands.
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Process commands using coroutines
        runBlocking {
            commandManager.handleMessage(prefix, event)
        }
    }

    /**
     * Handle slash command interactions.
     */
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        // Process slash commands using coroutines
        runBlocking {
            slashCommandManager.handleSlashCommand(event)
        }
    }
}