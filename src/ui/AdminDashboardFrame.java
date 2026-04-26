package ui;

import config.SupabaseConfig;
import service.SupabaseService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AdminDashboardFrame extends JFrame {

    private JPanel listPanel;

    public AdminDashboardFrame() {
        setTitle("Admin Dashboard - Quản lý Hệ thống");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);

        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(UIConstants.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));

        JLabel logo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon("resources/logo.jpg");
            Image img = icon.getImage();
            if (img.getWidth(null) > 0) {
                int origW = img.getWidth(null);
                int origH = img.getHeight(null);
                int newW = 60;
                int newH = (origH * newW) / origW;
                Image scaledImg = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                logo.setIcon(new ImageIcon(scaledImg));
            } else {
                throw new Exception("Ảnh không tải được");
            }
        } catch (Exception e) {
            logo.setText("⚡ Admin Panel");
            logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
            logo.setForeground(UIConstants.TEXT_PRIMARY);
        }
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        sidebar.add(logo, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setOpaque(false);
        menu.add(createMenuBtn("👥 Quản lý người dùng", true));
        sidebar.add(menu, BorderLayout.CENTER);

        JButton logoutBtn = new JButton("Đăng xuất");
        logoutBtn.setFont(UIConstants.FONT_BODY);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(239, 68, 68));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        logoutBtn.addActionListener(e -> logout());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        bottom.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);

        // Center content
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIConstants.BG_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Danh sách tài khoản");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        content.add(title, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UIConstants.BG_PRIMARY);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_PRIMARY);
        content.add(scroll, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createMenuBtn(String text, boolean active) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(active ? UIConstants.BG_SELECTED : UIConstants.BG_SIDEBAR);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        p.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(active ? UIConstants.ACCENT : UIConstants.TEXT_SECONDARY);
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private void loadUsers() {
        listPanel.removeAll();
        JLabel loading = new JLabel("Đang tải dữ liệu...");
        loading.setForeground(UIConstants.TEXT_MUTED);
        listPanel.add(loading);
        listPanel.revalidate();
        listPanel.repaint();

        new SwingWorker<com.google.gson.JsonArray, Void>() {
            @Override
            protected com.google.gson.JsonArray doInBackground() throws Exception {
                return SupabaseService.getInstance().getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    com.google.gson.JsonArray users = get();
                    listPanel.removeAll();
                    for (com.google.gson.JsonElement el : users) {
                        com.google.gson.JsonObject u = el.getAsJsonObject();
                        String uid = getStr(u, "id");
                        String uEmail = getStr(u, "email");
                        String uName = getStr(u, "display_name");
                        String role = getStr(u, "role");

                        JPanel item = new JPanel(new BorderLayout());
                        item.setBackground(UIConstants.BG_SECONDARY);
                        item.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(UIConstants.BORDER, 1, true),
                                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

                        String infoStr = uEmail;
                        if (uName != null && !uName.isBlank())
                            infoStr = uName + " (" + uEmail + ")";

                        JPanel infoWrap = new JPanel(new GridLayout(2, 1, 0, 5));
                        infoWrap.setOpaque(false);

                        JLabel nameLbl = new JLabel(infoStr);
                        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                        nameLbl.setForeground(UIConstants.TEXT_PRIMARY);

                        JLabel roleLbl = new JLabel(
                                "Vai trò: " + ("admin".equals(role) ? "QUẢN TRỊ VIÊN" : "Người dùng"));
                        roleLbl.setFont(UIConstants.FONT_SMALL);
                        roleLbl.setForeground("admin".equals(role) ? new Color(239, 68, 68) : UIConstants.TEXT_MUTED);

                        infoWrap.add(nameLbl);
                        infoWrap.add(roleLbl);

                        JButton resetBtn = new JButton("Reset Mật Khẩu");
                        resetBtn.setFont(UIConstants.FONT_SMALL_BOLD);
                        resetBtn.setForeground(Color.WHITE);
                        resetBtn.setBackground(new Color(239, 68, 68));
                        resetBtn.setFocusPainted(false);
                        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        if ("admin".equals(role) && !uid.equals(SupabaseConfig.getInstance().getCurrentUserId())) {
                            resetBtn.setEnabled(false);
                        }

                        resetBtn.addActionListener(e -> resetPasswordFor(uid, uEmail));

                        item.add(infoWrap, BorderLayout.CENTER);
                        item.add(resetBtn, BorderLayout.EAST);
                        listPanel.add(item);
                        listPanel.add(Box.createVerticalStrut(15));
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Lỗi tải danh sách: " + ex.getMessage());
                    err.setForeground(Color.RED);
                    listPanel.add(err);
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            }
        }.execute();
    }

    private void resetPasswordFor(String uid, String email) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn tự động tạo mật khẩu mới cho tài khoản " + email + "?",
                "Xác nhận Reset", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            String newPass = "Aa@" + (100000 + new java.util.Random().nextInt(900000));
            try {
                SupabaseService.getInstance().adminResetUserPassword(uid, newPass);
                JTextArea ta = new JTextArea(
                        "Đã reset mật khẩu thành công!\n\nMật khẩu mới: " + newPass + "\n\nHãy copy và gửi cho họ.");
                ta.setEditable(false);
                JOptionPane.showMessageDialog(this, ta, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            SupabaseConfig.getInstance().clearAuthSession();
            dispose();
            LoginFrame login = new LoginFrame();
            if (login.checkSupabaseConfig())
                login.setVisible(true);
        }
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
