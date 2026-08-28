-- snake_menu.lua
-- Snake Game Menu Entry for Receiver UI
-- Place in: /home/diggerwf/reciver/scripts/games/snake_menu.lua
-- Requires: menu.lua (core menu system), mpv script-messages

local snake_menu = {}

-- Snake Game Metadata
snake_menu.meta = {
    id = "snake",
    title = "SNAKE",
    version = "0.1.0-beta",
    author = "diggerwf",
    description = [[
Klassisches Snake-Spiel mit Level-System:
- Steuerung: Pfeiltasten (← ↑ → ↓) + OK/Enter
- Alle 5 Äpfel = Level Up (Geschwindigkeit steigt)
- Ab Level 3: Hindernisse (graue Blöcke)
- Highscore wird nicht gespeichert (Beta)
- Läuft direkt auf DRM/KMS (HDMI), kein X11 nötig
]],
    icon = "snake_icon.png",  -- optional: 128x128 PNG in same folder
    installed = false,
    install_path = "/home/diggerwf/snake",
    repo_url = "https://github.com/diggerwf/Reciver-Games",
    tag = "v0.1.0-beta",
}

-- Menu State
snake_menu.state = {
    focused = 1,  -- 1=install, 2=back
    items = {"install", "back"},
    labels = {"Installieren", "Zurück"},
}

-- Check if already installed
function snake_menu.check_installed()
    local f = io.open(snake_menu.meta.install_path .. "/snake.jar", "r")
    if f then f:close(); snake_menu.meta.installed = true; return true end
    snake_menu.meta.installed = false
    return false
end

-- Draw the snake menu page
function snake_menu.draw()
    local w, h = 960, 540  -- logical resolution
    local cx, cy = w/2, h/2

    -- Background overlay
    draw_rect(0, 0, w, h, 0, 0, 0, 0.85)

    -- Title
    draw_text(snake_menu.meta.title, cx - 80, 60, 48, 1, 0.9, 0.2, 1)

    -- Version
    draw_text("Version " .. snake_menu.meta.version, cx - 60, 115, 18, 0.7, 0.7, 0.7, 1)

    -- Icon placeholder (draw snake head)
    local icon_size = 80
    local ix, iy = cx - icon_size/2, 150
    -- Snake head (green rect)
    draw_rect(ix + 10, iy + 10, icon_size - 20, icon_size - 20, 0.2, 1, 0.2, 1)
    -- Eyes
    draw_rect(ix + 25, iy + 25, 10, 10, 0, 0, 0, 1)
    draw_rect(ix + 45, iy + 25, 10, 10, 0, 0, 0, 1)
    -- Apple (red)
    draw_rect(ix + icon_size + 10, iy + 20, 20, 20, 1, 0.2, 0.2, 1)

    -- Description box
    local desc_x, desc_y = 80, 260
    local desc_w, desc_h = w - 160, 180
    draw_rect(desc_x, desc_y, desc_w, desc_h, 0.1, 0.1, 0.1, 0.9)
    draw_rect(desc_x, desc_y, desc_w, 2, 0.3, 0.8, 0.3, 1)  -- top accent line

    -- Description text (wrapped)
    local lines = {}
    for line in snake_menu.meta.description:gmatch("[^\n]+") do
        table.insert(lines, line)
    end
    local y = desc_y + 15
    for _, line in ipairs(lines) do
        draw_text(line, desc_x + 15, y, 18, 0.9, 0.9, 0.9, 1)
        y = y + 24
    end

    -- Buttons
    local btn_w, btn_h = 200, 50
    local btn_y = desc_y + desc_h + 30
    local gap = 40
    local total_w = btn_w * 2 + gap
    local start_x = cx - total_w/2

    for i, item in ipairs(snake_menu.state.items) do
        local x = start_x + (i-1) * (btn_w + gap)
        local focused = (snake_menu.state.focused == i)
        local label = snake_menu.state.labels[i]

        -- Button background
        if focused then
            draw_rect(x, btn_y, btn_w, btn_h, 0.2, 0.7, 0.2, 1)
            draw_rect(x, btn_y, btn_w, 3, 1, 1, 0.2, 1)
        else
            draw_rect(x, btn_y, btn_w, btn_h, 0.2, 0.2, 0.2, 1)
            draw_rect(x, btn_y, btn_w, btn_h, 0.4, 0.4, 0.4, 0.5) -- border
        end

        -- Button label
        local tw = label:len() * 9
        draw_text(label, x + (btn_w - tw)/2, btn_y + 14, 22, 1, 1, 1, 1)

        -- Install status on install button
        if item == "install" and snake_menu.meta.installed then
            draw_text("✓ Installiert", x + 10, btn_y + btn_h + 8, 14, 0.3, 1, 0.3, 1)
        end
    end

    -- Hint bar
    draw_text("Pfeile: Navigieren  |  OK: Auswählen  |  Exit: Zurück", cx - 200, h - 40, 16, 0.6, 0.6, 0.6, 1)
end

-- Navigation
function snake_menu.nav_left()
    if snake_menu.state.focused > 1 then
        snake_menu.state.focused = snake_menu.state.focused - 1
    end
end

function snake_menu.nav_right()
    if snake_menu.state.focused < #snake_menu.state.items then
        snake_menu.state.focused = snake_menu.state.focused + 1
    end
end

function snake_menu.nav_up()
    -- no vertical nav in this menu
end

function snake_menu.nav_down()
    -- no vertical nav in this menu
end

-- Action (OK pressed)
function snake_menu.action()
    local item = snake_menu.state.items[snake_menu.state.focused]
    if item == "install" then
        snake_menu.install()
    elseif item == "back" then
        snake_menu.exit()
    end
end

-- Install snake game
function snake_menu.install()
    if snake_menu.meta.installed then
        snake_menu.launch()
        return
    end

    -- Show installing overlay
    draw_rect(0, 0, 960, 540, 0, 0, 0, 0.9)
    draw_text("Installiere Snake...", 380, 250, 28, 1, 1, 0.2, 1)
    draw_text("Bitte warten...", 400, 290, 18, 0.7, 0.7, 0.7, 1)
    mpv.osd_message("Installiere Snake...", 10)

    -- Run install script (git clone + build)
    local cmd = string.format([[
        cd /home/diggerwf && \
        git clone --branch %s --depth 1 %s Reciver-Games 2>&1 && \
        cd Reciver-Games/snake && \
        ./gradlew fatJar 2>&1 && \
        cp build/libs/snake-1.0.jar snake.jar && \
        echo "SUCCESS"
    ]], snake_menu.meta.tag, snake_menu.meta.repo_url)

    local handle = io.popen(cmd)
    local result = handle:read("*a")
    handle:close()

    if result:match("SUCCESS") then
        snake_menu.meta.installed = true
        mpv.osd_message("Snake installiert!", 3)
        snake_menu.draw()
    else
        mpv.osd_message("Installation fehlgeschlagen!", 5)
        snake_menu.draw()
    end
end

-- Launch snake game
function snake_menu.launch()
    mpv.osd_message("Starte Snake...", 2)
    mpv.command_native({"run", "/home/diggerwf/snake/snake.sh"})
end

-- Exit menu
function snake_menu.exit()
    -- Return to main menu
    mpv.command_native({"script-message", "blue-navigate", "0", "0"})
end

-- Entry point from menu.lua
function snake_menu.enter()
    snake_menu.check_installed()
    snake_menu.state.focused = 1
    snake_menu.draw()
end

return snake_menu