package dev.phoenix.modules;

import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class BaseRaid {
    private BaseRaid() {}

    public static void register(ModuleManager m) {
        m.add(new Stub("Player Bypass", Category.BASE_RAID));
        m.add(new Stub("Render Distance Toggle", Category.BASE_RAID));
        m.add(new Stub("Home Meta", Category.BASE_RAID));
        m.add(new Stub("Quantum", Category.BASE_RAID));
        m.add(new Stub("Render Util", Category.BASE_RAID));
        m.add(new Stub("Amethyst Bypass", Category.BASE_RAID));
        m.add(new SusChunkFinder());
        m.add(new ChunkScanner());
        m.add(new Stub("Prime Chunk Finder", Category.BASE_RAID));
        m.add(new BlockEntityDebug());
        m.add(new Stub("Light Debug", Category.BASE_RAID));
    }

    /** One loaded chunk and how many containers / spawners it holds. */
    static final class ChunkInfo {
        final int cx, cz;
        int storage, spawners;
        ChunkInfo(int cx, int cz) { this.cx = cx; this.cz = cz; }
    }

    static List<ChunkInfo> scanChunks() {
        Map<Long, ChunkInfo> map = new HashMap<>();
        for (BlockEntity be : Scan.blockEntities()) {
            boolean storage = Scan.isLootStorage(be);
            boolean spawner = be instanceof MobSpawnerBlockEntity;
            if (!storage && !spawner) continue;
            int cx = be.getPos().getX() >> 4, cz = be.getPos().getZ() >> 4;
            long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
            ChunkInfo info = map.computeIfAbsent(key, k -> new ChunkInfo(cx, cz));
            if (storage) info.storage++;
            if (spawner) info.spawners++;
        }
        List<ChunkInfo> out = new ArrayList<>(map.values());
        out.sort((a, b) -> Integer.compare(b.storage + b.spawners * 4, a.storage + a.spawners * 4));
        return out;
    }

    static class ChunkScanner extends Module {
        final NumSetting top = add(new NumSetting("Show Top", 5, 1, 12, 1));
        List<ChunkInfo> result = new ArrayList<>();
        int timer;

        ChunkScanner() { super("Chunk Scanner", Category.BASE_RAID, "Lists the loaded chunks with the most containers."); }

        @Override public void onTick() {
            if (timer-- > 0) return;
            timer = 40;
            result = scanChunks();
        }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            var tr = mc.textRenderer;
            int y = 150;
            ctx.drawText(tr, "Chunk Scanner", 6, y, Theme.accent, true);
            y += 11;
            int n = 0;
            for (ChunkInfo c : result) {
                if (n++ >= top.getInt()) break;
                String s = "chunk " + c.cx + ", " + c.cz + ": " + c.storage + " containers"
                    + (c.spawners > 0 ? ", " + c.spawners + " spawners" : "");
                ctx.drawText(tr, s, 6, y, Theme.WHITE, true);
                y += 10;
            }
            if (result.isEmpty()) ctx.drawText(tr, "nothing found yet", 6, y, Theme.TEXT_DIM, true);
        }
    }

    static class SusChunkFinder extends Module {
        final NumSetting minStorage = add(new NumSetting("Min Containers", 6, 2, 50, 1));
        List<ChunkInfo> result = new ArrayList<>();
        final Set<Long> notified = new HashSet<>();
        int timer;

        SusChunkFinder() { super("Sus Chunk Finder", Category.BASE_RAID, "Marks chunks with an unusually high number of containers."); }

        @Override protected void onEnable() { notified.clear(); }

        @Override public void onTick() {
            if (timer-- > 0) return;
            timer = 40;
            result = new ArrayList<>();
            for (ChunkInfo c : scanChunks()) {
                if (c.storage < minStorage.getInt() && c.spawners == 0) continue;
                result.add(c);
                long key = ((long) c.cx << 32) | (c.cz & 0xFFFFFFFFL);
                if (notified.add(key)) {
                    U.chat("Suspicious chunk at " + (c.cx * 16 + 8) + ", " + (c.cz * 16 + 8) + ": "
                        + c.storage + " containers" + (c.spawners > 0 ? ", " + c.spawners + " spawners" : "") + ".");
                }
            }
        }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (result.isEmpty() || !Proj.begin()) return;
            Draw.beginReal(ctx);
            try {
                double eyeY = mc.player.getEyeY();
                for (ChunkInfo c : result) {
                    Vec3d centre = new Vec3d(c.cx * 16 + 8, eyeY, c.cz * 16 + 8);
                    double[] p = Proj.to(centre);
                    if (p == null) continue;
                    int col = c.spawners > 0 ? 0xFFFF4D4D : 0xFFFFA500;
                    Draw.box(ctx, p[0] - 6, p[1] - 6, p[0] + 6, p[1] + 6, col, 2);
                    String label = "sus " + c.storage + (c.spawners > 0 ? " +" + c.spawners + "sp" : "");
                    Draw.text(ctx, label, (float) p[0] + 10, (float) p[1] - 6, col, 1.5f);
                }
            } finally {
                Draw.endReal(ctx);
            }
        }
    }

    static class BlockEntityDebug extends Module {
        BlockEntityDebug() { super("Block Entity Debug", Category.BASE_RAID, "Shows how many of each block entity are loaded."); }

        @Override public void onHud(DrawContext ctx, float tp) {
            if (mc.options.hudHidden) return;
            Map<String, Integer> counts = new TreeMap<>();
            for (BlockEntity be : Scan.blockEntities()) {
                String n = be.getClass().getSimpleName().replace("BlockEntity", "");
                counts.merge(n, 1, Integer::sum);
            }
            var tr = mc.textRenderer;
            int y = 150;
            int x = mc.getWindow().getScaledWidth() / 2 + 60;
            ctx.drawText(tr, "Block Entities", x, y, Theme.accent, true);
            y += 11;
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                ctx.drawText(tr, e.getKey() + ": " + e.getValue(), x, y, Theme.WHITE, true);
                y += 10;
            }
        }
    }
}
