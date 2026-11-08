package dev.phoenix.gui;

import dev.phoenix.PhoenixClient;
import dev.phoenix.config.Config;
import dev.phoenix.module.Category;
import dev.phoenix.module.Module;
import dev.phoenix.setting.*;
import dev.phoenix.util.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The click GUI. Layout is designed for a 1920 px wide window and scaled to fit, so it looks the same at any
 * GUI scale. Everything is drawn with plain rectangles and text (no custom render pipelines).
 */
public class ClickGui extends Screen {
    private static final float DESIGN_W = 1920f;
    private static final float PANEL_W = 212, PITCH = 226, X0 = 53, TOP = 20;
    private static final float HEADER_H = 41, ROW_H = 32, PAD_BOTTOM = 5;
    private static final int MAX_ROWS = 19;
    private static final float TEXT = 1.5f, TITLE = 1.6f;
    private static final String[] TABS = {"Modules", "HUD", "Friends", "Theme", "Config"};
    private static final float[] TAB_CX = {-154, -76, -1, 80, 158};

    private static final int K_HEADER = 0, K_ITEM = 1, K_TAB = 2, K_SEARCH = 3;
    private static final int T_MODULE = 0, T_BIND = 1, T_HOLD = 2, T_SETTING = 3, T_BUTTON = 4, T_INPUT = 5;

    private static int tab = 0;

    private static class Panel {
        Category cat;
        String title;
        float x, y, w = PANEL_W;
        int scroll;
        boolean collapsed;
    }

    private static class Item {
        int type;
        Module m;
        Setting<?> s;
        String label = "", right = "";
        Runnable action;
        int chip;
        boolean on;
    }

    private static class Hit {
        float x1, y1, x2, y2;
        int kind, index;
        Panel panel;
        Item item;
        boolean contains(float x, float y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }

    private final List<Panel> panels = new ArrayList<>();
    private final Panel tabPanel = new Panel();
    private final List<Hit> hits = new ArrayList<>();

    private float s = 1, u = 1, vh = 1080;
    private float mx, my;

    private Panel dragPanel;
    private float dragOffX, dragOffY;
    private NumSetting dragNum;
    private float dragNumX1, dragNumX2;
    private Module bindTarget;
    private StrSetting editStr;
    private String query = "";
    private boolean searchFocus;
    private String friendInput = "";
    private boolean friendFocus;

    public ClickGui() {
        super(Text.literal("Phoenix"));
        int i = 0;
        for (Category c : Category.values()) {
            Panel p = new Panel();
            p.cat = c;
            p.title = c.title;
            p.x = X0 + i * PITCH;
            p.y = i == 0 ? TOP - 10 : TOP;
            float[] saved = Config.panels.get(c.title);
            if (saved != null) {
                p.x = saved[0];
                p.y = saved[1];
            }
            panels.add(p);
            i++;
        }
        tabPanel.w = 320;
        tabPanel.x = (DESIGN_W - tabPanel.w) / 2f;
        tabPanel.y = 110;
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void close() {
        Config.save();
        super.close();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int fbW = client.getWindow().getFramebufferWidth();
        int fbH = client.getWindow().getFramebufferHeight();
        int gs = client.getWindow().getScaleFactor();
        u = Math.max(0.4f, fbW / DESIGN_W);
        s = u / gs;
        vh = fbH / u;
        mx = mouseX / s;
        my = mouseY / s;
        hits.clear();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(s, s);

        if (tab == 0) {
            Panel clientPanel = null;
            for (Panel p : panels) {
                renderPanel(ctx, p, moduleItems(p));
                if (p.cat == Category.CLIENT) clientPanel = p;
            }
            if (clientPanel != null) renderSearch(ctx, clientPanel);
        } else {
            tabPanel.title = TABS[tab];
            renderPanel(ctx, tabPanel, tabItems());
        }
        renderTabs(ctx);

        ctx.getMatrices().popMatrix();
    }

    private List<Item> moduleItems(Panel p) {
        List<Item> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Module m : PhoenixClient.modules.in(p.cat)) {
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            addModule(out, m);
        }
        return out;
    }

