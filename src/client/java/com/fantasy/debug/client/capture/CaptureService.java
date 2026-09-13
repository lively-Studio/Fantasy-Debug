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
package com.fantasy.debug.client.capture;

import com.fantasy.debug.client.probe.PerfProbe;
import com.fantasy.debug.client.probe.SystemProbe;
import com.fantasy.debug.client.probe.WorldProbe;
import net.minecraft.client.MinecraftClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 汇总采集服务（分 Tab 编辑）。
 * JVM / 线程 / 性能探针无需游戏线程，可在 Swing 后台线程立即执行；
 * 维度 / 实体探针需要游戏线程，此处异步派发并带超时——若游戏线程已卡死则回退为提示文本，
 * 从而实现在「主世界等待死掉」时仍可靠捕获关键数据。
 */
public final class CaptureService {

    /** 多 Tab 采集结果：纯文本，跨线程安全。 */
    public record CaptureResult(String jvm, String thread, String perf, String world) {
        public String fullSnapshot() {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return "===== Fantasy-Debug 快照 "
                    + fmt.format(new Date()) + " =====\n"
                    + jvm + "\n\n"
                    + thread + "\n\n"
                    + perf + "\n\n"
                    + "===== 维度与实体 =====\n" + world + "\n";
        }
    }

    /** 一次性采集全部数据。所有逻辑可安全地在非 EDT 线程调用。 */
    public static CaptureResult captureAll() {
        return new CaptureResult(
                SystemProbe.jvmOverview(),
                SystemProbe.threadDump(),
                PerfProbe.current(),
                captureWorld()
        );
    }

    /** 触发「外部捕获」心跳标记，便于用户在主世界卡死时快速截图定位。 */
    public static void printHeartbeatMarker() {
        System.out.println("=====[Fantasy-Debug CAPTURE-MARKER] " + System.currentTimeMillis() + "=====");
    }

    private static String captureWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return "客户端未初始化。\n";
        }
        try {
            CompletableFuture<String> future = new CompletableFuture<>();
            client.execute(() -> {
                try {
                    future.complete(renderWorld());
                } catch (Throwable t) {
                    future.complete("世界采集异常: " + t + "\n");
                }
            });
            try {
                return future.get(2500, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                return "!!! 游戏主线程在 2.5s 内未响应，疑似卡死 !!!\n"
                        + "实体/维度数据无法即时采集；请参考「线程」Tab 的转储定位阻塞点。\n";
            } catch (Exception e) {
                return "世界采集失败: " + e + "\n";
            }
        } catch (Throwable t) {
            return "世界采集异常: " + t + "\n";
        }
    }

    private static String renderWorld() {
        StringBuilder sb = new StringBuilder(4096);
        for (WorldProbe.EntityLine line : WorldProbe.capture()) {
            switch (line.type) {
                case "---- 维度摘要 ----" -> sb.append(line.dim).append('\n');
                default -> sb.append(String.format("  [%s] %s | Type=%s | UUID=%s | 位置=%s | tamed=%s | owner=%s%n",
                        line.dim, line.name, line.type, line.uuid, line.pos, line.tamed, line.owner));
            }
        }
        return sb.toString();
    }

    private CaptureService() {
    }
}