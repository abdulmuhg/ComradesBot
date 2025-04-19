# Discord Kotlin Bot

A comprehensive Discord bot built with Kotlin and JDA (Java Discord API), featuring both traditional prefix commands and modern slash commands, with extensive moderation and utility features.

## Features

### Command Systems
- Traditional command system with prefix customization
- Modern slash commands with parameter validation

### Utility Commands
- `/ping` - Check bot response time
- `/echo` - Have the bot repeat a message
- `/userinfo` - Display information about a user
- `/serverinfo` - Display detailed information about the server

### Interactive Features
- `/poll` - Create interactive polls with up to 5 options and automatic results tracking
- `/reactionrole` - Set up reaction roles that automatically assign server roles when users react to a message

### Moderation Tools
- `/mod kick` - Kick a member from the server
- `/mod ban` - Ban a member from the server
- `/mod timeout` - Temporarily mute a member (Discord timeout)
- `/mod removetimeout` - Remove a timeout from a member
- `/mod purge` - Delete multiple messages at once, with optional user filtering

### Event Handling
- Welcome messages for new members joining the server
- Farewell messages when members leave
- Reaction role assignment and removal

## Setup

1. Clone this repository
2. Copy `src/main/resources/config.properties.template` to `src/main/resources/config.properties`
3. Get a bot token from the [Discord Developer Portal](https://discord.com/developers/applications)
4. Add your bot token to the `config.properties` file
5. Build the project using Gradle: `./gradlew build`
6. Run the bot: `./gradlew run`

### Important Discord Bot Settings

When setting up your bot in the Discord Developer Portal:

1. Go to the "Bot" tab
2. Enable the following Privileged Gateway Intents:
   - SERVER MEMBERS INTENT
   - MESSAGE CONTENT INTENT
   - PRESENCE INTENT
3. Go to OAuth2 > URL Generator
4. Select scopes:
   - bot
   - applications.commands
5. Select appropriate bot permissions:
   - Send Messages
   - Read Message History
   - Use Slash Commands
   - Add Reactions
   - Manage Roles (for reaction roles)
   - Kick Members (for moderation)
   - Ban Members (for moderation)
   - Moderate Members (for timeouts)
   - Manage Messages (for purge command)

## Command Registration

### Global vs. Guild Commands

You can register slash commands in two ways:
- **Global registration**: Commands available in all servers (may take up to an hour to update)
- **Guild registration**: Commands available immediately but only in one server

To register commands for a specific guild (for faster development):
```
./gradlew run --args="YOUR_BOT_TOKEN YOUR_GUILD_ID"
```

## Architecture

The bot is designed with a modular, object-oriented architecture that makes it easy to add new features:

- **Command System**: Both traditional and slash commands follow a similar interface pattern, making it easy to add new commands.
- **Event Handlers**: Separate classes handle specific event types (member join/leave, reactions, etc.).
- **Managers**: Specialized manager classes handle specific features (PollManager, ReactionRoleManager).

## Adding New Commands

### Traditional Commands

To add a new traditional command:

1. Create a new class that implements the `Command` interface
2. Implement the required properties and methods
3. Register the command in the `DiscordBot.registerCommands()` method

Example:

```kotlin
class MyCommand : Command {
    override val name = "mycommand"
    override val description = "Description of my command"
    override val usage = "mycommand [args]"
    
    override suspend fun execute(args: List<String>, event: MessageReceivedEvent) {
        // Command implementation here
        event.channel.sendMessage("My command was executed!").queue()
    }
}
```

### Slash Commands

To add a new slash command:

1. Create a new class that implements the `SlashCommand` interface
2. Implement the required properties and methods
3. Register the command in the `DiscordBot.registerSlashCommands()` method

Example:

```kotlin
class MySlashCommand : SlashCommand {
    override val name = "myslash"
    override val description = "Description of my slash command"
    
    // Optional: Add command parameters
    override val options = listOf(
        OptionData(OptionType.STRING, "param", "A parameter for the command", true)
    )
    
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val param = event.getOption("param")?.asString
        event.reply("You provided: $param").queue()
    }
}
```

## Adding Event Listeners

The bot includes an `EventHandler` class that listens for various Discord events. You can extend this to handle additional events:

1. Open `EventListeners.kt`
2. Add new methods for the events you want to handle
3. The event listener is automatically registered in the `DiscordBot.initialize()` method

Example of adding a reaction event handler:

```kotlin
/**
 * Called when a reaction is added to a message.
 */
override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
    // Your code here to handle the reaction
}
```

## Future Enhancements

Potential future features for this bot:

- Music playback capabilities
- Custom server settings with database integration
- Advanced auto-moderation features
- Level/XP system for server engagement
- Scheduled announcements and reminders
- Voice channel management
- Temporary voice channels
- Translation services

## License

MIT
# Discord Kotlin Bot

A modern Discord bot built with Kotlin and JDA (Java Discord API), featuring both traditional prefix commands and slash commands.

## Features

- Traditional command system with prefix customization
- Modern slash commands with parameter validation
- Event listeners for member join/leave events
- Basic commands:
  - Traditional: ping, help, echo
  - Slash commands: /ping, /echo, /userinfo
- Easy to extend with new commands and event listeners

## Setup

1. Clone this repository
2. Copy `src/main/resources/config.properties.template` to `src/main/resources/config.properties`
3. Get a bot token from the [Discord Developer Portal](https://discord.com/developers/applications)
4. Add your bot token to the `config.properties` file
5. Build the project using Gradle: `./gradlew build`
6. Run the bot: `./gradlew run`

### Important Discord Bot Settings

When setting up your bot in the Discord Developer Portal:

1. Go to the "Bot" tab
2. Enable the following Privileged Gateway Intents:
   - SERVER MEMBERS INTENT
   - MESSAGE CONTENT INTENT
   - PRESENCE INTENT
3. Go to OAuth2 > URL Generator
4. Select scopes:
   - bot
   - applications.commands
5. Select appropriate bot permissions (at minimum: Send Messages, Read Message History, and Use Slash Commands)

## Command Registration

### Global vs. Guild Commands

You can register slash commands in two ways:
- **Global registration**: Commands available in all servers (may take up to an hour to update)
- **Guild registration**: Commands available immediately but only in one server

To register commands for a specific guild (for faster development):
```
./gradlew run --args="YOUR_BOT_TOKEN YOUR_GUILD_ID"
```

## Adding New Commands

### Traditional Commands

To add a new traditional command:

1. Create a new class that implements the `Command` interface
2. Implement the required properties and methods
3. Register the command in the `DiscordBot.registerCommands()` method

Example:

```kotlin
class MyCommand : Comman