    private void addModule(List<Item> out, Module m) {
        Item it = new Item();
        it.type = T_MODULE;
        it.m = m;
        it.label = m.name;
        it.right = m.key > 0 ? U.keyName(m.key) : "";
        it.on = m.enabled;
        out.add(it);
        if (!m.expanded) return;
        Item b = new Item();
        b.type = T_BIND;
        b.m = m;
        b.label = "Bind";
        b.right = bindTarget == m ? "press a key..." : U.keyName(m.key);
        out.add(b);
        Item h = new Item();
        h.type = T_HOLD;
        h.m = m;
        h.label = "Hold to enable";
        h.right = m.holdMode ? "On" : "Off";
        out.add(h);
        for (Setting<?> st : m.settings) addSetting(out, m, st);
    }

    private void addSetting(List<Item> out, Module m, Setting<?> st) {
        Item it = new Item();
        it.type = T_SETTING;
        it.m = m;
        it.s = st;
        it.label = st.name;
        it.right = st.display();
        if (st instanceof StrSetting && st == editStr) it.right = st.get() + ((System.currentTimeMillis() / 400) % 2 == 0 ? "|" : "");
        out.add(it);
    }

    private List<Item> tabItems() {
        List<Item> out = new ArrayList<>();
        if (tab == 1) {
            for (String n : new String[]{"HUD", "Radar", "Hotkeys"}) {
                Module m = PhoenixClient.modules.byName(n);
                if (m != null) addModule(out, m);
            }
            Module hud = PhoenixClient.modules.byName("HUD");
            if (hud != null) for (Setting<?> st : hud.settings) addSetting(out, hud, st);
        } else if (tab == 2) {
            Item in = new Item();
            in.type = T_INPUT;
            in.label = friendInput.isEmpty() ? "Type a name + Enter" : friendInput + (friendFocus && (System.currentTimeMillis() / 400) % 2 == 0 ? "|" : "");
            out.add(in);
            Item near = new Item();
            near.type = T_BUTTON;
            near.label = "Add nearest player";
            near.action = () -> {
                if (!U.ready()) return;
                PlayerEntity best = null;
                double bd = Double.MAX_VALUE;
                for (PlayerEntity p : U.mc.world.getPlayers()) {
                    if (p == U.mc.player) continue;
                    double d = U.mc.player.squaredDistanceTo(p);
                    if (d < bd) { bd = d; best = p; }
                }
                if (best != null) Friends.add(best.getName().getString());
            };
            out.add(near);
            for (String n : new ArrayList<>(Friends.names)) {
                Item f = new Item();
                f.type = T_BUTTON;
                f.label = n;
                f.right = "Remove";
                f.action = () -> Friends.remove(n);
                out.add(f);
            }
        } else if (tab == 3) {
            for (int i = 0; i < Theme.PRESETS.length; i++) {
                Item t = new Item();
                t.type = T_BUTTON;
                t.label = Theme.NAMES[i];
                t.chip = Theme.PRESETS[i];
                t.right = Theme.accent == Theme.PRESETS[i] ? "Selected" : "";
                final int col = Theme.PRESETS[i];
                t.action = () -> Theme.accent = col;
                out.add(t);
            }
        } else if (tab == 4) {
            out.add(button("Save config", () -> { Config.save(); U.chat("Config saved."); }));
            out.add(button("Load config", () -> { Config.load(); U.chat("Config loaded."); }));
            out.add(button("Reset everything", () -> { Config.reset(); U.chat("Config reset."); }));
        }
        return out;
    }

    private static Item button(String label, Runnable r) {
        Item b = new Item();
        b.type = T_BUTTON;
        b.label = label;
        b.action = r;
        return b;
    }

