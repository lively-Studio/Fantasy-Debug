/*
 * Copyright (C) 2026 cangcang
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fantasy.debug.client;

import com.fantasy.debug.client.ui.DebugWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fantasy:Debug 客户端入口。
 * 注册 0 键开关外部 Swing 诊断窗口（工作区）。
 */
public class FantasyDebugClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("Fantasy-Debug-Client");

    private static DebugWindow window;
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        // 默认按 0 键打开/切换调查工作区
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("fantasy_debug", "category"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fantasy_debug.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_0,
                cat
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                toggle();
            }
        });

        window = new DebugWindow();
        LOGGER.info("[Fantasy:Debug] 客户端初始化完成，按 0 键打开诊断窗口。");
    }

    private static void toggle() {
        if (window == null) return;
        if (window.isOpen()) {
            window.hide();
        } else {
            window.open();
        }
    }
}