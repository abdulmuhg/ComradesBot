import java.util.Properties
import org.slf4j.LoggerFactory

/**
 * Loads configuration properties from a file.
 * This class is responsible for loading and providing access to configuration values.
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    private val properties = Properties()

    init {
        try {
            // Load properties from the config file
            logger.info("Attempting to load config.properties")
            val inputStream = javaClass.classLoader.getResourceAsStream("config.properties")

            if (inputStream == null) {
                logger.error("Unable to find config.properties in resources. Make sure it exists in src/main/resources/")
                throw IllegalStateException("Unable to find config.properties in resources")
            }

            properties.load(inputStream)
            inputStream.close()

            // Debug: Print loaded properties (careful not to log the actual token)
            logger.info("Config loaded successfully. Properties found: ${properties.stringPropertyNames().joinToString(", ")}")

            // Validate the token format
            val token = properties.getProperty("bot.token")
            if (token == null) {
                logger.error("Bot token not found in config")
            } else if (token == "YOUR_BOT_TOKEN_HERE") {
                logger.error("Bot token is still set to the default placeholder. Please update with your actual token.")
            } else {
                logger.info("Bot token found (length: ${token.length})")
            }

        } catch (e: Exception) {
            logger.error("Failed to load configuration: ${e.message}", e)
            throw RuntimeException("Failed to load configuration: ${e.message}", e)
        }
    }

    /**
     * Get a configuration property by key.
     *
     * @param key The property key
     * @return The property value
     */
    fun getProperty(key: String): String {
        val value = properties.getProperty(key)
        if (value == null) {
            logger.error("Property $key not found in configuration")
            throw IllegalArgumentException("Property $key not found in configuration")
        }
        return value
    }

    /**
     * Get the bot token from configuration.
     */
    fun getBotToken(): String {
        val token = getProperty("bot.token")
        // Make sure not to log the actual token, but verify it's not the placeholder
        if (token == "YOUR_BOT_TOKEN_HERE") {
            logger.error("Using placeholder token. Please replace it with your actual Discord bot token in config.properties")
        }
        return token
    }

    /**
     * Get the command prefix from configuration.
     */
    fun getCommandPrefix(): String {
        return getProperty("bot.prefix")
    }
}