package dev.phoenix.modules;

import dev.phoenix.mixin.ClientPlayerInteractionManagerAccessor;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.module.ModuleManager;
import dev.phoenix.module.Stub;
import dev.phoenix.setting.BoolSetting;
import dev.phoenix.setting.NumSetting;
import dev.phoenix.util.BlockFinder;
import dev.phoenix.util.Friends;
import dev.phoenix.util.Scan;
import dev.phoenix.util.U;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public final class Donut {
    private Donut() {}

    public static void register(ModuleManager m) {
        m.add(new Stub("Anti Vanish", Category.DONUT));
        m.add(new PlayerDetection());
        m.add(new Stub("Auto Relog", Category.DONUT));
        m.add(new Stub("Anti Trap", Category.DONUT));
        m.add(new Stub("Air Stuck", Category.DONUT));
        m.add(new Stub("Spawner Protect", Category.DONUT));
        m.add(new Stub("Staff List", Category.DONUT));
        m.add(new NetheriteFinder());
        m.add(new Stub("Trident Util", Category.DONUT));
        m.add(new DonutNoFall());
        m.add(new DonutSpeedMine());
        m.add(new Stub("Fake Stats", Category.DONUT));
        m.add(new SpawnerNotifier());
    }

    static class PlayerDetection extends Module {
        final BoolSetting ignoreFriends = add(new BoolSetting("Ignore Friends", true));
        Set<String> seen = new HashSet<>();
        int timer;

        PlayerDetection() { super("Player Detection", Category.DONUT, "Alerts you in chat when a player comes into range."); }

        @Override protected void onEnable() { seen.clear(); }

        @Override public void onTick() {
            if (++timer % 10 != 0) return;
            Set<String> now = new HashSet<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                if (ignoreFriends.get() && Friends.is(p)) continue;
                String name = p.getName().getString();
                now.add(name);
                if (!seen.contains(name)) U.chat(name + " is nearby (" + Math.round(mc.player.distanceTo(p)) + " blocks).");
            }
            seen = now;
        }
    }

    static class NetheriteFinder extends Module {
        final NumSetting radius = add(new NumSetting("Chunk Radius", 5, 1, 8, 1));
        final BlockFinder finder = new BlockFinder();
        final Set<Block> targets = Set.of(Blocks.ANCIENT_DEBRIS);

        NetheriteFinder() { super("Netherite Finder", Category.DONUT, "Boxes ancient debris in loaded chunks."); }

        @Override protected void onDisable() { finder.reset(); }

        @Override public void onTick() { finder.update(targets, radius.getInt(), 40); }

        @Override public void onHud(DrawContext ctx, float tp) { finder.draw(ctx, 0xFFB5651D, 256); }
    }

    static class DonutNoFall extends Module {
        DonutNoFall() { super("Donut NoFall", Category.DONUT, "Prevents fall damage by spoofing the on-ground flag."); }

        @Override public void onTick() {
            if (mc.player.fallDistance > 2.0 && !mc.player.isOnGround()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            }
        }
    }

    static class DonutSpeedMine extends Module {
        final NumSetting boost = add(new NumSetting("Boost", 1.5, 1.0, 4.0, 0.1));

        DonutSpeedMine() { super("Donut Speed Mine", Category.DONUT, "Mines blocks faster."); }

        @Override public void onTick() {
            if (mc.interactionManager == null) return;
            var im = (ClientPlayerInteractionManagerAccessor) mc.interactionManager;
            im.phoenix$setBlockBreakingCooldown(0);
            if (mc.options.attackKey.isPressed()) {
                float p = im.phoenix$getBreakingProgress();
                if (p > 0f && p < 1f) im.phoenix$setBreakingProgress(Math.min(0.999f, p + (boost.getF() - 1f) * 0.05f));
            }
        }
    }

    static class SpawnerNotifier extends Module {
        final Set<BlockPos> known = new HashSet<>();

        SpawnerNotifier() { super("Spawner Notifier", Category.DONUT, "Tells you in chat when spawners load in."); }

        @Override protected void onEnable() { known.clear(); }

        @Override public void onTick() {
            for (BlockEntity be : Scan.blockEntities()) {
                if (!(be instanceof MobSpawnerBlockEntity)) continue;
                BlockPos p = be.getPos();
                if (known.add(p.toImmutable())) {
                    U.chat("Spawner at " + p.getX() + " " + p.getY() + " " + p.getZ()
                        + " (" + Math.round(Math.sqrt(mc.player.squaredDistanceTo(p.getX(), p.getY(), p.getZ()))) + " blocks).");
                }
            }
        }
    }
}
