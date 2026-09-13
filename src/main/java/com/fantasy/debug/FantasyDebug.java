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