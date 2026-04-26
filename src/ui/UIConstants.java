package ui;

import java.awt.*;

/**
 * Hằng số giao diện: Light theme hiện đại.
 * Sử dụng Unicode emoji thay vì Segoe MDL2 Assets để đảm bảo tương thích.
 */
public class UIConstants {

    // === COLORS ===
    public static Color BG_PRIMARY = new Color(243, 244, 246);
    public static Color BG_SECONDARY = new Color(255, 255, 255);
    public static Color BG_SIDEBAR = new Color(255, 255, 255);
    public static Color BG_CARD = new Color(249, 250, 251);
    public static Color BG_HOVER = new Color(239, 246, 255);
    public static Color BG_SELECTED = new Color(219, 234, 254);
    public static Color BG_INPUT = new Color(255, 255, 255);

    public static Color ACCENT = new Color(59, 130, 246);
    public static Color ACCENT_HOVER = new Color(37, 99, 235);
    public static Color ACCENT_DARK = new Color(29, 78, 216);

    public static Color TEXT_PRIMARY = new Color(17, 24, 39);
    public static Color TEXT_SECONDARY = new Color(75, 85, 99);
    public static Color TEXT_MUTED = new Color(156, 163, 175);

    public static Color BORDER = new Color(229, 231, 235);
    public static Color BORDER_LIGHT = new Color(243, 244, 246);

    public static Color SUCCESS = new Color(22, 163, 74);
    public static Color WARNING = new Color(234, 179, 8);
    public static Color DANGER = new Color(220, 38, 38);

    public static Color TABLE_ROW_ALT = new Color(249, 250, 251);

    // === GROUP COLORS ===
    public static final Color GROUP_FAVORITES = new Color(239, 68, 68);
    public static final Color GROUP_FAMILY = new Color(22, 163, 74);
    public static final Color GROUP_WORK = new Color(59, 130, 246);
    public static final Color GROUP_FRIENDS = new Color(234, 88, 12);
    public static final Color GROUP_OTHER = new Color(107, 114, 128);

    // === FONTS ===
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_SIDEBAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_ICON_EMOJI = new Font("Segoe UI Emoji", Font.PLAIN, 14);

    // === ICONS — Sử dụng Unicode/Emoji đại diện ===
    public static final String ICON_CONTACTS = "\uD83D\uDCCB"; // 📋
    public static final String ICON_RECENT = "\uD83D\uDD51"; // 🕑
    public static final String ICON_MERGE = "\uD83D\uDD00"; // 🔀
    public static final String ICON_CLEANUP = "\uD83E\uDDF9"; // 🧹
    public static final String ICON_TRASH = "\uD83D\uDDD1"; // 🗑
    public static final String ICON_ADD = "+";
    public static final String ICON_EDIT = "\u270E"; // ✎
    public static final String ICON_SEARCH = "\uD83D\uDD0D"; // 🔍
    public static final String ICON_USER = "\uD83D\uDC64"; // 👤
    public static final String ICON_LOGOUT = "\u2192"; // →
    public static final String ICON_APP = "\uD83D\uDCDA"; // 📚
    public static final String ICON_SAVE = "\uD83D\uDCBE"; // 💾
    public static final String ICON_CLOCK = "\u23F0"; // ⏰
    public static final String ICON_IMAGE = "\uD83D\uDDBC"; // 🖼
    public static final String ICON_SETTINGS = "\u2699"; // ⚙
    public static final String ICON_RESTORE = "\u21A9"; // ↩
    public static final String ICON_SPARKLE = "\u2728"; // ✨
    public static final String ICON_UPLOAD = "\u2191"; // ↑

    // === DIMENSIONS ===
    public static final int SIDEBAR_WIDTH = 210;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 760;
    public static final int BORDER_RADIUS = 8;
    public static final int PADDING = 16;
    public static final int PADDING_SMALL = 8;
    public static final Font FONT_BOLD = null;

    // === UTILITY ===
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static String encodeColor(Color c) {
        if (c == null)
            return "#000000";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static javax.swing.border.Border createPaddingBorder(int padding) {
        return javax.swing.BorderFactory.createEmptyBorder(padding, padding, padding, padding);
    }
    
    public static void applyTheme(boolean isDark) {
        if (isDark) {
            BG_PRIMARY = new Color(30, 30, 30);
            BG_SECONDARY = new Color(37, 37, 38);
            BG_SIDEBAR = new Color(37, 37, 38);
            BG_CARD = new Color(45, 45, 48);
            BG_HOVER = new Color(63, 63, 70);
            BG_SELECTED = new Color(9, 71, 113);
            BG_INPUT = new Color(60, 60, 60);

            ACCENT = new Color(59, 130, 246);
            ACCENT_HOVER = new Color(96, 165, 250);
            ACCENT_DARK = new Color(147, 197, 253);

            TEXT_PRIMARY = new Color(243, 244, 246);
            TEXT_SECONDARY = new Color(209, 213, 219);
            TEXT_MUTED = new Color(156, 163, 175);

            BORDER = new Color(63, 63, 70);
            BORDER_LIGHT = new Color(45, 45, 48);
            TABLE_ROW_ALT = new Color(45, 45, 48);
        } else {
            BG_PRIMARY = new Color(243, 244, 246);
            BG_SECONDARY = new Color(255, 255, 255);
            BG_SIDEBAR = new Color(255, 255, 255);
            BG_CARD = new Color(249, 250, 251);
            BG_HOVER = new Color(239, 246, 255);
            BG_SELECTED = new Color(219, 234, 254);
            BG_INPUT = new Color(255, 255, 255);

            ACCENT = new Color(59, 130, 246);
            ACCENT_HOVER = new Color(37, 99, 235);
            ACCENT_DARK = new Color(29, 78, 216);

            TEXT_PRIMARY = new Color(17, 24, 39);
            TEXT_SECONDARY = new Color(75, 85, 99);
            TEXT_MUTED = new Color(156, 163, 175);

            BORDER = new Color(229, 231, 235);
            BORDER_LIGHT = new Color(243, 244, 246);
            TABLE_ROW_ALT = new Color(249, 250, 251);
        }
    }
}