    private void renderPanel(DrawContext ctx, Panel p, List<Item> items) {
        int total = items.size();
        int maxScroll = Math.max(0, total - MAX_ROWS);
        p.scroll = Math.max(0, Math.min(p.scroll, maxScroll));
        int visible = p.collapsed ? 0 : Math.min(total, MAX_ROWS);
        float h = HEADER_H + (visible > 0 ? visible * ROW_H + PAD_BOTTOM : 0);
        float x = p.x, y = p.y;

        Draw.round(ctx, x, y, x + p.w, y + h, 10, Theme.PANEL);

        // header
        Draw.text(ctx, p.title, x + 16, y + 14, Theme.WHITE, TITLE);
        float tw = Draw.width(p.title, TITLE);
        if (p.collapsed) Draw.chevronDown(ctx, x + 16 + tw + 12, y + 18, Theme.TEXT_DIM);
        else Draw.chevronDown(ctx, x + 16 + tw + 12, y + 19, Theme.TEXT);
        if (p.cat != null) Draw.icon(ctx, p.cat.ordinal(), x + p.w - 30, y + 13, 2, Theme.TEXT);
        Hit hh = new Hit();
        hh.x1 = x; hh.y1 = y; hh.x2 = x + p.w; hh.y2 = y + HEADER_H;
        hh.kind = K_HEADER; hh.panel = p;
        hits.add(hh);

        for (int i = 0; i < visible; i++) {
            Item it = items.get(i + p.scroll);
            float ry = y + HEADER_H + i * ROW_H;
            renderItem(ctx, p, it, x + 6, ry, x + p.w - 6, ry + ROW_H);
        }

        if (total > MAX_ROWS && !p.collapsed) {
            float trackY = y + HEADER_H;
            float trackH = visible * ROW_H;
            float thumbH = Math.max(24, trackH * MAX_ROWS / total);
            float thumbY = trackY + (trackH - thumbH) * (maxScroll == 0 ? 0 : p.scroll / (float) maxScroll);
            Draw.rect(ctx, x + p.w - 4, thumbY, x + p.w - 2, thumbY + thumbH, 0xFF4A5363);
        }
    }

    private void renderItem(DrawContext ctx, Panel p, Item it, float x1, float y1, float x2, float y2) {
        boolean hover = mx >= x1 && mx <= x2 && my >= y1 && my < y2;
        boolean sub = it.type == T_BIND || it.type == T_HOLD || (it.type == T_SETTING);
        int bg = 0;
        if (it.type == T_MODULE && it.on) bg = Theme.ROW_ON;
        else if (sub) bg = Theme.ROW_SUB;
        if (hover && bg == 0) bg = Theme.ROW_HOVER;
        else if (hover && it.type == T_MODULE) bg = 0xFF303848;
        if (bg != 0) Draw.round(ctx, x1, y1, x2, y2 - 1, 6, bg);

        float indent = sub ? 12 : 10;
        int col = it.type == T_MODULE ? (it.on ? Theme.WHITE : Theme.TEXT) : (sub ? Theme.TEXT_DIM : Theme.TEXT);
        float tx = x1 + indent;
        if (it.chip != 0) {
            Draw.round(ctx, tx, y1 + 9, tx + 14, y1 + 23, 3, it.chip);
            tx += 22;
        }
        float rightW = it.right.isEmpty() ? 0 : Draw.width(it.right, 1.3f);
        String label = fit(it.label, (x2 - x1) - indent - rightW - 24, TEXT);
        Draw.text(ctx, label, tx, y1 + 10, col, TEXT);
        if (!it.right.isEmpty()) {
            String r = fit(it.right, (x2 - x1) - 80, 1.3f);
            float rw = Draw.width(r, 1.3f);
            int rc = it.type == T_SETTING && it.s instanceof BoolSetting b ? (b.get() ? Theme.accent : Theme.TEXT_DIM) : Theme.TEXT_DIM;
            Draw.text(ctx, r, x2 - 10 - rw, y1 + 11, rc, 1.3f);
        }
        if (it.type == T_SETTING && it.s instanceof NumSetting n) {
            float bx1 = x1 + 12, bx2 = x2 - 12, by = y2 - 7;
            Draw.rect(ctx, bx1, by, bx2, by + 2, 0xFF2B3340);
            Draw.rect(ctx, bx1, by, bx1 + (bx2 - bx1) * (float) n.fraction(), by + 2, Theme.accent);
        }
        if (it.type == T_INPUT) {
            Draw.round(ctx, x1, y1 + 2, x2, y2 - 3, 6, Theme.INPUT);
        }

        Hit h = new Hit();
        h.x1 = x1; h.y1 = y1; h.x2 = x2; h.y2 = y2;
        h.kind = K_ITEM; h.panel = p; h.item = it;
        hits.add(h);
    }

