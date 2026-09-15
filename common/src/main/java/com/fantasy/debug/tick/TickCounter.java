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