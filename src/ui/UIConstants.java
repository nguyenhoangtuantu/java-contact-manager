package ui;

import java.awt.*;

/**
 * Hằng số giao diện: màu sắc, font, kích thước cho toàn bộ ứng dụng.
 * Dark theme hiện đại.
 */
public class UIConstants {

    // === COLORS ===
    public static final Color BG_PRIMARY = new Color(18, 18, 24);
    public static final Color BG_SECONDARY = new Color(26, 26, 36);
    public static final Color BG_SIDEBAR = new Color(22, 22, 32);
    public static final Color BG_CARD = new Color(32, 32, 44);
    public static final Color BG_HOVER = new Color(42, 42, 58);
    public static final Color BG_SELECTED = new Color(50, 50, 70);
    public static final Color BG_INPUT = new Color(38, 38, 52);

    public static final Color ACCENT = new Color(74, 158, 255);
    public static final Color ACCENT_HOVER = new Color(100, 175, 255);
    public static final Color ACCENT_DARK = new Color(45, 100, 180);

    public static final Color TEXT_PRIMARY = new Color(230, 230, 240);
    public static final Color TEXT_SECONDARY = new Color(150, 150, 170);
    public static final Color TEXT_MUTED = new Color(100, 100, 120);

    public static final Color BORDER = new Color(55, 55, 75);
    public static final Color BORDER_LIGHT = new Color(70, 70, 90);

    public static final Color SUCCESS = new Color(75, 200, 110);
    public static final Color WARNING = new Color(255, 190, 50);
    public static final Color DANGER = new Color(255, 75, 75);

    public static final Color TABLE_ROW_ALT = new Color(28, 28, 40);

    // === COLORS FOR GROUPS ===
    public static final Color GROUP_FAVORITES = new Color(255, 75, 75);
    public static final Color GROUP_FAMILY = new Color(75, 200, 110);
    public static final Color GROUP_WORK = new Color(74, 158, 255);
    public static final Color GROUP_FRIENDS = new Color(255, 165, 50);
    public static final Color GROUP_OTHER = new Color(150, 150, 160);

    // === FONTS ===
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_ICON = new Font("Segoe UI Emoji", Font.PLAIN, 18);
    public static final Font FONT_SIDEBAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);

    // === DIMENSIONS ===
    public static final int SIDEBAR_WIDTH = 220;
    public static final int WINDOW_WIDTH = 1100;
    public static final int WINDOW_HEIGHT = 720;
    public static final int BORDER_RADIUS = 10;
    public static final int PADDING = 16;
    public static final int PADDING_SMALL = 8;

    // === UTILITY METHODS ===

    /**
     * Tạo màu với alpha (opacity).
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Tạo border compound bo tròn.
     */
    public static javax.swing.border.Border createPaddingBorder(int padding) {
        return javax.swing.BorderFactory.createEmptyBorder(padding, padding, padding, padding);
    }
}