    private void renderSearch(DrawContext ctx, Panel clientPanel) {
        int rows = clientPanel.collapsed ? 0 : Math.min(PhoenixClient.modules.in(Category.CLIENT).size(), MAX_ROWS);
        float ch = HEADER_H + (rows > 0 ? rows * ROW_H + PAD_BOTTOM : 0);
        float x = clientPanel.x, y = clientPanel.y + ch + 14;
        float w = clientPanel.w, h = 76;
        Draw.round(ctx, x, y, x + w, y + h, 10, Theme.PANEL);
        Draw.text(ctx, "Search", x + 13, y + 16, Theme.WHITE, 1.5f);
        Draw.icon(ctx, 8, x + w - 30, y + 14, 2, Theme.TEXT);
        float ix1 = x + 7, iy1 = y + 39, ix2 = x + w - 9, iy2 = y + 68;
        Draw.round(ctx, ix1, iy1, ix2, iy2, 6, searchFocus ? 0xFF1B2330 : 0xFF222936);
        if (query.isEmpty() && !searchFocus) {
            Draw.text(ctx, "Search modules...", ix1 + 10, iy1 + 9, Theme.TEXT_DIM, 1.4f);
        } else {
            String shown = query + (searchFocus && (System.currentTimeMillis() / 400) % 2 == 0 ? "|" : "");
            Draw.text(ctx, fit(shown, ix2 - ix1 - 20, 1.4f), ix1 + 10, iy1 + 9, Theme.TEXT, 1.4f);
        }
        Hit hit = new Hit();
        hit.x1 = ix1; hit.y1 = iy1; hit.x2 = ix2; hit.y2 = iy2;
        hit.kind = K_SEARCH;
        hits.add(hit);
    }

    private void renderTabs(DrawContext ctx) {
        float cx = DESIGN_W / 2f - 2;
        float px1 = cx - 202.5f, px2 = cx + 202.5f, py1 = vh - 67, py2 = vh - 24;
        Draw.round(ctx, px1 - 1, py1 - 1, px2 + 1, py2 + 1, 15, Theme.PILL_BORDER);
        Draw.round(ctx, px1, py1, px2, py2, 14, Theme.PILL);
        for (int i = 0; i < TABS.length; i++) {
            float c = cx + TAB_CX[i];
            float tw = Draw.width(TABS[i], TEXT);
            float x1 = c - (tw + 32) / 2f, x2 = c + (tw + 32) / 2f;
            float y1 = py1 + 4, y2 = py2 - 4;
            if (i == tab) Draw.round(ctx, x1, y1, x2, y2, 10, Theme.accent);
            Draw.text(ctx, TABS[i], c - tw / 2f, y1 + 12, i == tab ? Theme.WHITE : 0xFFC9CFD8, TEXT);
            Hit h = new Hit();
            h.x1 = x1; h.y1 = y1; h.x2 = x2; h.y2 = y2;
            h.kind = K_TAB; h.index = i;
            hits.add(h);
        }
    }

    private static String fit(String str, float maxW, float scale) {
        if (Draw.width(str, scale) <= maxW) return str;
        String t = str;
        while (t.length() > 1 && Draw.width(t + "..", scale) > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    // ------------------------------------------------------------------ input

    private float lx(Click c) { return (float) (c.x() / s); }

    private float ly(Click c) { return (float) (c.y() / s); }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float cx = lx(click), cy = ly(click);
        int btn = click.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(cx, cy)) continue;
            clearFocus();
            handle(h, btn, cx, cy);
            return true;
        }
        clearFocus();
        return super.mouseClicked(click, doubled);
    }

    private void clearFocus() {
        searchFocus = false;
        friendFocus = false;
        editStr = null;
        bindTarget = null;
    }

    private void handle(Hit h, int btn, float cx, float cy) {
        switch (h.kind) {
            case K_HEADER -> {
                if (btn == 0) {
                    dragPanel = h.panel;
                    dragOffX = cx - h.panel.x;
                    dragOffY = cy - h.panel.y;
                    if (panels.remove(h.panel)) panels.add(h.panel);
                } else if (btn == 1) {
                    h.panel.collapsed = !h.panel.collapsed;
                }
            }
            case K_TAB -> tab = h.index;
            case K_SEARCH -> searchFocus = true;
            case K_ITEM -> handleItem(h, btn, cx);
            default -> {}
        }
    }

