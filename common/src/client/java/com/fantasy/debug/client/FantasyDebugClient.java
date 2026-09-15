/*
 * Copyright (c) 2026 lively-Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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