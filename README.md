<div align="center">

# 🌀 Speedrunner Swap + Task Master 🌀

### Five Epic Game Modes in One Plugin

</div>

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-28A745?style=for-the-badge&logo=minecraft)
![API](https://img.shields.io/badge/API-Paper-2875D7?style=for-the-badge)
![Game Modes](https://img.shields.io/badge/Game_Modes-5_in_1-8A2BE2?style=for-the-badge)
![Task Master](https://img.shields.io/badge/Task_Master-BETA-FF6B35?style=for-the-badge)
![Tasks](https://img.shields.io/badge/100%2B_Tasks-Included-FFC107?style=for-the-badge)

</div>

> **Experience Dream's legendary challenges in one powerful plugin! Play the classic Speedrunners vs Hunters, the cooperative Multi-Runner Control Swap, the shared-body Task Master sabotage mode, Task Master Duo, or the no-swap Task Race mode. Dream mode supports multiple independent shared hunter bodies.**

Version **4.3.6** targets **Paper 1.21.11 and Java 21**. This release does not target Minecraft 26.x or claim compatibility with older server APIs. [Paper's Java requirements](https://docs.papermc.io/paper/getting-started/) and the [1.21.11 API](https://jd.papermc.io/paper/1.21.11/) are the compatibility references.

---

<div align="center">

## 🎬 Inspired by Dream's Original Videos

<table>
  <tr>
    <td align="center">
      <strong>🏹 Speedrunners vs Hunters</strong><br>
      <a href="https://www.youtube.com/watch?v=Zj3G5hN-EBQ"><img src="https://img.youtube.com/vi/Zj3G5hN-EBQ/0.jpg" width="300"></a>
    </td>
    <td align="center">
      <strong>🎛️ Multi-Runner Control</strong><br>
      <a href="https://www.youtube.com/watch?v=GwrAvYlT7xg"><img src="https://img.youtube.com/vi/GwrAvYlT7xg/0.jpg" width="300"></a>
    </td>
    <td align="center">
      <strong>🎯 Task Master (NEW!)</strong><br>
      <a href="https://www.youtube.com/watch?v=gTgrHPax0hk"><img src="https://img.youtube.com/vi/gTgrHPax0hk/0.jpg" width="300"></a>
    </td>
  </tr>
</table>
</div>

---

## 🎯 NEW: Task Master Mode (BETA)

<div align="center">

### The Ultimate Mind Game - As Seen in Dream's Latest Video!

**One body. Multiple players. Secret tasks. Pure chaos.**

</div>

In Task Master mode, players take turns controlling a single character while racing to complete their secret objectives. Every minute, control swaps to the next player. Will you help others to hide your true goal? Will you sabotage their progress? The choice is yours, but only the first to complete their task wins!

## 🏁 NEW: Task Race Mode

Task Race uses the same secret task pool, but removes the shared-body swap loop entirely. Two or more runners can play at the same time on their own characters, and the first player to finish their task wins instantly.

## ⚔️ NEW: Task Master Duo

Task Master Duo keeps the shared-body chaos, but splits the round into two active bodies instead of one. One group shares body A, another group shares body B, and both bodies rotate on the same interval while trying to finish their secret tasks first. That means you still get the sabotage, deception, and swap pressure from classic Task Master, but now another shared body is in the world interfering with you at the same time.

## 🏹 Shared Hunter Groups

Dream mode can run a permanent runner against two or more shared hunter bodies. Each hunter group has its own controller queue, state, respawn point, and timer. Only one member of each group plays at a time; waiting members use the configured freeze mode. Inventory, armor, offhand, selected slot, location, health, hunger, XP, effects, and motion pass to the next controller **within that group**, never to another hunter group.

Example: one runner and four hunters split into two independent bodies (replace names with online players):

```text
/swap mode dream
/swap setrunners Runner
/swap sethunters Alice Bob Charlie Dave
/swap huntergroups set A Alice Bob
/swap huntergroups set B Charlie Dave
/swap huntergroups interval A 60
/swap huntergroups interval B 60
/swap start
```

Both groups can use the same interval or different intervals. A group may contain one or more players. Assigning a group enables shared hunter control. Use `/swap huntergroups gui` or **Settings → Hunter Groups** to cycle player assignments, edit timers, and remove groups; `/swap huntergroups list` shows the saved configuration. Right-click a player to unassign; shift-click the remove button to delete a group. Group names accept 1–24 letters, numbers, hyphens, or underscores.

Every selected hunter must belong to exactly one nonempty group. Missing/offline members, duplicate memberships, and incomplete assignments prevent starting. Stop the round before editing teams/groups. Definitions persist in `swap.shared_hunter_control.groups`; deleting all groups restores the original one-shared-hunter-body behavior. Disable shared hunter control for classic independent hunters. Task Master Duo keeps its existing two-body behavior and does not use these groups.

With `swap.pause_on_disconnect: true`, losing an active controller pauses all timers until the missing controllers return. With it disabled, the affected body hands off to its next available member; if none are online it waits with saved state. Death defers the handoff until respawn, so death drops are not duplicated. A cancelled teleport leaves the old controller in charge. Pause preserves each group's remaining timer. Round stop cancels the timers and uses the existing participant-restoration/global-spawn policy.

### Build and code-level verification

Run `mvn clean verify` with JDK 21. The plugin JAR is `target/speedrunnerswap-4.3.6.jar`. JUnit/MockBukkit tests cover real plugin loading, timers, state transfer, configuration, commands, menu events, disconnects, deaths, cleanup, and compatibility paths. Test libraries are not included in the plugin JAR. These are automated code-level checks, **not a Minecraft client/playtest or verification of third-party plugin interactions**.

### 🎲 100+ Unique Tasks Included!

<details>
<summary><strong>Click to see all task categories (100+ tasks!)</strong></summary>

#### 🔥 Special Multi-Step Tasks (Like Dream's Video)
- Fall from surface to bedrock and die from fall damage
- Kill an iron golem in the Nether with a bed explosion
- Kill one of every colored sheep with an iron shovel
- Sleep in a Nether fortress bed
- Die holding exactly 10 diamonds
- Get a full stack of rotten flesh
- Kill a mob by dropping an anvil on it
- Take 100 damage without dying
- Name 5 different mobs
- Collect all 9 wood types

#### ⛏️ Underground & Mining Challenges
- Mine 1000 blocks total
- Find 12 diamonds
- Create 100-block strip mine at Y=11
- Die in lava below Y=5
- Place torch on bedrock
- Fill chest with all ore types
- Explode 50 TNT
- Dig through bedrock to void
- Build 3x3x3 obsidian room
- Find and break mob spawner

#### ⚔️ Combat & Mob Challenges
- Kill 50 hostile mobs
- Kill 10 zombies with golden sword
- Kill 5 creepers without explosions
- Get killed by baby zombie
- Kill skeleton with its own arrow
- Survive 5 creeper explosions
- Kill enderman with water
- Tame wolf and have it kill 10 sheep
- Kill witch with splash potions
- Find and kill spider jockey

#### 🔥 Nether Challenges
- Bridge across lava lake
- Collect 16 glowstone dust
- Kill ghast by reflecting fireball
- Loot Nether fortress chest
- Collect 10 magma cream
- Trade 16 gold with piglins
- Kill 20 piglins
- Ride strider across lava ocean
- Brew fire resistance potion
- Get killed by wither skeleton

#### 🏗️ Crafting & Building
- Craft full diamond armor set
- Build 50-block high tower
- Build automatic redstone farm
- Craft and place 10 paintings
- Build 2 nether portals in overworld
- Enchanting table with 15 bookshelves
- Create 5 infinite water sources
- Build house with 5 rooms
- Craft 100 items total
- Create 3x3 map wall

#### 🌾 Food & Farming
- Breed 20 animals
- Cook 64 pieces of meat
- Harvest 100 wheat
- Create bee farm with 3 hives
- Tame 10 wolves
- Collect every flower type
- Craft and place cake
- Eat 25 different foods
- Get poisoned 5 times
- Achieve max saturation with golden carrots

#### 🚂 Transportation Challenges
- Travel 1000 blocks from spawn
- Ride minecart 500 blocks
- Fly 1000 blocks with elytra
- Cross ocean by boat
- Ride pig 100 blocks
- Build 50-block ice road
- Jump 5 blocks high on horse
- Swim 500 blocks
- Create 30-block bubble elevator
- Travel 100 blocks with ender pearls

#### 📦 Collection Challenges
- Collect 64 bones
- Collect 32 ender pearls
- Find a music disc
- Reach level 30 experience
- Fill entire inventory with unique items
- Collect 64 string
- Collect 32 gunpowder
- Collect every dye color
- Find 2 saddles
- Find Totem of Undying

#### 💎 Trading & Villager Challenges
- Trade with 5 different villagers
- Max out a villager's trade
- Cure zombie villager
- Build and spawn iron golem
- Defeat a raid
- Collect 64 emeralds
- Build villager breeder
- Transport villager 500 blocks
- Get Hero of the Village
- Trade for enchanted book

#### 🎪 Unique & Special Challenges
- Sleep in bed 10 times
- Die 5 different ways
- Watch sunset from Y=100
- Create 10 snow golems
- Throw diamond in lava
- Drown with Respiration III
- Kill yourself with own TNT
- Travel 10000 blocks one direction
- Take 50 hearts damage without dying
- Place 1000 blocks

</details>

### ✨ Task Master Features

| Feature | Description |
|:--|:--|
| **🎲 100+ Pre-Built Tasks** | Extensive task library inspired by Dream's challenges |
| **➕ Custom Task Creator** | Add your own tasks via GUI or config file |
| **🕵️ Secret Objectives** | Tasks hidden from other players to maintain mystery |
| **📊 Task Balancing** | All tasks designed for similar completion times |
| **🎚 Difficulty Buckets** | Pick Easy, Medium, or Hard pools for assignments |
| **↩️ First-Turn Rerolls** | Players can spend a configurable reroll before start or during their opening turn |
| **🎭 Strategic Deception** | Hide your true objective while sabotaging others |
| **🏆 Instant Victory** | First to complete their task wins immediately |

---

## 🎮 All Game Modes

<table>
  <tr>
    <td width="25%" valign="top">
      <h3>🏹 Speedrunners vs Hunters</h3>
      <p>The classic chase. Speedrunners must defeat the Ender Dragon while being hunted. Control swaps between runners at intervals.</p>
      <ul>
        <li>🔄 Periodic control swaps</li>
        <li>🧭 Compass tracking</li>
        <li>⚔️ Intense PvP</li>
        <li>🎯 Goal: Defeat Ender Dragon</li>
      </ul>
    </td>
    <td width="25%" valign="top">
      <h3>🎛️ Multi-Runner Control</h3>
      <p>Pure cooperation. Multiple players share control of one character in a queue system to beat the game together.</p>
      <ul>
        <li>🔄 Queue-based swaps</li>
        <li>🤝 Shared body & inventory</li>
        <li>👥 No PvP, pure co-op</li>
        <li>🎯 Goal: Beat the game as one</li>
      </ul>
    </td>
    <td width="25%" valign="top">
      <h3>🎯 Task Master <span style="color: #FF6B35;">(BETA)</span></h3>
      <p>Strategic competition. Complete your secret task while sharing control and preventing others from completing theirs.</p>
      <ul>
        <li>🎲 100+ included tasks</li>
        <li>➕ Custom task support</li>
        <li>🕵️ Hidden objectives</li>
        <li>🎯 Goal: Complete your task first</li>
      </ul>
    </td>
    <td width="25%" valign="top">
      <h3>🏁 Task Race</h3>
      <p>Parallel competition. Every runner keeps their own body, inventory, and progress while racing to finish a hidden task first.</p>
      <ul>
        <li>👥 2+ runners at once</li>
        <li>🚫 No periodic swaps</li>
        <li>🕵️ Hidden objectives</li>
        <li>🎯 Goal: Finish before everyone else</li>
      </ul>
    </td>
  </tr>
</table>

---

## ✨ Universal Features

| Feature | Description |
|:--|:--|
| **🖥️ Full GUI Control** | Manage teams, settings, task pools, spawn, and cosmetics without touching files |
| **🔄 Customizable Swaps** | Set intervals, randomization, grace periods, and jitter |
| **🏹 Shared Hunter Body** | Optional Dream-mode hunter queue so runners and hunters can each share a separate body |
| **🛡️ Safe Swap System** | Prevents swapping into dangerous situations |
| **🎤 Voice Chat Integration** | Auto-mute inactive players via Simple Voice Chat support |
| **📚 Task Pool Manager** | Enable/disable every objective, adjust difficulty filters, and reload `tasks.yml` live |

---

## 🚀 Quick Start

<div align="center">

| Step | Action | Details |
|:---:|:---|:---|
| **1** | 📥 **Download** | Get `SpeedrunnerSwap-*.jar` from releases |
| **2** | 📁 **Install** | Place in server's `plugins/` directory |
| **3** | 🔄 **Restart** | Start server to generate configs |
| **4** | ⚙️ **Configure** | Run `/swap gui` → Mode Selector (shift-click a mode to save it as the server default) |
| **5** | 🎮 **Play!** | Use `/swap gui` to start! |

</div>

---

## 📝 Commands

| Command | Description |
|:--|:--|
| `/swap gui` | Opens the full management hub (mode selector, team setup, settings, task pool, stats, kits, etc.). |
| `/swap start` · `/swap stop` · `/swap pause` · `/swap resume` · `/swap status` | Runtime controls for starting, stopping, pausing and inspecting the current match. |
| `/swap mode <dream|sapnap|task|taskrace>` | Switches the active gameplay mode. Automatically blocks switching mid-match unless `--force` is provided. |
| `/swap setrunners <players…>` · `/swap sethunters <players…>` · `/swap clearteams` | Quick team assignment helpers (mirrors the GUI team selector). |
| `/swap interval <seconds>` · `/swap randomize <on|off>` | Fast tweaks for the base swap interval and randomisation without opening the menu. |
| `/swap tasks list` | Prints all registered Task Master objectives with their enabled state and difficulty. |
| `/swap tasks enable|disable <id>` | Toggle individual tasks from chat (same functionality is available in the GUI Task Pool). |
| `/swap tasks difficulty <easy|medium|hard>` | Chooses the Task Master difficulty filter. |
| `/swap tasks reroll` | Assigns fresh secret tasks to the currently selected runners (only before the round starts). |
| `/swap tasks endwhenoneleft <on|off|toggle>` | Controls the “end when one runner remains” rule. |
| `/swap tasks reload` | Reloads `tasks.yml` without restarting the server. |
| `/swap complete [confirm]` | Shows your current secret task or, with `confirm`, manually completes it (and ends the game). |
| `/swap complete reroll confirm` | Spends your configurable one-time task reroll when the current round rules allow it. |
| `/swap creator` · `/swap help` | Plugin credits and in-game help. |

Permissions:
- `speedrunnerswap.command` lets trusted players open the GUI, start/stop rounds, and manage teams.
- `speedrunnerswap.admin` is required for mode changes, interval/randomize tweaks, task management, reloads, and `/swap clearteams`.
- `/swap complete` and `/swap complete reroll confirm` are available to runners who have been assigned a Task Master objective.

---

## ⚙️ Configuration & GUI Coverage

The plugin ships with a comprehensive `config.yml`, but every option can be adjusted from the in-game GUI. Here’s how the configuration maps to the menu structure:

| GUI Section | Key Config Areas | Highlights |
|:--|:--|:--|
| **⚙️ Swap & Timing** | `swap.*` | Interval, randomisation, experimental limits, jitter, grace period, hunter swap, hot potato, preserve runner progress. |
| **🛡️ Safety & Freeze** | `safe_swap.*`, `freeze_mode`, `freeze_mechanic.*`, `cancel.*`, `single_player_sleep`, `spawn.*`, `limbo.*` | Safe-swap radii, freeze mechanics, spawn handling, limbo/spawn location setters. |
| **🎯 Hunter Tools** | `tracker.*` | Compass enable/disable, update ticks, portal retry attempts/delay, compass jamming duration & distance. |
| **🧪 Power-ups** | `power_ups.*` | Toggle system, choose positive/negative effects, duration & level ranges. |
| **🌍 World Border** | `world_border.*` | Enable shrink, starting/ending size, shrink duration, warning distance/interval. |
| **🏹 Bounty System** | `bounty.*` | Master toggle, cooldown, glow length, reward durations, manual assign/clear. |
| **🛡 Last Stand & Sudden Death** | `last_stand.*`, `sudden_death.*` | Configure runner clutch buffs and sudden-death arena, timers, and effects. |
| **📊 Statistics & UI** | `stats.*`, `ui.update_ticks.*`, `timer_visibility.*` | Enable tracking, broadcast cadence, timer visibility, action bar/title update rates. |
| **✨ Particle Trail** | `particle_trail.*` | Toggle runner particle trail, tick interval, particle id, and RGB colour (new GUI controls). |
| **🎙 Voice Chat** | `voice_chat.*` | Simple Voice Chat integration toggles. |
| **🎒 Kits** | `kits.*` & `kits.yml` | Enable runner/hunter kits and quick testing buttons. |
| **🎯 Task Master** | `task_manager.*` | Pause behaviour, reconnection grace, max duration, include defaults, difficulty filter, player reroll rules, per-task enable/disable, custom task creator, task pool management, assignments viewer. |

> 📝 **Tip:** Use `/swap gui` (or the `/swap` hotkey in the tab completions) to reach any of these menus instantly—every slider, toggle, and button writes back to `config.yml` (or `tasks.yml` / `kits.yml`) for persistence.

---

## 🎨 Creating & Managing Custom Tasks

### GUI Workflow
1. Open `/swap gui` → **Task Master**.
2. **Task Settings** now exposes the round difficulty plus player-reroll rules.
3. **Custom Tasks** lets you add new objectives (ID + description) and remove existing ones.
4. **Task Pool** provides page-based toggles to enable/disable any built-in or custom task, adjust the difficulty filter, and reload `tasks.yml` live.

### Command Workflow
- `/swap tasks list` – review all definitions.
- `/swap tasks enable|disable <id>` – quick toggles from console or chat.
- `/swap tasks difficulty <easy|medium|hard>` – change the pool filter.
- `/swap tasks reroll` – reassign secret tasks prior to a round.
- `/swap complete reroll confirm` – let a player spend their opening-round reroll.
- `/swap tasks reload` – re-read `tasks.yml` after editing.

### Design Tips
- Aim for goals that take 10–30 minutes so races stay competitive.
- Use exact numbers (e.g. *collect 32 ender pearls*) to avoid ambiguity.
- Mix tasks that encourage sabotage with those that reward cooperation.
- Keep descriptions concise, the GUI shows the entire text to players.

## 🌟 Community Creator Showcase

### 🎥 Videos Made With Speedrunner Swap + Task Master

> **A huge thank you to every creator who has featured, played, or made a video using the plugin! ❤️ Check out their videos below and show them some support!**

|                                                                                                                             |                                                                                                                              |
| :-------------------------------------------------------------------------------------------------------------------------: | :--------------------------------------------------------------------------------------------------------------------------: |
| [![Creator Video](https://img.youtube.com/vi/ZIazl5qIs0k/hqdefault.jpg)](https://www.youtube.com/watch?v=ZIazl5qIs0k&t=38s) | [![Creator Video](https://img.youtube.com/vi/7jdgkXWRtfY/hqdefault.jpg)](https://www.youtube.com/watch?v=7jdgkXWRtfY&t=230s) |
|                         **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=ZIazl5qIs0k&t=38s)**                        |                         **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=7jdgkXWRtfY&t=230s)**                        |
|    [![Creator Video](https://img.youtube.com/vi/8yuIJDaC_Po/hqdefault.jpg)](https://www.youtube.com/watch?v=8yuIJDaC_Po)    |     [![Creator Video](https://img.youtube.com/vi/jhhVrgFN1G4/hqdefault.jpg)](https://www.youtube.com/watch?v=jhhVrgFN1G4)    |
|                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=8yuIJDaC_Po)**                           |                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=jhhVrgFN1G4)**                            |
|    [![Creator Video](https://img.youtube.com/vi/j8lmICOn9w8/hqdefault.jpg)](https://www.youtube.com/watch?v=j8lmICOn9w8)    |     [![Creator Video](https://img.youtube.com/vi/znRJMLGnhFg/hqdefault.jpg)](https://www.youtube.com/watch?v=znRJMLGnhFg)    |
|                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=j8lmICOn9w8)**                           |                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=znRJMLGnhFg)**                            |
|    [![Creator Video](https://img.youtube.com/vi/Pqh0d3lz6Fc/hqdefault.jpg)](https://www.youtube.com/watch?v=Pqh0d3lz6Fc)    |     [![Creator Video](https://img.youtube.com/vi/sCTRVGCFDUI/hqdefault.jpg)](https://www.youtube.com/watch?v=sCTRVGCFDUI)    |
|                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=Pqh0d3lz6Fc)**                           |                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=sCTRVGCFDUI)**                            |
|    [![Creator Video](https://img.youtube.com/vi/C9iI_FYG1YE/hqdefault.jpg)](https://www.youtube.com/watch?v=C9iI_FYG1YE)    |     [![Creator Video](https://img.youtube.com/vi/ODE0nb0c24s/hqdefault.jpg)](https://www.youtube.com/watch?v=ODE0nb0c24s)    |
|                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=C9iI_FYG1YE)**                           |                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=ODE0nb0c24s)**                            |
|    [![Creator Video](https://img.youtube.com/vi/x8asWE2tq3M/hqdefault.jpg)](https://www.youtube.com/watch?v=x8asWE2tq3M)    |     [![Creator Video](https://img.youtube.com/vi/rzVqw495QFs/hqdefault.jpg)](https://www.youtube.com/watch?v=rzVqw495QFs)    |
|                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=x8asWE2tq3M)**                           |                            **[▶️ Watch on YouTube](https://www.youtube.com/watch?v=rzVqw495QFs)**                            |

### 📺 Featured on Bilibili

**[▶️ Watch on Bilibili](https://www.bilibili.com/video/BV1KRuR64Ej4)**

> 🎬 **Made a video using the plugin?** Share it in the Discord, you might be featured here!

---

<div align="center">

## 🙌 Credits & Support

**Inspired by Dream, Sapnap & GeorgeNotFound's legendary videos**

**Developed by muj3b**

[![Donate](https://img.shields.io/badge/💖_Donate-Support_Development-ff69b4?style=for-the-badge)](https://ko-fi.com/muj4b)

---

### 🎉 Experience Dream's Challenges Today! 🎉

**100+ tasks included • Custom task support • Four game modes • Endless possibilities**

*Task Master mode is in BETA - Report issues and suggest new tasks on our Discord!*

</div>
