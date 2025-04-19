import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
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
 */
fun main(args: Array<String> = emptyArray()) {
    // Check if a token was provided as a command-line argument
    val directToken = if (args.isNotEmpty()) args[0] else null

    // Initialize the bot with optional direct token
    DiscordBot.initialize(directToken)
}

/**
 * Main bot class that handles initialization and event listening.
 */
object DiscordBot : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(DiscordBot::class.java)
    private val commandManager = CommandManager()
    private val prefix by lazy { ConfigLoader.getCommandPrefix() }

    /**
     * Initialize the bot and connect to Discord.
     *
     * @param directToken Optional token to use instead of loading from config
     */
    fun initialize(directToken: String? = null) {
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

        try {
            // Build the JDA instance
            logger.info("Building JDA instance...")
            val jda = JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.playing("Type $prefix help"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(this)
                .build()

            // Block until it's connected
            logger.info("Waiting for connection...")
            jda.awaitReady()
            logger.info("Bot has successfully connected to Discord!")
        } catch (e: Exception) {
            logger.error("Failed to initialize bot: ${e.message}", e)
            logger.error("Please check that your token is correct and that you have proper internet connectivity")
            throw e
        }
    }

    /**
     * Register all command handlers.
     */
    private fun registerCommands() {
        // Register basic commands
        commandManager.registerCommand(PingCommand())
        commandManager.registerCommand(EchoCommand())
        // Add the help command last so it can see all other commands
        commandManager.registerCommand(HelpCommand(commandManager))

        logger.info("Registered all commands")
    }

    /**
     * Handle the ready event when the bot connects to Discord.
     */
    override fun onReady(event: ReadyEvent) {
        logger.info("Bot is ready! Connected to ${event.guildAvailableCount} guilds")
    }

    /**
     * Handle incoming messages.
     */
    override fun onMessageReceived(event: MessageReceivedEvent) {
        // Process commands using coroutines
        runBlocking {
            commandManager.handleMessage(prefix, event)
        }
    }
}