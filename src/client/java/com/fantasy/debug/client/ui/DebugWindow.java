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
package com.fantasy.debug.client.ui;

import com.fantasy.debug.client.capture.CaptureService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 独立外部 Swing 诊断窗口（非游戏内 UI）。
 * 多 Tab（JVM / 线程 / 性能 / 维度与实体）+ 刷新 + 心跳标记 + 快照导出。
 * 必须在 EDT 之外创建/显示（用 SwingUtilities.invokeLater），其余绘制工作全部在 EDT 上进行。
 */
public final class DebugWindow {

    private static final String TITLE = "Fantasy: Debug 诊断窗口";

    private final JFrame frame;
    private final JTextArea jvmText;
    private final JTextArea threadText;
    private final JTextArea perfText;
    private final JTextArea worldText;

    /** 后台采样线程池（非 EDT，避免阻塞窗口交互）。 */
    private final ExecutorService sampler = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Fantasy-Debug-Sampler");
        t.setDaemon(true);
        return t;
    });

    public DebugWindow() {
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(1000, 720);
        frame.setMinimumSize(new Dimension(760, 480));

        JTabbedPane tabs = new JTabbedPane();
        jvmText = newTextArea();
        threadText = newTextArea();
        perfText = newTextArea();
        worldText = newTextArea();
        tabs.addTab("JVM / 内存 / GC", new JScrollPane(jvmText));
        tabs.addTab("线程转储", new JScrollPane(threadText));
        tabs.addTab("性能 / TPS", new JScrollPane(perfText));
        tabs.addTab("维度与实体", new JScrollPane(worldText));

        JButton refreshBtn = new JButton("刷新 (F5)");
        refreshBtn.addActionListener(e -> refresh());
        JButton markerBtn = new JButton("心跳标记");
        markerBtn.addActionListener(e -> {
            CaptureService.printHeartbeatMarker();
            append(jvmText, "\n[心跳标记已输出到 stdout]\n");
        });
        JButton snapshotBtn = new JButton("导出快照...");
        snapshotBtn.addActionListener(e -> exportSnapshot());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        topBar.add(refreshBtn);
        topBar.add(markerBtn);
        topBar.add(snapshotBtn);

        frame.add(topBar, BorderLayout.NORTH);
        frame.add(tabs, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                sampler.shutdownNow();
            }
        });
    }

    private static JTextArea newTextArea() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return ta;
    }

    public void open() {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.toFront();
            refresh();
        });
    }

    public boolean isOpen() {
        return frame.isVisible();
    }

    public void hide() {
        SwingUtilities.invokeLater(() -> frame.setVisible(false));
    }

    public void refresh() {
        if (!frame.isVisible()) return;
        sampler.submit(() -> {
            CaptureService.CaptureResult result = CaptureService.captureAll();
            SwingUtilities.invokeLater(() -> apply(result));
        });
    }

    private void apply(CaptureService.CaptureResult result) {
        jvmText.setText(result.jvm());
        threadText.setText(result.thread());
        perfText.setText(result.perf());
        worldText.setText(result.world());
        jvmText.setCaretPosition(0);
        threadText.setCaretPosition(0);
        perfText.setCaretPosition(0);
        worldText.setCaretPosition(0);
    }

    private static void append(JTextArea area, String s) {
        area.append(s);
        area.setCaretPosition(area.getDocument().getLength());
    }

    private void exportSnapshot() {
        JFileChooser chooser = new JFileChooser();
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        chooser.setSelectedFile(new File("fantasy-debug-" + ts + ".txt"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File target = chooser.getSelectedFile();
        sampler.submit(() -> {
            try {
                CaptureService.CaptureResult result = CaptureService.captureAll();
                Files.write(target.toPath(),
                        result.fullSnapshot().getBytes(StandardCharsets.UTF_8));
                SwingUtilities.invokeLater(() -> append(jvmText, "\n[快照已导出: " + target.getAbsolutePath() + "]\n"));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> append(jvmText, "\n[快照导出失败: " + ex + "]\n"));
            }
        });
    }
}