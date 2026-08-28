# Snake Menu Integration für Receiver UI

## Dateien
```
reciver/
├── scripts/
│   ├── menu.lua              # Core Menu System (existiert)
│   └── games/
│       ├── snake_menu.lua    # <-- NEU: Snake Menu
│       └── snake_icon.png    # Optional: 128x128 Icon
```

## Einbindung in menu.lua

### 1. Require hinzufügen (oben in menu.lua)
```lua
local snake_menu = require("games.snake_menu")
```

### 2. In `blue.enter` Handler (oder wo Spiele gelistet werden)
```lua
-- In der Spiele-Liste / Blue-Menü
local game_list = {
    {id = "snake", title = "SNAKE", icon = "games/snake_icon.png", module = snake_menu},
    -- weitere Spiele...
}
```

### 3. Navigation im Menü erweitern
```lua
-- In cec-nav Handler für Spiel-Seite
if current_page == "games" then
    if action == "nav:left" then
        snake_menu.nav_left()
    elseif action == "nav:right" then
        snake_menu.nav_right()
    elseif action == "nav:ok" then
        snake_menu.action()
    elseif action == "nav:exit" then
        snake_menu.exit()
    end
end
```

### 4. Draw-Funktion aufrufen
```lua
-- In render/draw loop für game page
if current_page == "games" and selected_game == "snake" then
    snake_menu.draw()
end
```

### 5. Enter-Callback beim Fokus auf Snake
```lua
function on_game_selected(game_id)
    if game_id == "snake" then
        snake_menu.enter()
    end
end
```

## mpv script-messages (bereits implementiert)

| Message | Parameter | Bedeutung |
|---------|-----------|-----------|
| `cec-nav` | `left/right/up/down/ok/esc` | Navigation |
| `blue-enter` | — | OK auf fokussiertem Element |
| `blue-navigate` | `dx, dy` | Kanal-Tasten (V+/V-) |

## Installer-Flow

1. **User wählt "Installieren"** → `snake_menu.install()`
2. **Git Clone + Gradle Build** läuft im Hintergrund
3. **Erfolg:** Button ändert sich zu "Starten" / "✓ Installiert"
4. **Fehler:** OSD-Meldung "Installation fehlgeschlagen"

## Voraussetzungen auf dem Pi

```bash
# Im snake/ Ordner:
sudo apt install git openjdk-21-jdk gradle libglfw3 libgbm1 libdrm2 libegl1 libgles2
sudo usermod -a -G video,render $USER
# Reboot!
```

## Manuelle Installation (falls Auto-Install fehlschlägt)
```bash
cd /home/diggerwf
git clone --branch v0.1.0-beta --depth 1 https://github.com/diggerwf/Reciver-Games.git
cd Reciver-Games/snake
./gradlew fatJar
cp build/libs/snake-1.0.jar snake.jar
```

## Icon erstellen (optional)
```bash
# 128x128 PNG, transparenter Hintergrund
# Grün: Snake-Kopf, Rot: Apfel
# Speichern als: scripts/games/snake_icon.png
```

## Test
```bash
# Im mpv Lua-Kontext:
script-message blue-enter   # auf Snake-Seite → zeigt Menü
script-message cec-nav left # navigiert
script-message cec-nav ok   # installiert/startet
```