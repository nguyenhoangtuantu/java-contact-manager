package model;

import java.awt.Color;

/**
 * Enum đại diện cho các nhóm phân loại ưu tiên trong danh bạ.
 */
public enum ContactGroup {
    FAVORITES("Yêu thích", "⭐", new Color(255, 75, 75)),
    FAMILY("Gia đình", "🏠", new Color(75, 200, 110)),
    WORK("Công việc", "💼", new Color(74, 158, 255)),
    FRIENDS("Bạn bè", "👥", new Color(255, 165, 50)),
    OTHER("Khác", "📋", new Color(150, 150, 160));

    private final String displayName;
    private final String icon;
    private final Color color;

    ContactGroup(String displayName, String icon, Color color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public Color getColor() { return color; }

    @Override
    public String toString() {
        return icon + " " + displayName;
    }

    /**
     * Tìm ContactGroup từ tên (case-insensitive).
     */
    public static ContactGroup fromString(String value) {
        if (value == null) return OTHER;
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
