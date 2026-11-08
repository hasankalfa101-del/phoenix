package dev.phoenix.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** World scanning helpers for block entities and block states in loaded chunks. */
public final class Scan {
    private static long cachedAt = -1000;
    private static List<BlockEntity> cache = new ArrayList<>();

    private Scan() {}

    /** All block entities in loaded chunks around the player; refreshed at most once per second. */
    public static List<BlockEntity> blockEntities() {
        if (!U.ready()) return cache;
        long now = U.mc.world.getTime();
        if (Math.abs(now - cachedAt) < 20) return cache;
        cachedAt = now;
        List<BlockEntity> out = new ArrayList<>();
        int r = U.mc.options.getViewDistance().getValue();
        int pcx = U.mc.player.getChunkPos().x;
        int pcz = U.mc.player.getChunkPos().z;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                WorldChunk ch = U.mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                if (ch != null) out.addAll(ch.getBlockEntities().values());
            }
        }
        cache = out;
        return out;
    }

    /** Positions of blocks matching the predicate within the given chunk radius (capped). */
    public static List<BlockPos> blocks(Predicate<BlockState> match, int chunkRadius, int cap) {
        List<BlockPos> out = new ArrayList<>();
        if (!U.ready()) return out;
        int pcx = U.mc.player.getChunkPos().x;
        int pcz = U.mc.player.getChunkPos().z;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                WorldChunk ch = U.mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                if (ch == null) continue;
                ChunkSection[] sections = ch.getSectionArray();
                int bottom = ch.getBottomY();
                int sx = ch.getPos().getStartX();
                int sz = ch.getPos().getStartZ();
                for (int i = 0; i < sections.length; i++) {
                    ChunkSection sec = sections[i];
                    if (sec == null || sec.isEmpty() || !sec.hasAny(match)) continue;
                    int baseY = bottom + i * 16;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                if (match.test(sec.getBlockState(x, y, z))) {
                                    out.add(new BlockPos(sx + x, baseY + y, sz + z));
                                    if (out.size() >= cap) return out;
                                }
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    /** ARGB colour used to draw a storage block entity, or 0 if it is not a storage block. */
    public static int storageColor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return 0xFFFF4444;
        if (be instanceof ChestBlockEntity) return 0xFFFFA500;
        if (be instanceof EnderChestBlockEntity) return 0xFFAA55FF;
        if (be instanceof BarrelBlockEntity) return 0xFFC68642;
        if (be instanceof ShulkerBoxBlockEntity) return 0xFFFF66CC;
        if (be instanceof HopperBlockEntity) return 0xFF9AA0A6;
        if (be instanceof DispenserBlockEntity) return 0xFF8899AA;
        if (be instanceof AbstractFurnaceBlockEntity) return 0xFF7A8594;
        return 0;
    }

    /** Containers that usually hold player loot (used by the chunk scanners). */
    public static boolean isLootStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity;
    }
}
