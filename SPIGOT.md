# SpeedrunnerSwap — Dream-style Swap VS Hunters (Paper/Spigot 1.20.6)

## Description

SpeedrunnerSwap recreates the exciting "Speedrunner Swap VS Hunters" challenge from Dream's videos. Two or more speedrunners share the same state and swap control on a timer, while hunters try to stop them from beating the game.

This plugin provides a complete, ready-to-use implementation with intuitive GUIs, comprehensive commands, and highly configurable settings to customize the experience for your server.

## Features

### Core Mechanics
- **Swap System**: Runners automatically swap control on a timer
- **State Syncing**: Complete synchronization of inventory, health, location, and more
- **Blackout UI**: Inactive runners get blindness and can't move or interact
- **Action Bar Timer**: Shows countdown to next swap and runner status
- **Safe Swaps**: Optional protection from dangerous blocks like void/lava

### Team Management
- **Multi-Runner Support**: More than two runners can rotate turns
- **Hunter Tracking**: Compass always points to active runner
- **Team Assignment**: Easy GUI for assigning players to teams

### Configuration
- **Flexible Swap Timing**: Fixed interval or random with Gaussian jitter
- **Freeze Options**: Choose between potion effects or spectator mode
- **Grace Period**: Brief invulnerability after swaps
- **Pause on Disconnect**: Game auto-pauses if active runner leaves

### User Interface
- **Intuitive GUIs**: Team selector, settings menu, main menu
- **Command System**: Start, stop, pause, resume, and more
- **Chat Isolation**: Inactive runners can't communicate
- **Voice Chat Integration**: Works with Simple Voice Chat plugin

## Commands

- `/swap` - Opens the main GUI
- `/swap start` - Starts the game
- `/swap stop` - Stops the game
- `/swap pause` - Pauses the game
- `/swap resume` - Resumes the game
- `/swap status` - Shows the current game status
- `/swap setrunners <names...>` - Sets the runners
- `/swap sethunters <names...>` - Sets the hunters
- `/swap reload` - Reloads the configuration
- `/swap gui` - Opens the main GUI

## Permissions

- `speedrunnerswap.command` - Allows use of basic commands
- `speedrunnerswap.admin` - Allows use of administrative commands

## Installation

1. Download the plugin
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/SpeedrunnerSwap/config.yml`

## Requirements

- Java 17 or higher
- Paper/Spigot 1.21.8

## Support

If you encounter any issues or have suggestions, please report them on our GitHub repository.

Enjoy the challenge!