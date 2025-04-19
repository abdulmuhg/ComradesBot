# Discord Kotlin Bot

A simple Discord bot built with Kotlin and JDA (Java Discord API).

## Features

- Command system with prefix customization
- Basic commands: ping, help, echo
- Easy to extend with new commands

## Setup

1. Clone this repository
2. Copy `src/main/resources/config.properties.template` to `src/main/resources/config.properties`
3. Get a bot token from the [Discord Developer Portal](https://discord.com/developers/applications)
4. Add your bot token to the `config.properties` file
5. Build the project using Gradle: `./gradlew build`
6. Run the bot: `./gradlew run`

## Adding New Commands

To add a new command:

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

## License

MIT
