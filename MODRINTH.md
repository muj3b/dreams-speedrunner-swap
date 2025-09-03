# SpeedrunnerSwap

Dream-style "Speedrunner Swap" challenge: action-bar timer, blackout UI, chat isolation, multi-runner, jittered swaps, safe-swaps, GUIs, hunter tracker.

## Overview

SpeedrunnerSwap recreates the exciting "Speedrunner Swap VS Hunters" challenge from Dream's videos. Two or more speedrunners share the same state and swap control on a timer, while hunters try to stop them from beating the game.

## Key Features

- **Swap System**: Runners automatically swap control on a configurable timer
- **Blackout UI**: Inactive runners get blindness and can't move or interact
- **Action Bar Timer**: Shows countdown to next swap and runner status
- **Multi-Runner Support**: More than two runners can rotate turns
- **Hunter Tracking**: Compass always points to active runner
- **Safe Swaps**: Protection from dangerous blocks like void/lava
- **Intuitive GUIs**: Team selector, settings menu, main menu
- **Complete State Syncing**: Inventory, armor, health, effects, location, and more
- **Voice Chat Integration**: Works with Simple Voice Chat plugin

## Commands & Permissions

### Commands
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

### Permissions
- `speedrunnerswap.command` - Allows use of basic commands
- `speedrunnerswap.admin` - Allows use of administrative commands

## Configuration

The plugin is highly configurable through `config.yml`:

- Runners / hunters list
- Swap interval, randomization, jitter settings
- Safe swap settings
- Freeze mode (EFFECTS or SPECTATOR)
- Tracker settings
- GUI customization
- Broadcast options
- Voice chat integration

## Requirements

- Java 17 or higher
- Paper/Spigot 1.21+

## Links

- [GitHub Repository](https://github.com/muj3b/minecraft-speedrunner-swap-plugin)
- [Issue Tracker](https://github.com/muj3b/minecraft-speedrunner-swap-plugin/issues)
