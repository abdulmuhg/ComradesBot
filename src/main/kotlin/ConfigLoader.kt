import java.util.Properties
import java.io.FileInputStream

/**
 * Loads configuration properties from a file.
 * This class is responsible for loading and providing access to configuration values.
 */
object ConfigLoader {
    private val properties = Properties()
    
    init {
        try {
            // Load properties from the config file
            val inputStream = javaClass.classLoader.getResourceAsStream("config.properties")
                ?: throw IllegalStateException("Unable to find config.properties in resources")
            
            properties.load(inputStream)
            inputStream.close()
        } catch (e: Exception) {
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
        return properties.getProperty(key) 
            ?: throw IllegalArgumentException("Property $key not found in configuration")
    }
    
    /**
     * Get the bot token from configuration.
     */
    fun getBotToken(): String {
        return getProperty("bot.token")
    }
    
    /**
     * Get the command prefix from configuration.
     */
    fun getCommandPrefix(): String {
        return getProperty("bot.prefix")
    }
}