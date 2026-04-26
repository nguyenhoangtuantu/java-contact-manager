import javax.swing.*;

/**
 * Entry point cho ứng dụng Quản lý Danh bạ.
 */
public class Main {
    public static void main(String[] args) {
        // Thiết lập Look and Feel
        try {
            boolean isDark = java.util.prefs.Preferences.userRoot().node("contactmanager").getBoolean("dark_mode",
                    false);
            if (isDark) {
                com.formdev.flatlaf.FlatDarkLaf.setup();
            } else {
                com.formdev.flatlaf.FlatLightLaf.setup();
            }
            ui.UIConstants.applyTheme(isDark);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.Arc", 999);
            UIManager.put("ScrollBar.thumbIarc", 6);
            UIManager.put("ScrollBar.thumbnsets", new java.awt.Insets(2, 2, 2, 2));
        } catch (Exception e) {
            System.err.println("Không thể thiết lập FlatLaf: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // fallback
            }
        }

        // Chạy trên EDT
        SwingUtilities.invokeLater(() -> {
            ui.LoginFrame login = new ui.LoginFrame();
            if (!login.checkSupabaseConfig()) {
                System.exit(0);
            }
            login.setVisible(true);
        });
    }
}
