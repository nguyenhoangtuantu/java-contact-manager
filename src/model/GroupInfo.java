package model;

import java.awt.Color;

/**
 * Model đại diện cho một nhóm ưu tiên (từ bảng contact_groups).
 * Hỗ trợ nhóm động, không giới hạn.
 */
public class GroupInfo {
    private int id;
    private String name;           // Tên hệ thống: FAVORITES, FAMILY, CUSTOM_1, ...
    private String displayName;    // Tên hiển thị: Yêu thích, Gia đình, ...
    private String icon;           // Icon emoji: ⭐, 🏠, ...
    private String colorHex;       // Mã màu hex: #FF4B4B
    private String description;    // Mô tả

    public GroupInfo() {}

    public GroupInfo(String displayName, String icon, String colorHex) {
        this.displayName = displayName;
        this.icon = icon;
        this.colorHex = colorHex;
        // Tự tạo name từ displayName
        this.name = "CUSTOM_" + System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getIcon() {
        if (icon == null) return "";
        return icon;
    }

    public void setIcon(String icon) { this.icon = icon; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /**
     * Chuyển hex color thành Color object.
     */
    public Color getColor() {
        if (colorHex == null || colorHex.isBlank()) return new Color(150, 150, 160);
        try {
            return Color.decode(colorHex);
        } catch (Exception e) {
            return new Color(150, 150, 160);
        }
    }

    @Override
    public String toString() {
        return (icon != null ? icon + " " : "") + displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((GroupInfo) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
