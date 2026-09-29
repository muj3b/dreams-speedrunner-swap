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
- **Independent Hunter Groups**: One permanent runner can face separate hunter bodies, each rotating its own members and state

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
- Voice chat settings are placeholders; Simple Voice Chat auto-muting is not implemented

## Requirements

- Java 21 for Paper 1.21.x; Java 25 for Paper 26.x
- Version 4.3.7 targets published Paper 1.21.x through 26.3 in one JAR
- Code-only verification: 43 tests on matching 1.21.11, 26.1.2, and 26.2 mocks; packaged-JAR linkage checks on 15 APIs. Paper 26.3 build 135 is beta and API-only verified. No Minecraft playtest or third-party plugin verification is claimed. See [compatibility details](https://github.com/muj3b/dreams-speedrunner-swap/blob/main/COMPATIBILITY.md).

## Links

- [GitHub Repository](https://github.com/muj3b/dreams-speedrunner-swap)
- [Issue Tracker](https://github.com/muj3b/dreams-speedrunner-swap/issues)
