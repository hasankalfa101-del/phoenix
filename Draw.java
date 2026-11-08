package dev.phoenix.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/** 2D drawing helpers built only on DrawContext.fill / drawText so they work with Sodium and shaders. */
public final class Draw {
    private Draw() {}

    public static TextRenderer tr() { return U.mc.textRenderer; }

    public static void rect(DrawContext c, float x1, float y1, float x2, float y2, int col) {
        c.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), col);
    }

    public static void round(DrawContext c, float x1, float y1, float x2, float y2, int r, int col) {
        int ix1 = Math.round(x1), iy1 = Math.round(y1), ix2 = Math.round(x2), iy2 = Math.round(y2);
        r = Math.min(r, Math.min(ix2 - ix1, iy2 - iy1) / 2);
        if (r <= 0) {
            c.fill(ix1, iy1, ix2, iy2, col);
            return;
        }
        c.fill(ix1, iy1 + r, ix2, iy2 - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(ix1 + inset, iy1 + i, ix2 - inset, iy1 + i + 1, col);
            c.fill(ix1 + inset, iy2 - i - 1, ix2 - inset, iy2 - i, col);
        }
    }

    public static void text(DrawContext c, String s, float x, float y, int color, float scale) {
        Matrix3x2fStack m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        c.drawText(tr(), s, 0, 0, color, false);
        m.popMatrix();
    }

    public static float width(String s, float scale) { return tr().getWidth(s) * scale; }

    public static void box(DrawContext c, double x1, double y1, double x2, double y2, int col, int t) {
        int a = (int) Math.round(x1), b = (int) Math.round(y1), d = (int) Math.round(x2), e = (int) Math.round(y2);
        c.fill(a, b, d, b + t, col);
        c.fill(a, e - t, d, e, col);
        c.fill(a, b, a + t, e, col);
        c.fill(d - t, b, d, e, col);
    }

    public static void line(DrawContext c, double x1, double y1, double x2, double y2, int col, int t) {
        int ax = (int) Math.round(x1), ay = (int) Math.round(y1), bx = (int) Math.round(x2), by = (int) Math.round(y2);
        int dx = Math.abs(bx - ax), dy = Math.abs(by - ay);
        if (dx > 5000 || dy > 5000) return;
        if (dx == 0 && dy == 0) {
            c.fill(ax, ay, ax + t, ay + t, col);
            return;
        }
        if (dx >= dy) {
            int sx = ax < bx ? 1 : -1;
            double slope = (double) (by - ay) / (bx - ax);
            int runStart = ax;
            int curY = ay;
            for (int i = 0; i <= dx; i++) {
                int x = ax + sx * i;
                int y = (int) Math.round(ay + slope * (x - ax));
                if (y != curY) {
                    int end = x - sx;
                    c.fill(Math.min(runStart, end), curY, Math.max(runStart, end) + t, curY + t, col);
                    runStart = x;
                    curY = y;
                }
            }
            c.fill(Math.min(runStart, bx), curY, Math.max(runStart, bx) + t, curY + t, col);
        } else {
            int sy = ay < by ? 1 : -1;
            double slope = (double) (bx - ax) / (by - ay);
            int runStart = ay;
            int curX = ax;
            for (int i = 0; i <= dy; i++) {
                int y = ay + sy * i;
                int x = (int) Math.round(ax + slope * (y - ay));
                if (x != curX) {
                    int end = y - sy;
                    c.fill(curX, Math.min(runStart, end), curX + t, Math.max(runStart, end) + t, col);
                    runStart = y;
                    curX = x;
                }
            }
            c.fill(curX, Math.min(runStart, by), curX + t, Math.max(runStart, by) + t, col);
        }
    }

    /** Switches drawing to real framebuffer pixels (1 unit = 1 screen pixel). Must be paired with endReal. */
    public static void beginReal(DrawContext c) {
        c.getMatrices().pushMatrix();
        float s = 1f / U.mc.getWindow().getScaleFactor();
        c.getMatrices().scale(s, s);
    }

    public static void endReal(DrawContext c) { c.getMatrices().popMatrix(); }

    public static void chevronDown(DrawContext c, float cx, float cy, int col) {
        int x = Math.round(cx), y = Math.round(cy);
        for (int i = 0; i < 4; i++) {
            c.fill(x - 4 + i, y + i, x - 4 + i + 2, y + i + 1, col);
            c.fill(x + 3 - i - 1, y + i, x + 3 - i + 1, y + i + 1, col);
        }
    }

    public static void chevronUp(DrawContext c, float cx, float cy, int col) {
        int x = Math.round(cx), y = Math.round(cy);
        for (int i = 0; i < 4; i++) {
            c.fill(x - 4 + i, y + 3 - i, x - 4 + i + 2, y + 4 - i, col);
            c.fill(x + 3 - i - 1, y + 3 - i, x + 3 - i + 1, y + 4 - i, col);
        }
    }

    private static final String[][] ICONS = {
        // combat: crosshair
        {"...#...", ".##.##.", ".#...#.", "##.#.##", ".#...#.", ".##.##.", "...#..."},
        // misc: grid
        {".......", ".##.##.", ".##.##.", ".......", ".##.##.", ".##.##.", "......."},
        // donut: ring
        {"..###..", ".#####.", "##...##", "##...##", "##...##", ".#####.", "..###.."},
        // render: eye
        {".......", ".#####.", "##.#.##", "#.###.#", "##.#.##", ".#####.", "......."},
        // client: ankh
        {"..###..", "..#.#..", "..###..", "...#...", ".#####.", "...#...", "...#..."},
        // movement: chevron up
        {".......", "...#...", "..#.#..", ".#...#.", "#.....#", ".......", "......."},
        // base raid: house
        {"...#...", "..###..", ".#####.", "#######", ".##.##.", ".#...#.", ".#...#."},
        // visuals: monitor
        {"#######", "#.....#", "#.....#", "#.....#", "#######", "..###..", ".#####."},
        // search: magnifier
        {".###...", "#...#..", "#...#..", "#...#..", ".###...", ".....#.", "......#"},
    };

    public static void icon(DrawContext c, int index, float x, float y, int cell, int col) {
        String[] rows = ICONS[index];
        int ix = Math.round(x), iy = Math.round(y);
        for (int r = 0; r < rows.length; r++) {
            for (int k = 0; k < rows[r].length(); k++) {
                if (rows[r].charAt(k) == '#') {
                    c.fill(ix + k * cell, iy + r * cell, ix + (k + 1) * cell, iy + (r + 1) * cell, col);
                }
            }
        }
    }
}
