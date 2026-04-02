import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

/**
 * Entry point cho ứng dụng Quản lý Danh bạ.
 */
public class Main {
    public static void main(String[] args) {
        // Thiết lập Look and Feel
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
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
