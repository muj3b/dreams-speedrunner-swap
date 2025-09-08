# 🌀 Speedrunner Swap — Ultimate Challenge Plugin

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8%2B-brightgreen?style=for-the-badge&logo=minecraft)
![API](https://img.shields.io/badge/API-Paper%2FSpigot_1.21.8-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Two_Game_Modes-9cf?style=for-the-badge)
![Downloads](https://img.shields.io/badge/Downloads-🔥Hot-red?style=for-the-badge)

</div>

---

> **🎮 Two Epic Game Modes in One Plugin!**  
> Experience both Dream's classic **Speedrunners vs Hunters** challenge AND the innovative **Multi-Runner Control Swap** mode. Choose your adventure!

---

## 📺 Watch the Originals!

<div align="center">

**🏹 Speedrunners vs Hunters Mode**
<iframe width="450" height="253" src="https://www.youtube-nocookie.com/embed/Zj3G5hN-EBQ" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

**🎛️ Multi-Runner Control Mode**
<iframe width="450" height="253" src="https://www.youtube-nocookie.com/embed/GwrAvYlT7xg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

</div>

---

## 🎮 Game Modes

### 🏹 **Speedrunners vs Hunters Mode**
Classic Dream-style gameplay with speedrunners swapping control while hunters pursue them.

**Core Mechanics:**
- 🔄 Speedrunners swap control on intervals
- 🧭 Hunters track with compasses  
- ⚔️ PvP elimination mechanics
- 🎯 Goal: Beat the Ender Dragon before getting caught

### 🎛️ **Multi-Runner Control Mode** 
Pure cooperation mode where multiple runners share one character with no hunters.

**Core Mechanics:**
- 🔄 Queue-based rotation system
- 👥 Any number of runners (2+)
- 🤝 Shared inventory, health, XP, effects
- 🎯 Goal: Beat the Ender Dragon together

---

## 🚀 How Both Modes Work

<div align="center">

<table>
<tr>
<td width="50%">

### 🔄 **Swap System**
- ⏰ Configurable intervals (default: 60s)
- 🎲 Fixed or randomized timing
- 🛡️ Safe swap locations
- 🕶️ Inactive players frozen/spectating

</td>
<td width="50%">

### 🤝 **Shared Elements**
- 🎒 Inventory, health, XP sync
- 📍 Position continuity  
- 💬 Teamwork coordination
- 🔗 Seamless transitions

</td>
</tr>
</table>

</div>

---

## ✨ Core Features

<div align="center">

| 🎛️ **Feature** | 📝 **Description** | 🏹 **Hunters** | 🎛️ **Control** |
|:---|:---|:---:|:---:|
| **🔄 Customizable Swaps** | Fixed/random intervals, jitter, grace period | ✅ | ✅ |
| **🛡️ Safe Swap Mode** | Avoid lava/fire/dangerous blocks | ✅ | ✅ |
| **🧭 Hunter Tracking** | Compass updates, coordinate display | ✅ | ❌ |
| **👥 Queue System** | Fair rotation for multiple runners | ❌ | ✅ |
| **🖥️ GUI Interface** | Team selection, settings management | ✅ | ✅ |
| **🎤 Voice Chat Support** | Mute inactive runners | ✅ | ✅ |
| **🏰 Cage System** | Robust cross-world containment | ❌ | ✅ |

</div>

---

## 🎯 Exclusive Hunter Mode Features
### *10 Additions Not in the Original!*

<details>
<summary><strong>🔥 Click to Reveal All New Features</strong></summary>

<div align="center">

<table>
<tr>
<td>

**1. 🔄 Hunter Swapping**  
Hunters can swap control too

**2. ✨ Swap Power-Ups**  
Random potion effects on swap

**3. 💪 "Last Stand" Mode**  
Final runner gets strength boost

**4. 🎒 Custom Kits**  
Define starting equipment

**5. 🔥 "Hot Potato" Mode**  
Damage-triggered swaps

</td>
<td>

**6. 📊 Advanced Stats**  
Detailed end-game statistics

**7. 🌍 Shrinking Border**  
World border forces action

**8. 💰 Bounty System**  
Special rewards for targets

**9. ⚡ Sudden Death**  
End teleport to final battle

**10. 🧭 Compass Jamming**  
Temporary hunter confusion

</td>
</tr>
</table>

</div>

</details>

---

## 🎛️ Multi-Runner Control Features

<div align="center">

| 🎛️ **Feature** | 📝 **Description** |
|:---|:---|
| **🔄 Queue Management** | Fair rotation system for any number of runners |
| **📊 Queue HUD** | Active sees countdown; others see queue position |
| **🌍 Cross-World Cages** | Robust cage system works in all dimensions |
| **⏲️ Smart Timing** | Grace periods, disconnect pausing, jitter options |
| **🎯 Victory Condition** | Dragon death triggers donation link broadcast |
| **🛠️ Freeze Modes** | EFFECTS, SPECTATOR, LIMBO, or CAGE options |

</div>

---

## ⚙️ Configuration

Edit `plugins/SpeedrunnerSwap/config.yml` after first run.

### **Game Mode Settings**
```yaml
game_mode: "HUNTERS"  # HUNTERS or CONTROL
```

### **Common Settings**
```yaml
swap:
  interval: 60
  randomize: false
  grace_period_ticks: 60
safe_swap:
  enabled: true
  scan_radius: 5
```

### **Hunter Mode Specific**
```yaml
hunters:
  tracking_enabled: true
  pvp_enabled: true
  compass_updates: true
```

### **Control Mode Specific**
```yaml
freeze_mode: "CAGE"  # EFFECTS, SPECTATOR, LIMBO, CAGE
cage:
  auto_rebuild: true
  chunk_preload: true
```

---

## 📝 Commands

<div align="center">

| Command | Description | Hunter Mode | Control Mode |
|:--|:--|:---:|:---:|
| `/swap start` | Start the game | ✅ | ✅ |
| `/swap stop` | End the current game | ✅ | ✅ |
| `/swap gui` | Open management interface | ✅ | ✅ |
| `/swap status` | Show game status | ✅ | ✅ |
| `/swap mode <hunters\|control>` | Switch game mode | ✅ | ✅ |
| `/swap setrunners <p1> [p2] ...` | Set runner team | ✅ | ✅ |
| `/swap sethunters <p1> [p2] ...` | Set hunter team | ✅ | ❌ |
| `/swap shuffle` | Shuffle queue order | ❌ | ✅ |

**Permissions:** `speedrunnerswap.command` (default: op), `speedrunnerswap.admin` (default: op)

</div>

---

## 🛠️ Installation

<div align="center">
  
| Step | Action | Details |
|:---:|:---|:---|
| **1** | 📥 **Download** | Get latest `speedrunnerswap-*.jar` |
| **2** | 📁 **Install** | Place in `plugins/` directory |
| **3** | 🔄 **Restart** | Restart your server |
| **4** | ⚙️ **Configure** | Choose mode in `config.yml` |
| **5** | 🎮 **Play** | Use `/swap gui` or commands to start |

</div>

**Requirements:** Paper 1.21.8+ recommended; Spigot 1.21.x supported with fallbacks.

---

## 🧱 Technical Notes

### **Hunter Mode**
- Compass tracking updates every 3 seconds
- PvP mechanics handle elimination
- Spectator mode for eliminated players
- GUI-based team management

### **Control Mode**  
- Cages work across all dimensions (Overworld/Nether/End)
- Chunk preloading prevents generation issues
- Safe Y calculation per world environment
- Automatic cage rebuilding on world changes

---

## 🙌 Credits & Support

<div align="center">

**🎬 Inspired by:** Dream's Speedrunner Swap & Sapnap's Multi-Runner videos  
**👨‍💻 Developed by:** muj3b

[![Donate](https://img.shields.io/badge/💖_Donate-Support_Development-ff69b4?style=for-the-badge)](https://donate.stripe.com/8x29AT0H58K03judnR0Ba01)

---

### 🎉 **Ready for the Ultimate Challenge?** 🚀

*Two game modes, endless possibilities. Download now and choose your adventure!*

</div>
