# 🌀 Speedrunner Swap — Dream’s Newest Challenge

**Speedrunner Swap** is a Minecraft plugin that brings Dream’s latest “Speedrunner Swap” challenge—featured in his new video—straight to your server! Multiple speedrunners share the same player state and swap control on a configurable timer, while hunters try to stop them from beating the game.

This plugin is the first of its kind, packed with features and customization not found anywhere else: built-in GUI, randomized countdowns, safe swap options, and more.

---

## 📺 Watch the Original Challenge

[![Watch the original video](https://img.youtube.com/vi/Zj3G5hN-EBQ/0.jpg)](https://www.youtube.com/watch?v=Zj3G5hN-EBQ)

---

## 🚀 How It Works

- **Swap Mechanic**
  - Speedrunners swap control at configurable intervals (default: 60s)
  - Swaps can be fixed or randomized (min/max, Gaussian jitter)
  - Inactive runners are frozen (blindness/slowness effects or spectator mode)
- **Shared State**
  - Runners share inventory, health, position, XP, and effects
  - Teamwork and communication required
- **Hunters**
  - Track the active runner with a compass (periodic updates)
  - Coordinates shown in action bar
  - Use PvP to eliminate runners
- **Objective**
  - Runners must beat the Ender Dragon before hunters stop them

---

## ✨ Features

- **Customizable Swap System**: Fixed/random intervals, jitter, grace period, auto-pause
- **Safe Swap Mode**: Avoid dangerous locations (lava, fire, etc.)
- **Freeze Inactive Runners**: Effects or spectator options
- **Hunter Compass Tracking**: Periodic updates, coordinate display
- **GUI Menus**: Team selection, settings, game management
- **Broadcast Messages**: Swaps, game events, team changes
- **Simple Voice Chat Integration**: Optionally mute inactive runners
- **Customizable Timer Visibility**: Configure timer visibility for active runners, waiting runners, and hunters
- **Version Support**: Minecraft 1.21+, Bukkit/Spigot/Paper

---

## 🛠️ Installation

1. **Download** the latest `.jar` from Modrinth.
2. **Place** it in your server’s `plugins` folder.
3. **Restart** your server.
4. **Configure** settings in `config.yml` (teams, swap intervals, tracker options).

---

## 📝 Commands

| Command              | Description                     |
|----------------------|---------------------------------|
| `/swap start`        | Start a Speedrunner Swap game   |
| `/swap stop`         | End the current game            |
| `/swap pause`        | Pause the game                  |
| `/swap resume`       | Resume the game                 |
| `/swap status`       | Check game status               |
| `/swap setrunners`   | Set runner players              |
| `/swap sethunters`   | Set hunter players              |
| `/swap reload`       | Reload configuration            |
| `/swap gui`          | Open the management GUI         |

> Requires `speedrunnerswap.command` permission (default: op).

---

## 🙌 Credits

- **Inspired by:** Dream’s Speedrunner Swap Manhunt
- **Developed by:** muj3b

---

## ❤️ Support the Project

I put a ton of work into making Speedrunner Swap the best it can be. If you appreciate the plugin and want to help out, please consider [donating here](https://donate.stripe.com/8x29AT0H58K03judnR0Ba01). Your support helps keep the project updated and improving!

---

Enjoy!
