# Speedrunner Swap 
 
 **Speedrunner Swap** is a Minecraft plugin that brings Dream's "Speedrunner Swap" challenge to your server! With this plugin, multiple speedrunners share the same player state and swap control on a configurable timer, while hunters try to stop them from beating the game. 
 
 This version offers more features and customization than the original! Highlights include a built-in GUI, the option for randomized countdowns instead of fixed intervals, safe swap functionality, and much more. 
 
 --- 
 
 ## 📺 Original YouTube Video 
 
 `https://img.youtube.com/vi/Zj3G5hN-EBQ/0.jpg` ]( `https://www.youtube.com/watch?v=Zj3G5hN-EBQ)` 
 
 --- 
 
 ## 🚀 How It Works 
 
 - **Swap Mechanic** 
   - Speedrunners swap control at configurable intervals (default: 60 seconds). 
   - Swaps can be fixed or randomized (customizable min/max, Gaussian jitter). 
   - Inactive runners are frozen (blindness/slowness effects or spectator mode); cannot move or interact. 
 - **Shared State** 
   - Runners share inventory, health, position, experience, and status effects. 
   - Requires teamwork and communication. 
 - **Hunters** 
   - Track the active runner using a compass (updates periodically). 
   - Coordinates shown in the action bar. 
   - Use PvP to eliminate runners. 
 - **Objective** 
   - Runners must defeat the Ender Dragon before hunters kill them. 
 
 --- 
 
 ## ✨ Features 
 
 - **Customizable Swap System** 
   - Fixed or randomized intervals 
   - Jitter settings 
   - Grace period after swaps 
   - Auto-pause on disconnect 
 - **Safe Swap Mode** 
   - Avoids dangerous locations (lava, fire, etc.) 
 - **Freeze Inactive Runners** 
   - Configurable modes: effects or spectator 
 - **Hunter Compass Tracking** 
   - Periodic updates 
   - Coordinate display 
 - **GUI Menus** 
   - Team selection 
   - Settings 
   - Game management 
 - **Broadcast Messages** 
   - Swaps 
   - Game events 
   - Team changes 
 - **Optional Simple Voice Chat Integration** 
   - Mute inactive runners 
 - **Version Support** 
   - Minecraft 1.21+ 
   - Bukkit/Spigot/Paper servers 
 
 --- 
 
 ## 🛠️ Installation 
 
 1. **Download** the latest `.jar` from Modrinth. 
 2. **Place** it into your server’s `plugins` folder. 
 3. **Restart** your server. 
 4. **Configure** settings in `config.yml`: 
    - Teams 
    - Swap intervals 
    - Tracker options 
 
 --- 
 
 ## 📝 Commands 
 
 | Command              | Description                       | 
 |----------------------|-----------------------------------| 
 | `/swap start`        | Start a Speedrunner Swap game     | 
 | `/swap stop`         | End the current game              | 
 | `/swap pause`        | Pause the game                    | 
 | `/swap resume`       | Resume the game                   | 
 | `/swap status`       | Check game status                 | 
 | `/swap setrunners`   | Set runner players                | 
 | `/swap sethunters`   | Set hunter players                | 
 | `/swap reload`       | Reload configuration              | 
 | `/swap gui`          | Open the management GUI           | 
 
 > Requires `speedrunnerswap.command` permission (default: op). 
 
 --- 
 
 ## 🙌 Credits 
 
 - **Inspired by:** Dream’s Speedrunner Swap Manhunt 
 - **Developed by:** muj3b (me) 
 
 --- 
 
 Enjoy!