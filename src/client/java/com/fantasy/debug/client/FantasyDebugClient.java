/*
 * MIT License
 *
 * Copyright (c) 2026 cangcang
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

import com.fantasy.debug.FantasyDebug;
import com.fantasy.debug.client.ui.DebugWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import javax.swing.SwingUtilities;

/**
 * Fantasy:Debug 客户端入口（fabric.mod.json 声明的 client entrypoint）。
 * 注册「打开诊断窗口」按键（默认数字键 0），在客户端 tick 中监听，
 * 按 0 在独立外部 Swing 诊断窗口与隐藏之间切换——即便主世界 tick 卡死也能弹出。
 */
public class FantasyDebugClient implements ClientModInitializer {

    private static KeyBinding openWindowKey;
    private static DebugWindow window;

    @Override
    public void onInitializeClient() {
        openWindowKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fantasy_debug.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_0,
                "category.fantasy_debug"
        ));

        // 所有 Swing 组件的创建/访问都放到 EDT，避免跨线程读写 JFrame
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openWindowKey.wasPressed()) {
                SwingUtilities.invokeLater(() -> {
                    if (window == null) {
                        window = new DebugWindow();
                    }
                    if (window.isOpen()) {
                        window.hide();
                    } else {
                        window.open();
                    }
                });
            }
        });

        FantasyDebug.LOGGER.info("[Fantasy:Debug] 客户端初始化完成：已挂载按键（默认 0 键）与独立诊断窗口切换逻辑。");
    }
}
