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
