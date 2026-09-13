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
package com.fantasy.debug.tick;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务端 tick 计数器。
 * 在 FantasyDebug#onInitialize 中注册到 ServerTickEvents，
 * 无论主世界是否卡死，独立 Swing 线程都能据此估算真实 TPS 与主线程健康状况。
 */
public final class TickCounter {

    @FunctionalInterface
    public interface TickListener {
        void onTick(long tick);
    }

    /** 当前累计的服务端 tick 数。 */
    public static final AtomicLong TICKS = new AtomicLong(0);

    /** 是否收到了任何服务端 tick（用于判断集成服务器是否在本 JVM 中运行）。 */
    public static volatile boolean serverAlive = false;

    /** 最近一次回调（可为 null）。 */
    private static volatile TickListener listener;

    private TickCounter() {
    }

    /** 由 FantasyDebug 主入口调用，每次服务端 tick 递增计数。 */
    public static void serverTick() {
        long value = TICKS.incrementAndGet();
        serverAlive = true;
        TickListener l = listener;
        if (l != null) {
            try {
                l.onTick(value);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void setListener(TickListener listener) {
        TickCounter.listener = listener;
    }
}