# 🌀 Speedrunner Swap — Dream's Newest Challenge

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-brightgreen?style=for-the-badge&logo=minecraft)
![Version](https://img.shields.io/badge/Version-Latest-blue?style=for-the-badge)
![Downloads](https://img.shields.io/badge/Downloads-🔥Hot-red?style=for-the-badge)

</div>

---

> **🎮 The Ultimate Speedrunner Swap Experience**  
> Bring Dream's latest "Speedrunner Swap" challenge straight to your server! Multiple speedrunners share the same player state and swap control on a configurable timer, while hunters try to stop them from beating the game.

<details>
<summary><strong>🚨 What Makes This Special?</strong></summary>

This plugin is the **first of its kind**, packed with features and customization not found anywhere else: built-in GUI, randomized countdowns, safe swap options, and more. **Plus, I've added 10 brand new features that weren't in the original plugin from the video** to make the gameplay even more exciting and unpredictable!

</details>

---

## 📺 Watch the Original Challenge

<div align="center">

[![Watch the original video](https://img.youtube.com/vi/Zj3G5hN-EBQ/0.jpg)](https://www.youtube.com/watch?v=Zj3G5hN-EBQ)

*Click to watch Dream's original Speedrunner Swap video*

</div>

---

## 🚀 How It Works

<table>
<tr>
<td width="50%">

### 🔄 **Swap Mechanic**
- ⏰ Speedrunners swap control at configurable intervals (default: 60s)
- 🎲 Swaps can be fixed or randomized (min/max, Gaussian jitter)
- 🥶 Inactive runners are frozen (blindness/slowness effects or spectator mode)

</td>
<td width="50%">

### 🤝 **Shared State**
- 🎒 Runners share inventory, health, position, XP, and effects
- 💬 Teamwork and communication required
- 🔗 Seamless state transitions between players

</td>
</tr>
<tr>
<td width="50%">

### 🏹 **Hunters**
- 🧭 Track the active runner with a compass (periodic updates)
- 📍 Coordinates shown in action bar
- ⚔️ Use PvP to eliminate runners

</td>
<td width="50%">

### 🎯 **Objective**
- 🐉 Runners must beat the Ender Dragon
- 🛡️ Hunters must stop them before they succeed
- 🏆 Ultimate test of skill and teamwork

</td>
</tr>
</table>

---

## ✨ Core Features

<div align="center">

| 🎛️ **System** | 📝 **Description** |
|:---|:---|
| **🔄 Customizable Swap System** | Fixed/random intervals, jitter, grace period, auto-pause |
| **🛡️ Safe Swap Mode** | Avoid dangerous locations (lava, fire, etc.) |
| **❄️ Freeze Inactive Runners** | Effects or spectator options |
| **🧭 Hunter Compass Tracking** | Periodic updates, coordinate display |
| **🖥️ GUI Menus** | Team selection, settings, game management |
| **📢 Broadcast Messages** | Swaps, game events, team changes |
| **🎤 Simple Voice Chat Integration** | Optionally mute inactive runners |
| **👁️ Customizable Timer Visibility** | Configure timer visibility for all player types |
| **⚡ Version Support** | Minecraft 1.21+, Bukkit/Spigot/Paper |

</div>

---

## 🎯 Exclusive New Features 
### *10 Additions Not in the Original!*

<details>
<summary><strong>🔥 Click to Reveal All New Features</strong></summary>

<br>

> **🌟 These features are completely original and add massive depth to the gameplay!**

<table>
<tr>
<td>

**1. 🔄 Hunter Swapping**  
Just like the runners, hunters will also swap control at configurable intervals, adding a new layer of unpredictability

**2. ✨ Swap Power-Ups**  
The newly-swapped active runner receives a random positive or negative potion effect for a short duration

**3. 💪 "Last Stand" Mode**  
The final remaining runner receives a temporary strength and speed boost to give them a fighting chance

**4. 🎒 Custom Kits**  
Define custom starting kits for both runners and hunters in the configuration file

**5. 🔥 "Hot Potato" Swap Mode**  
A new game mode where swaps are triggered by the active runner taking damage, not by a timer

</td>
<td>

**6. 📊 Advanced Stats**  
End-game summary displaying detailed statistics like "time as active runner" and "kills as hunter"

**7. 🌍 Shrinking World Border**  
The world border slowly shrinks over time, forcing players closer together for a more action-packed endgame

**8. 💰 Bounty System**  
A random runner is assigned as a "bounty" at game start—the hunter who eliminates them receives a special reward

**9. ⚡ Sudden Death Mode**  
If the game runs too long, "sudden death" mode activates, teleporting all players to the End for a final battle

**10. 🧭 Compass Jamming**  
After a swap, hunters' compasses are temporarily "jammed" and point in random directions, giving the new runner an escape window

</td>
</tr>
</table>

</details>

---

## 🛠️ Installation

<div align="center">

### Quick Setup Guide

</div>

```bash
# Step 1: Download
wget https://modrinth.com/plugin/speedrunner-swap

# Step 2: Install
cp speedrunner-swap.jar /server/plugins/

# Step 3: Restart & Configure
# Edit plugins/SpeedrunnerSwap/config.yml
```

| Step | Action | Details |
|:---:|:---|:---|
| **1** | 📥 **Download** | Get the latest `.jar` from Modrinth |
| **2** | 📁 **Place** | Put it in your server's `plugins` folder |
| **3** | 🔄 **Restart** | Restart your server |
| **4** | ⚙️ **Configure** | Edit settings in `config.yml` |

---

## 📝 Commands

<div align="center">

### 🎮 **Game Management**

</div>

<table align="center">
<tr>
<th width="30%">🔧 Command</th>
<th width="50%">📝 Description</th>
<th width="20%">🎯 Category</th>
</tr>
<tr>
<td><code>/swap start</code></td>
<td>Start a Speedrunner Swap game</td>
<td>🟢 Control</td>
</tr>
<tr>
<td><code>/swap stop</code></td>
<td>End the current game</td>
<td>🔴 Control</td>
</tr>
<tr>
<td><code>/swap pause</code></td>
<td>Pause the game</td>
<td>⏸️ Control</td>
</tr>
<tr>
<td><code>/swap resume</code></td>
<td>Resume the game</td>
<td>▶️ Control</td>
</tr>
<tr>
<td><code>/swap status</code></td>
<td>Check game status</td>
<td>📊 Info</td>
</tr>
<tr>
<td><code>/swap setrunners</code></td>
<td>Set runner players</td>
<td>👥 Setup</td>
</tr>
<tr>
<td><code>/swap sethunters</code></td>
<td>Set hunter players</td>
<td>🏹 Setup</td>
</tr>
<tr>
<td><code>/swap reload</code></td>
<td>Reload configuration</td>
<td>⚙️ Admin</td>
</tr>
<tr>
<td><code>/swap gui</code></td>
<td>Open the management GUI</td>
<td>🖥️ Interface</td>
</tr>
</table>

> **🔐 Permissions:** Requires `speedrunnerswap.command` permission (default: op)

---

## 🙌 Credits

<div align="center">

<table>
<tr>
<td align="center">

**🎬 Inspired by**  
[Dream's Speedrunner Swap Manhunt](https://www.youtube.com/watch?v=Zj3G5hN-EBQ)

</td>
<td align="center">

**👨‍💻 Developed by**  
**muj3b**

</td>
</tr>
</table>

</div>

---

## ❤️ Support the Project

<div align="center">

> **💝 Love the plugin? Show your support!**

I put a **ton of work** into making Speedrunner Swap the best it can be. If you appreciate the plugin and want to help out, please consider supporting the project!

[![Donate](https://img.shields.io/badge/💖_Donate-Support_Development-ff69b4?style=for-the-badge)](https://donate.stripe.com/8x29AT0H58K03judnR0Ba01)

*Your support helps keep the project updated and improving!*

</div>

---

<div align="center">

### 🎉 **Ready to Play?**

**Download now and experience the most advanced Speedrunner Swap plugin available!**

---

**Enjoy!** 🚀

</div>
