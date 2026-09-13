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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 维度与实体状态探针。
 * 所有逻辑都必须在游戏线程执行（调用方负责调度）；本类自身不触碰 Swing EDT。
 * 通过反射泛化读取任意模组的驯服状态字段（tamed / owner / ownerUuid / tamedByOwner），
 * 从而诊断「已在创造物品栏驯服列表却显示为空」这类实体注册/状态问题。
 */
public final class WorldProbe {

    /** 一条实体状态记录：纯数据，可安全跨线程传递。 */
    public static final class EntityLine {
        public final String dim;
        public final String type;
        public final String name;
        public final String uuid;
        public final String pos;
        public final String tamed;
        public final String owner;
        public final String id1;

        EntityLine(String dim, String type, String name, String uuid, String pos,
                   String tamed, String owner, String id1) {
            this.dim = dim;
            this.type = type;
            this.name = name;
            this.uuid = uuid;
            this.pos = pos;
            this.tamed = tamed;
            this.owner = owner;
            this.id1 = id1;
        }
    }

    private static final String[] TAMED_FIELDS = {
            "tamed", "tamedByOwner", "isTamed", "trusting", "ownerless"
    };
    private static final String[] OWNER_FIELDS = {
            "owner", "ownerUuid", "ownerUUID", "ownerId"
    };

    private WorldProbe() {
    }

    /** 收集所有维度（及其下的实体）快照。必须在游戏线程调用。 */
    public static List<EntityLine> capture() {
        List<EntityLine> lines = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return lines;
        }
        ClientWorld world = client.world;
        String dim = dimName(world);
        int entityCount = 0;
        List<Entity> entities = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e != null) {
                entities.add(e);
                entityCount++;
            }
        }
        StringBuilder summary = new StringBuilder(64);
        summary.append("[维度] ").append(dim)
               .append(" / 实体数=").append(entityCount);
        lines.add(new EntityLine(summary.toString(), "---- 维度摘要 ----", "", "", "", "", "", ""));

        for (Entity e : entities) {
            lines.add(toLine(world, e));
        }
        return lines;
    }

    private static EntityLine toLine(World world, Entity e) {
        String dim = dimName(world);
        String type = e.getType() != null ? e.getType().toString() : "?";
        String customName = e.getCustomName() != null ? e.getCustomName().getString() : "";
        String uuid = shortUuid(e.getUuid());
        BlockPos bp = e.getBlockPos();
        String pos = bp != null ? bp.getX() + "," + bp.getY() + "," + bp.getZ() : "?";

        String tamed = "?";
        String owner = "?";

        // 反射探测驯服状态
        for (Field f : collectFields(e.getClass())) {
            String fieldName = f.getName().toLowerCase();
            if (matches(fieldName, TAMED_FIELDS)) {
                tamed = readField(e, f);
            }
            if (matches(fieldName, OWNER_FIELDS)) {
                owner = readField(e, f);
            }
        }

        String id1 = String.valueOf(e.getId());
        String display = customName.isEmpty() ? id1 : customName + " (#" + id1 + ")";

        // 只关心带有驯服字段的实体会在摘要下方高亮；这里原样输出全部
        String mark = (isProbablyTamedThing(tamed, owner)) ? "  [被驯服]" : "";
        String name = (e instanceof PlayerEntity)
                ? "玩家 " + ((PlayerEntity) e).getDisplayName().getString() + " #" + id1
                : display + mark;
        return new EntityLine(dim, type, name, uuid, pos, tamed, owner, id1);
    }

    private static boolean isProbablyTamedThing(String tamed, String owner) {
        boolean t = "true".equalsIgnoreCase(tamed) || "1".equals(tamed.trim());
        boolean o = owner != null && !owner.isEmpty() && !"null".equalsIgnoreCase(owner)
                && !"?".equals(owner);
        return t || o;
    }

    private static boolean matches(String fieldName, String[] keys) {
        for (String k : keys) {
            if (fieldName.contains(k)) return true;
        }
        return false;
    }

    private static List<Field> collectFields(Class<?> clazz) {
        List<Field> out = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                out.add(f);
            }
            c = c.getSuperclass();
        }
        return out;
    }

    private static String readField(Object target, Field f) {
        try {
            f.setAccessible(true);
            Object v = f.get(target);
            if (v == null) return "null";
            if (v instanceof UUID) return v.toString();
            return v.toString();
        } catch (Throwable t) {
            return "err:" + t.getClass().getSimpleName();
        }
    }

    private static String dimName(World world) {
        Identifier id = world.getRegistryKey().getValue();
        return id != null ? id.toString() : "?";
    }

    private static String shortUuid(UUID uuid) {
        if (uuid == null) return "null";
        String s = uuid.toString();
        return s.substring(0, 8) + "…";
    }
}