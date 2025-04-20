package config

import error.ConfigurationException
import org.slf4j.LoggerFactory
import java.util.Properties

/**
 * Loads configuration properties from a file.
 * This class is responsible for loading and providing access to configuration values.
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    private val properties = Properties()

    init {
        try {
            logger.info("Loading configuration from config.properties")
            val inputStream = javaClass.classLoader.getResourceAsStream("config.properties")
                ?: throw ConfigurationException("Unable to find config.properties in resources")

            properties.load(inputStream)
            inputStream.close()

            // Validate required properties
            validateRequiredProperties()

            logger.info("Configuration loaded successfully")
        } catch (e: ConfigurationException) {
            logger.error("Configuration error: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to load configuration: ${e.message}", e)
            throw ConfigurationException("Failed to load configuration: ${e.message}", e)
        }
    }

    /**
     * Validates that all required properties are present in the config file.
     */
    private fun validateRequiredProperties() {
        val requiredProperties = listOf("bot.token", "bot.prefix")

        val missingProperties = requiredProperties.filter { !properties.containsKey(it) }
        if (missingProperties.isNotEmpty()) {
            throw ConfigurationException("Missing required properties: ${missingProperties.joinToString(", ")}")
        }

        // Check if token is the placeholder
        val token = properties.getProperty("bot.token")
        if (token == "YOUR_BOT_TOKEN_HERE") {
            throw ConfigurationException("Bot token is still set to the default placeholder. Please update with your actual token.")
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