package dev.phoenix.util;

import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Periodically scans nearby chunks for target blocks and draws them as 2D boxes. */
public final class BlockFinder {
    public List<BlockPos> found = new ArrayList<>();
    private int timer;

    public void update(Set<Block> targets, int chunkRadius, int everyTicks) {
        if (targets.isEmpty()) {
            found = new ArrayList<>();
            return;
        }
        if (timer-- > 0) return;
        timer = everyTicks;
        found = Scan.blocks(st -> targets.contains(st.getBlock()), chunkRadius, 400);
    }

    public void reset() {
        found = new ArrayList<>();
        timer = 0;
    }

    public void draw(DrawContext ctx, int color, double maxDist) {
        if (found.isEmpty() || !Proj.begin()) return;
        Draw.beginReal(ctx);
        try {
            double max2 = maxDist * maxDist;
            for (BlockPos p : found) {
                if (U.mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > max2) continue;
                double[] r = Proj.rect(new Box(p));
                if (r == null) continue;
                Draw.box(ctx, r[0], r[1], r[2], r[3], color, 2);
            }
        } finally {
            Draw.endReal(ctx);
        }
    }
}
