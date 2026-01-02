# 🎮 AksaraAntiGMC

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.4-green?style=for-the-badge&logo=minecraft" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/Paper-Compatible-blue?style=for-the-badge" alt="Paper Compatible"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License"/>
</p>

A Minecraft Paper plugin to prevent Creative Mode (GMC) abuse on your server. This plugin provides confirmation system, separate inventory management, and many restrictions to protect your server economy.

---

## ✨ Features

- 🔒 **Confirmation System** - Admin sends request, player must confirm
- 📦 **Separate Inventory** - Survival and Creative inventory stored separately
- 🚫 **Block Containers** - Block access to chest, shulker, barrel, etc in Creative
- 🎯 **Creative Item Tagging** - All items obtained in Creative get a lore tag
- 🔄 **Auto-Survival on Rejoin** - Players who disconnect in Creative become Survival
- 🥚 **Block Spawn Eggs** - Creative players cannot use spawn eggs
- 📤 **Block Drop/Pickup** - Prevent item transfer via drop/pickup

---

## 📥 Installation

1. Download `AksaraAntiGMC-1.0.0.jar` from [Releases](../../releases)
2. Put the `.jar` file in your server `plugins/` folder
3. Restart server
4. Edit `config.yml` as needed
5. Run `/aksaraantigmc reload` to reload config

---

## 📋 Commands

### `/gmc` - Main Command

| Command | Description | Permission |
|---------|-------------|------------|
| `/gmc <player>` | Send GMC request to player | `aksaraantigmc.gmc` |
| `/gmc confirm` | Confirm GMC request | Everyone* |

*Player must have pending confirmation

### `/aksaraantigmc` - Admin Command

| Command | Description | Permission |
|---------|-------------|------------|
| `/aksaraantigmc reload` | Reload config | `aksaraantigmc.admin` |
| `/aksaraantigmc restoreinventory <player> <gamemode>` | Restore player inventory | `aksaraantigmc.admin` |

---

## 🔑 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `aksaraantigmc.gmc` | Can use `/gmc <player>` | OP |
| `aksaraantigmc.admin` | Can use admin commands | OP |
| `aksaraantigmc.bypass` | Bypass all GMC restrictions | OP |
| `aksaraantigmc.confirm` | Can use `/gmc confirm` | Everyone |

---

## 📖 How to Use

### For Admin: Give Creative Mode to Player

```
1. Admin runs: /gmc PlayerName
2. Player gets 5 warning messages + title screen
3. Player types: /gmc confirm (within 30 seconds)
4. Player changes to Creative Mode with separate inventory
```

### For Player: Exit Creative Mode

Players **cannot** exit by themselves. Admin/Console must run:
```
/gamemode survival PlayerName
```

### Flow Diagram

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│    Admin    │────────>│   Plugin    │────────>│   Player    │
│ /gmc Player │         │   Request   │         │  Gets       │
└─────────────┘         │   Created   │         │  Warning    │
                        └─────────────┘         └──────┬──────┘
                                                       │
                                                       ▼
                        ┌─────────────┐         ┌─────────────┐
                        │  Gamemode   │<────────│   Player    │
                        │  Changed    │         │ /gmc confirm│
                        └─────────────┘         └─────────────┘
```

---

## ⚙️ Configuration

### config.yml

```yaml
# Enable debug mode for detailed logging
debug: false

# Commands blocked in Creative Mode
blocked-commands:
  - /trade
  - /ah
  - /auction

# Restrictions in Creative Mode
restrictions:
  block-chest-access: true      # Block chest
  block-ender-chest: true       # Block ender chest
  block-shulker: true           # Block shulker box
  block-barrel: true            # Block barrel
  block-dispenser: true         # Block dispenser
  block-dropper: true           # Block dropper
  block-hopper: true            # Block hopper
  block-furnace: true           # Block furnace/blast furnace/smoker
  block-item-frame: true        # Block item frame
  block-flower-pot: true        # Block flower pot
  block-bundle: true            # Block bundle
  block-item-drop: true         # Block drop items
  block-pickup-items: true      # Block pickup items
  block-spawn-eggs: true        # Block spawn eggs

# GMC confirmation system
gmc-confirmation:
  enabled: true     # Enable/disable confirmation
  timeout: 30       # Timeout in seconds
```

### messages.yml

```yaml
prefix: "&8[&bAksaraAntiGMC&8] "
no-permission: "&cYou don't have permission to use this command."

gmc:
  warning-message: "&c⚠ &eWarning: Your gamemode will switch to Creative! Type /gmc confirm to proceed."
  title: "&b&lATTENTION"
  subtitle: "&eChanging gamemode to Creative..."
  confirmation-prompt: "&eType &b/gmc confirm &eto accept the gamemode change."
  confirmation-timeout: "&cConfirmation timeout after &b%seconds% &cseconds."
  no-pending-confirmation: "&cNo pending confirmation found."
  switched: "&aYour gamemode has been changed to Creative!"
  unauthorized-access: "&cUse /gmc to properly switch to Creative mode."

items:
  lore-format: "&8[&bAksaraAntiGMC&8] &7Creative Items By &b%player%"
```

---

## 🛡️ Security Features

### 1. Creative Item Tagging
All items obtained in Creative Mode will have this lore:
```
[AksaraAntiGMC] Creative Items By <PlayerName>
```

### 2. Separate Inventory
- Survival inventory saved when entering Creative
- Creative inventory saved when leaving Creative
- Data stored in SQLite database

### 3. Auto-Survival on Rejoin
If player disconnects in Creative Mode:
- On rejoin, automatically changed to Survival
- Survival inventory is loaded back

### 4. External Command Protection
If player/plugin tries `/gamemode creative`:
- Event is cancelled
- Player must confirm via `/gmc confirm`

---

## 🗃️ Database

Plugin uses **SQLite** to store:
- Survival inventory for each player
- Creative inventory for each player

Location: `plugins/AksaraAntiGMC/inventories.db`

---

## 🔧 Troubleshooting

### Q: Player cannot `/gmc confirm`?
**A:** Check:
1. There is pending confirmation (admin already ran `/gmc <player>`)
2. Not timeout yet (default 30 seconds)
3. Server restarted after plugin update

### Q: Items don't get lore?
**A:** Lore is added when:
- Taking item from Creative inventory
- Middle-click block
- Click item in inventory while in Creative

### Q: Player can still open chest?
**A:** Check:
1. `block-chest-access: true` in config
2. Player doesn't have `aksaraantigmc.bypass` permission
3. Player is not OP

---

## 📊 Performance

| Aspect | Status |
|--------|--------|
| TPS Impact | 🟢 Minimal |
| Memory Usage | 🟢 Low |
| Database | 🟢 SQLite (Lightweight) |
| Event Listeners | 🟡 Normal (per-action) |

---

## 📝 Changelog

### v1.0.0
- Initial release
- GMC confirmation system
- Separate Survival/Creative inventory
- Container & command restrictions
- Creative item lore tagging
- Auto-GMS on rejoin
- Block spawn eggs
- Block item drop/pickup

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

## 👨‍💻 Author

**Aksara Project**  
Website: [www.aksaraproject.com](https://www.aksaraproject.com)

---

<p align="center">
  Made with ❤️ for Minecraft Server Owners
</p>
