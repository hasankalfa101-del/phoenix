package dev.phoenix.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Predicate;

/** Small helpers shared by all modules. */
public final class U {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    private U() {}

    public static boolean ready() { return mc.player != null && mc.world != null; }

    public static void chat(String msg) {
        if (mc.inGameHud == null) return;
        mc.inGameHud.getChatHud().addMessage(Text.literal("\u00a7b[Phoenix] \u00a7f" + msg));
    }

    // ---- inventory ----

    /** First hotbar slot (0-8) whose stack matches, or -1. */
    public static int hotbar(Predicate<ItemStack> p) {
        for (int i = 0; i < 9; i++) if (p.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    /** First slot in hotbar + main inventory (0-35) whose stack matches, or -1. */
    public static int inventory(Predicate<ItemStack> p) {
        for (int i = 0; i < 36; i++) if (p.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    public static void select(int slot) {
        if (slot >= 0 && slot < 9) mc.player.getInventory().setSelectedSlot(slot);
    }

    public static int selected() { return mc.player.getInventory().getSelectedSlot(); }

    /** Converts an inventory index (0-35) to a slot id in the player screen handler. */
    public static int screenSlot(int invIndex) { return invIndex < 9 ? invIndex + 36 : invIndex; }

    public static void swapWithOffhand(int invIndex) {
        mc.interactionManager.clickSlot(0, screenSlot(invIndex), 40, SlotActionType.SWAP, mc.player);
    }

    // ---- targeting ----

    public static boolean validTarget(Entity e, boolean players, boolean hostile, boolean passive) {
        if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || e instanceof ArmorStandEntity) return false;
        if (Friends.is(e)) return false;
        if (e instanceof PlayerEntity p) return players && !p.isSpectator();
        if (e instanceof Monster) return hostile;
        return passive;
    }

    public static LivingEntity nearest(double range, boolean players, boolean hostile, boolean passive, boolean walls) {
        LivingEntity best = null;
        double bestDist = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!validTarget(e, players, hostile, passive)) continue;
            double d = mc.player.squaredDistanceTo(e);
            if (d > bestDist) continue;
            if (!walls && !mc.player.canSee(e)) continue;
            best = (LivingEntity) e;
            bestDist = d;
        }
        return best;
    }

    public static Vec3d center(Entity e) {
        Box b = e.getBoundingBox();
        return new Vec3d((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, (b.minZ + b.maxZ) / 2);
    }

    /** {yaw, pitch} needed to look from the player's eyes at the given point. */
    public static float[] rotations(Vec3d to) {
        Vec3d eye = mc.player.getEyePos();
        double dx = to.x - eye.x, dy = to.y - eye.y, dz = to.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, pitch};
    }

    public static void attack(Entity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static float cooldown() { return mc.player.getAttackCooldownProgress(0.5f); }

    public static boolean movingInput() {
        var o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    // ---- misc ----

    public static String keyName(int k) {
        if (k <= 0) return "None";
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F25) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        switch (k) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RAlt";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_INSERT: return "Ins";
            case GLFW.GLFW_KEY_DELETE: return "Del";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PgDn";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps";
            default: break;
        }
        String n = GLFW.glfwGetKeyName(k, 0);
        if (n != null) return n.toUpperCase(Locale.ROOT);
        return "K" + k;
    }
}
