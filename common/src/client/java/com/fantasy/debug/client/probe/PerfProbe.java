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

package com.fantasy.debug.client.probe;

import com.fantasy.debug.tick.TickCounter;
import net.minecraft.client.MinecraftClient;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 性能 / tick 探针。
 * TPS 折算通过公共源集 TickCounter 的服务器 tick 计数与真实耗时差值估算，
 * 因此即便主线程卡死，Swing 线程仍能报告上一采样周期内实际推进过多少个游戏 tick。
 */
public final class PerfProbe {

    private static long lastTick = -1;
    private static long lastNanos = -1;
    private static volatile String cached = "尚未采样。";

    private PerfProbe() {
    }

    /** 计算当前瞬时 TPS 与采样周期信息。必须在（任意非 EDT 的）采样线程调用。 */
    public static String current() {
        long nowTick = TickCounter.TICKS.get();
        boolean alive = TickCounter.serverAlive;
        long now = System.nanoTime();

        double tps = 0;
        long dtms = 0;
        if (lastTick >= 0 && lastNanos >= 0) {
            long dTick = nowTick - lastTick;
            long dNanos = now - lastNanos;
            dtms = dNanos / 1_000_000L;
            if (dNanos > 0) {
                tps = dTick * 1_000_000_000.0 / dNanos;
            }
        }
        lastTick = nowTick;
        lastNanos = now;

        StringBuilder sb = new StringBuilder(512);
        sb.append("=== 服务器 Tick / TPS ===\n");
        sb.append("服务器存活（本 JVM 内集成服）: ").append(alive ? "是" : "否").append('\n');
        sb.append("累计服务器 tick: ").append(nowTick).append('\n');
        long dTick = (lastTick >= 0) ? (nowTick - lastTick) : 0;
        sb.append(String.format("估算瞬时 TPS: %.2f (上一采样周期 %.0f ms 内推进 %d tick)%n",
                tps, (double) dtms, dTick));
        sb.append("\n=== 客户端 ===\n");
        MinecraftClient mc = MinecraftClient.getInstance();
        sb.append("Client: ").append(mc != null && mc.isRunning() ? "运行中" : "未启动").append('\n');
        sb.append("当前世界: ").append(mc != null && mc.world != null ? mc.world.getRegistryKey().getValue() : "无").append('\n');
        sb.append("当前游戏时间戳: ").append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date())).append('\n');

        sb.append("\n=== 渲染线程健康探测 ===\n");
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] ids = bean.getAllThreadIds();
        int active = 0;
        for (long id : ids) {
            ThreadInfo info = bean.getThreadInfo(id);
            if (info != null && info.getThreadState() == Thread.State.RUNNABLE) active++;
        }
        sb.append("当前 RUNNABLE 线程数: ").append(active).append(" / 总线程 ").append(ids.length).append('\n');
        cached = sb.toString();
        return cached;
    }
}