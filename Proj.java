package dev.phoenix.util;

import dev.phoenix.mixin.GameRendererAccessor;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Projects world positions to the screen so ESP-style modules can be drawn as plain 2D HUD graphics.
 * Because nothing touches the world renderer, this works the same with Sodium, Iris and vanilla.
 */
public final class Proj {
    public static int W, H;
    private static Vec3d cam = Vec3d.ZERO;
    private static double fx, fy, fz, rx, rz, ux, uy, uz, tanHalf, aspect;

    private Proj() {}

    /** Call once per frame before using {@link #to}. Returns false when no valid camera is available. */
    public static boolean begin() {
        var mc = U.mc;
        if (mc.player == null || mc.gameRenderer == null) return false;
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) return false;
        cam = camera.getCameraPos();
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double cp = Math.cos(pitch);
        fx = -Math.sin(yaw) * cp;
        fy = -Math.sin(pitch);
        fz = Math.cos(yaw) * cp;
        rx = -Math.cos(yaw);
        rz = -Math.sin(yaw);
        ux = -rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy;
        float tp = mc.getRenderTickCounter().getTickProgress(false);
        float fov = ((GameRendererAccessor) mc.gameRenderer).phoenix$getFov(camera, tp, true);
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
        W = mc.getWindow().getFramebufferWidth();
        H = mc.getWindow().getFramebufferHeight();
        aspect = W / (double) H;
        return true;
    }

    /** Returns {x, y, depth} in framebuffer pixels, or null when the point is behind the camera. */
    public static double[] to(Vec3d p) {
        double dx = p.x - cam.x, dy = p.y - cam.y, dz = p.z - cam.z;
        double z = dx * fx + dy * fy + dz * fz;
        if (z < 0.05) return null;
        double x = dx * rx + dz * rz;
        double y = dx * ux + dy * uy + dz * uz;
        double nx = x / (z * tanHalf * aspect);
        double ny = y / (z * tanHalf);
        return new double[]{W / 2.0 * (1 + nx), H / 2.0 * (1 - ny), z};
    }

    /** Screen rectangle {x1, y1, x2, y2} around a world box, or null if any corner is behind the camera. */
    public static double[] rect(Box b) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double[] p = to(new Vec3d((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ));
            if (p == null) return null;
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }
        return new double[]{minX, minY, maxX, maxY};
    }
}
