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
package com.fantasy.debug.client.probe;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * JVM / 线程诊断探针。
 * 与游戏主线程解耦（Swing 独立线程调用），即便主世界 tick 卡死也能稳定捕获数据。
 */
public final class SystemProbe {

    /** 运行时总览文本（内存 / GC / JVM 信息 / CPU 粗估）。 */
    public static String jvmOverview() {
        StringBuilder sb = new StringBuilder(1024);
        Runtime rt = Runtime.getRuntime();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        sb.append("=== JVM 运行时 ===\n");
        sb.append("JVM: ").append(runtime.getVmName()).append(' ').append(runtime.getVmVersion()).append('\n');
        sb.append("Uptime: ").append(runtime.getUptime()).append(" ms\n");
        sb.append("OS: ").append(os.getName()).append(' ').append(os.getVersion()).append(" / ").append(os.getArch()).append('\n');
        sb.append("Processors: ").append(rt.availableProcessors()).append('\n');
        sb.append("\n=== 堆内存 ===\n");
        sb.append("Used: ").append(human(heap.getUsed())).append('\n');
        sb.append("Committed: ").append(human(heap.getCommitted())).append('\n');
        sb.append("Max: ").append(human(heap.getMax())).append('\n');
        sb.append("Used(+perm): ").append(human(rt.totalMemory() - rt.freeMemory())).append('\n');

        sb.append("\n=== GC ===\n");
        long count = 0, time = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += gc.getCollectionCount();
            time += gc.getCollectionTime();
            sb.append("  - ").append(gc.getName())
              .append(": count=").append(gc.getCollectionCount())
              .append(", time=").append(gc.getCollectionTime()).append("ms\n");
        }
        sb.append("GC 总次数: ").append(count).append(", 总耗时: ").append(time).append("ms\n");

        // OS 内存/负载（若可用）
        if (os instanceof com.sun.management.OperatingSystemMXBean sys) {
            sb.append("\n=== 系统负载 ===\n");
            sb.append("System Load Average: ").append(String.format("%.2f", os.getSystemLoadAverage())).append('\n');
            sb.append("Process CPU Load: ")
              .append(String.format("%.1f%%", sys.getProcessCpuLoad() * 100.0)).append('\n');
            try {
                sb.append("System CPU Load: ")
                  .append(String.format("%.1f%%", sys.getSystemCpuLoad() * 100.0)).append('\n');
                long total = sys.getTotalMemorySize();
                long used = total - sys.getFreeMemorySize();
                sb.append("OS 内存: ").append(human(used)).append(" / ").append(human(total)).append('\n');
            } catch (Throwable ignore) {
            }
        }
        return sb.toString();
    }

    /** 线程转储：所有线程名、状态与栈概要。 */
    public static String threadDump() {
        StringBuilder sb = new StringBuilder(8192);
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        Map<Thread, StackTraceElement[]> live = Thread.getAllStackTraces();
        sb.append("=== 线程转储 (").append(live.size()).append(" 线程) ===\n");
        for (Map.Entry<Thread, StackTraceElement[]> e : live.entrySet()) {
            Thread t = e.getKey();
            sb.append("\n[").append(t.getId()).append("] ").append(t.getName())
              .append(" | state=").append(t.getState())
              .append(" | daemon=").append(t.isDaemon())
              .append(" | alive=").append(t.isAlive()).append('\n');
            StackTraceElement[] st = e.getValue();
            int shown = Math.min(st.length, 8);
            for (int i = 0; i < shown; i++) {
                sb.append("    at ").append(st[i]).append('\n');
            }
            if (st.length > shown) sb.append("    ... ").append(st.length - shown).append(" more\n");
        }
        // 死锁检测
        try {
            long[] deadlocks = bean.findDeadlockedThreads();
            if (deadlocks != null && deadlocks.length > 0) {
                sb.append("\n!!! [死锁] 检测到 ").append(deadlocks.length).append(" 个死锁线程 !!!\n");
                for (long id : deadlocks) {
                    ThreadInfo info = bean.getThreadInfo(id);
                    if (info != null) sb.append("  - ").append(info.getThreadName()).append(" (id=").append(id).append(")\n");
                }
            } else {
                sb.append("\n[OK] 未检测到死锁。\n");
            }
        } catch (Throwable ignore) {
        }
        return sb.toString();
    }

    private static String human(long bytes) {
        if (bytes < 0) return "?";
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1) return String.format("%.2f GB", gb);
        if (mb >= 1) return String.format("%.1f MB", mb);
        return String.format("%.1f KB", kb);
    }

    private SystemProbe() {
    }
}