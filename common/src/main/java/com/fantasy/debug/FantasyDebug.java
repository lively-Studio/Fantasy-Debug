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

package com.fantasy.debug;

import com.fantasy.debug.tick.TickCounter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fantasy:Debug —— 高性能开发调试工具。
 * 主要功能在客户端（外部 Swing 诊断窗口）。此处注册服务端 tick 计数，供独立窗口估算 TPS。
 */
public class FantasyDebug implements ModInitializer {

    public static final String MOD_ID = "fantasy_debug";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fantasy: Debug");

    @Override
    public void onInitialize() {
        ServerTickEvents.START_SERVER_TICK.register(server -> TickCounter.serverTick());
        LOGGER.info("[Fantasy:Debug] 服务端侧初始化完成，已挂载服务器 tick 计数（用于估算 TPS）。");
    }
}