    private void handleItem(Hit h, int btn, float cx) {
        Item it = h.item;
        switch (it.type) {
            case T_MODULE -> {
                if (btn == 0) it.m.toggle();
                else if (btn == 1) it.m.expanded = !it.m.expanded;
                else if (btn == 2) bindTarget = it.m;
            }
            case T_BIND -> {
                if (btn == 0) bindTarget = it.m;
                else if (btn == 1) it.m.key = -1;
            }
            case T_HOLD -> it.m.holdMode = !it.m.holdMode;
            case T_SETTING -> {
                Setting<?> st = it.s;
                if (st instanceof BoolSetting b) b.toggle();
                else if (st instanceof EnumSetting e) e.cycle(btn == 1 ? -1 : 1);
                else if (st instanceof NumSetting n) {
                    dragNum = n;
                    dragNumX1 = h.x1 + 12;
                    dragNumX2 = h.x2 - 12;
                    setFromMouse(cx);
                } else if (st instanceof StrSetting str) editStr = str;
            }
            case T_BUTTON -> {
                if (it.action != null) it.action.run();
            }
            case T_INPUT -> friendFocus = true;
            default -> {}
        }
    }

    private void setFromMouse(float cx) {
        if (dragNum == null) return;
        double f = Math.max(0, Math.min(1, (cx - dragNumX1) / (dragNumX2 - dragNumX1)));
        dragNum.set(dragNum.min + f * (dragNum.max - dragNum.min));
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        float cx = lx(click), cy = ly(click);
        if (dragPanel != null) {
            dragPanel.x = cx - dragOffX;
            dragPanel.y = cy - dragOffY;
            if (dragPanel.cat != null) Config.panels.put(dragPanel.title, new float[]{dragPanel.x, dragPanel.y});
            return true;
        }
        if (dragNum != null) {
            setFromMouse(cx);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragPanel = null;
        dragNum = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float x = (float) (mouseX / s), y = (float) (mouseY / s);
        int dir = verticalAmount > 0 ? -1 : 1;
        List<Panel> list = new ArrayList<>(panels);
        if (tab != 0) { list.clear(); list.add(tabPanel); }
        for (int i = list.size() - 1; i >= 0; i--) {
            Panel p = list.get(i);
            float h = HEADER_H + MAX_ROWS * ROW_H + PAD_BOTTOM;
            if (x >= p.x && x <= p.x + p.w && y >= p.y && y <= p.y + h) {
                p.scroll += dir;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (bindTarget != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                // cancel
            } else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                bindTarget.key = -1;
            } else {
                bindTarget.key = k;
            }
            bindTarget = null;
            return true;
        }
        if (editStr != null) {
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_ESCAPE) editStr = null;
            else if (k == GLFW.GLFW_KEY_BACKSPACE && !editStr.get().isEmpty()) editStr.value = editStr.get().substring(0, editStr.get().length() - 1);
            return true;
        }
        if (searchFocus) {
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                if (query.isEmpty()) searchFocus = false; else query = "";
            } else if (k == GLFW.GLFW_KEY_ENTER) searchFocus = false;
            else if (k == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) query = query.substring(0, query.length() - 1);
            return true;
        }
        if (friendFocus) {
            if (k == GLFW.GLFW_KEY_ENTER) {
                Friends.add(friendInput);
                friendInput = "";
            } else if (k == GLFW.GLFW_KEY_ESCAPE) friendFocus = false;
            else if (k == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) friendInput = friendInput.substring(0, friendInput.length() - 1);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!input.isValidChar()) return false;
        String ch = input.asString();
        if (editStr != null) {
            if (editStr.get().length() < 120) editStr.value = editStr.get() + ch;
            return true;
        }
        if (searchFocus) {
            if (query.length() < 30) query += ch;
            return true;
        }
        if (friendFocus) {
            if (friendInput.length() < 16) friendInput += ch;
            return true;
        }
        return false;
    }
}
