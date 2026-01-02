# 🎮 AksaraAntiGMC

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.4-green?style=for-the-badge&logo=minecraft" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/Paper-Compatible-blue?style=for-the-badge" alt="Paper Compatible"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License"/>
</p>

**AksaraAntiGMC** adalah plugin Minecraft Paper yang dirancang untuk mencegah penyalahgunaan Creative Mode (GMC) di server. Plugin ini menyediakan sistem konfirmasi, pengelolaan inventory terpisah, dan berbagai pembatasan untuk memastikan keamanan ekonomi server.

---

## ✨ Fitur Utama

- 🔒 **Sistem Konfirmasi Creative Mode** - Admin harus mengirim permintaan, pemain harus konfirmasi
- 📦 **Inventory Terpisah** - Inventory Survival dan Creative disimpan terpisah
- 🚫 **Pembatasan Container** - Block akses ke chest, shulker, barrel, dll saat Creative
- 🎯 **Creative Lore Tagging** - Semua item yang diambil saat Creative ditandai dengan lore
- 🔄 **Auto-Survival on Rejoin** - Pemain yang disconnect saat Creative otomatis jadi Survival
- 🥚 **Block Spawn Eggs** - Pemain Creative tidak bisa menggunakan spawn eggs
- 📤 **Block Item Drop/Pickup** - Mencegah transfer item dengan drop/pickup

---

## 📥 Instalasi

1. Download file `AksaraAntiGMC-1.0.0.jar` dari [Releases](../../releases)
2. Letakkan file `.jar` di folder `plugins/` server Anda
3. Restart server
4. Edit `config.yml` sesuai kebutuhan
5. Jalankan `/aksaraantigmc reload` untuk reload config

---

## 📋 Commands

### `/gmc` - Main Creative Mode Command

| Command | Deskripsi | Permission |
|---------|-----------|------------|
| `/gmc <player>` | Kirim permintaan GMC ke pemain | `aksaraantigmc.gmc` |
| `/gmc confirm` | Konfirmasi permintaan GMC | Semua pemain* |

*Pemain harus memiliki pending confirmation

### `/aksaraantigmc` - Admin Command

| Command | Deskripsi | Permission |
|---------|-----------|------------|
| `/aksaraantigmc reload` | Reload konfigurasi | `aksaraantigmc.admin` |
| `/aksaraantigmc restoreinventory <player> <gamemode>` | Restore inventory pemain | `aksaraantigmc.admin` |

---

## 🔑 Permissions

| Permission | Deskripsi | Default |
|------------|-----------|---------|
| `aksaraantigmc.gmc` | Izin menggunakan `/gmc <player>` | OP |
| `aksaraantigmc.admin` | Izin command admin | OP |
| `aksaraantigmc.bypass` | Bypass semua pembatasan GMC | OP |
| `aksaraantigmc.confirm` | Izin menggunakan `/gmc confirm` | Semua |

---

## 📖 Cara Penggunaan

### Untuk Admin: Memberikan Creative Mode ke Pemain

```
1. Admin menjalankan: /gmc NamaPemain
2. Pemain menerima peringatan 5x + title screen
3. Pemain mengetik: /gmc confirm (dalam 30 detik)
4. Pemain berubah ke Creative Mode dengan inventory terpisah
```

### Untuk Pemain: Keluar dari Creative Mode

Pemain **tidak bisa** keluar sendiri. Admin/Console harus menjalankan:
```
/gamemode survival NamaPemain
```

### Flow Diagram

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│    Admin    │────────>│   Plugin    │────────>│   Player    │
│ /gmc Player │         │   Request   │         │  Receives   │
└─────────────┘         │   Created   │         │   Warning   │
                        └─────────────┘         └──────┬──────┘
                                                       │
                                                       ▼
                        ┌─────────────┐         ┌─────────────┐
                        │   Gamemode  │<────────│   Player    │
                        │   Changed   │         │ /gmc confirm│
                        └─────────────┘         └─────────────┘
```

---

## ⚙️ Konfigurasi

### config.yml

```yaml
# Enable debug mode untuk logging detail
debug: false

# Commands yang di-block saat GMC
blocked-commands:
  - /trade
  - /ah
  - /auction

# Pembatasan saat Creative Mode
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

# Sistem konfirmasi GMC
gmc-confirmation:
  enabled: true     # Enable/disable sistem konfirmasi
  timeout: 30       # Timeout dalam detik
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

## 🛡️ Fitur Keamanan

### 1. Creative Lore Tagging
Semua item yang diambil saat Creative Mode akan memiliki lore:
```
[AksaraAntiGMC] Creative Items By <PlayerName>
```

### 2. Inventory Terpisah
- Survival inventory disimpan saat masuk Creative
- Creative inventory disimpan saat keluar Creative
- Data tersimpan di SQLite database

### 3. Auto-Survival on Rejoin
Jika pemain disconnect saat Creative Mode:
- Saat rejoin, otomatis diubah ke Survival
- Inventory Survival di-load kembali

### 4. External Command Protection
Jika pemain/plugin lain mencoba `/gamemode creative`:
- Event di-cancel
- Pemain harus konfirmasi via `/gmc confirm`

---

## 🗃️ Database

Plugin menggunakan **SQLite** untuk menyimpan:
- Inventory Survival setiap pemain
- Inventory Creative setiap pemain

Lokasi: `plugins/AksaraAntiGMC/inventories.db`

---

## 🔧 Troubleshooting

### Q: Pemain tidak bisa `/gmc confirm`?
**A:** Pastikan:
1. Ada pending confirmation (admin sudah run `/gmc <player>`)
2. Belum timeout (default 30 detik)
3. Server sudah restart setelah update plugin

### Q: Item tidak dapat lore?
**A:** Lore ditambahkan saat:
- Mengambil item dari Creative inventory
- Middle-click block
- Click item di inventory saat Creative

### Q: Pemain masih bisa buka chest?
**A:** Periksa:
1. `block-chest-access: true` di config
2. Pemain tidak punya permission `aksaraantigmc.bypass`
3. Pemain tidak OP

---

## 📊 Performa

| Aspek | Status |
|-------|--------|
| TPS Impact | 🟢 Minimal |
| Memory Usage | 🟢 Low |
| Database | 🟢 SQLite (Lightweight) |
| Event Listeners | 🟡 Normal (per-action) |

---

## 📝 Changelog

### v1.0.0
- Initial release
- Sistem konfirmasi GMC
- Inventory terpisah Survival/Creative
- Pembatasan container & commands
- Creative lore tagging
- Auto-GMS on rejoin
- Block spawn eggs
- Block item drop/pickup

---

## 🤝 Contributing

Pull requests are welcome! Untuk perubahan besar, buka issue terlebih dahulu.

---

## 📜 License

[MIT License](LICENSE)

---

## 👨‍💻 Author

**Aksara Project**  
Website: [www.aksaraproject.com](https://www.aksaraproject.com)

---

<p align="center">
  Made with ❤️ for Minecraft Server Owners
</p>
