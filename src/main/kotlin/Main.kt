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
 */
fun main() {
    // Initialize the bot
    DiscordBot.initialize()
}

/**
 * Main bot class that handles initialization and event listening.
 */
object DiscordBot : ListenerAdapter() {
    private val logger = LoggerFactory.getLogger(DiscordBot::class.java)
    private val commandManager = CommandManager()
    private val prefix = ConfigLoader.getCommandPrefix()

    /**
     * Initialize the bot and connect to Discord.
     */
    fun initialize() {
        logger.info("Starting bot initialization...")

        // Register commands
        registerCommands()

        // Build the JDA instance
        val jda = JDABuilder.createDefault(ConfigLoader.getBotToken())
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
        jda.awaitReady()
        logger.info("Bot has successfully connected to Discord!")
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