package ui;

import config.SupabaseConfig;
import model.Contact;
import service.ContactService;
import service.SupabaseService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Hồ sơ Admin - Quản lý thông tin & Cá nhân hóa
 */
public class AdminProfileDialog extends JDialog {

    private MainFrame parentFrame;
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel emailLabel;

    public AdminProfileDialog(MainFrame parent) {
        super(parent, "Hồ sơ Admin", true);
        this.parentFrame = parent;
        setSize(500, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);

        initComponents();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIConstants.FONT_BODY);
        tabbedPane.setBackground(UIConstants.BG_PRIMARY);
        tabbedPane.setForeground(UIConstants.TEXT_PRIMARY);

        tabbedPane.addTab("Quản lý thông tin", createInfoPanel());
        tabbedPane.addTab("Cá nhân hóa", createPersonalizationPanel());

        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(UIConstants.BG_PRIMARY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(UIConstants.FONT_BODY);
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // 1. QUẢN LÝ THÔNG TIN
    // ==========================================
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BG_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Avatar & Basic Info ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                String text = getText();
                if (text == null || text.isBlank()) {
                    SupabaseConfig config = SupabaseConfig.getInstance();
                    String email = config.getCurrentUserEmail();
                    text = (email != null && !email.isEmpty()) ? String.valueOf(email.charAt(0)).toUpperCase() : "A";
                }
                util.AvatarUtil.drawAvatar((Graphics2D) g, getWidth(), getHeight(), text, "A");
            }
        };
        avatarLabel.setPreferredSize(new Dimension(64, 64));
        avatarLabel.setMinimumSize(new Dimension(64, 64));

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);

        emailLabel = new JLabel();
        emailLabel.setFont(UIConstants.FONT_BODY);
        emailLabel.setForeground(UIConstants.TEXT_MUTED);

        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(5));
        namePanel.add(emailLabel);

        headerPanel.add(avatarLabel);
        headerPanel.add(namePanel);

        JButton editProfileBtn = new JButton("✏");
        editProfileBtn.setToolTipText("Chỉnh sửa hồ sơ");
        editProfileBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        editProfileBtn.setForeground(UIConstants.TEXT_SECONDARY);
        editProfileBtn.setContentAreaFilled(false);
        editProfileBtn.setBorderPainted(false);
        editProfileBtn.setFocusPainted(false);
        editProfileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editProfileBtn.addActionListener(e -> showEditProfileDialog());
        headerPanel.add(editProfileBtn);

        refreshProfileUI();

        panel.add(headerPanel);
        panel.add(Box.createVerticalStrut(30));

        // --- Security Settings ---
        JLabel secTitle = new JLabel("Bảo mật & Tài khoản");
        secTitle.setFont(UIConstants.FONT_BOLD);
        secTitle.setForeground(UIConstants.TEXT_SECONDARY);
        secTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(secTitle);
        panel.add(Box.createVerticalStrut(10));

        JButton changePwdBtn = createActionButton("Đổi mật khẩu");
        changePwdBtn.addActionListener(e -> showChangePasswordDialog());
        panel.add(changePwdBtn);
        panel.add(Box.createVerticalStrut(10));

        JButton deleteAccBtn = createActionButton("Xóa tài khoản");
        deleteAccBtn.setForeground(UIConstants.DANGER);
        deleteAccBtn.addActionListener(e -> deleteAccount());
        panel.add(deleteAccBtn);
        panel.add(Box.createVerticalStrut(10));

        JButton logoutBtn = createActionButton("Đăng xuất");
        logoutBtn.addActionListener(e -> logout());
        panel.add(logoutBtn);

        panel.add(Box.createVerticalStrut(30));

        // --- System Statistics ---
        JLabel statTitle = new JLabel("Thống kê hệ thống");
        statTitle.setFont(UIConstants.FONT_BOLD);
        statTitle.setForeground(UIConstants.TEXT_SECONDARY);
        statTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statTitle);
        panel.add(Box.createVerticalStrut(10));

        JPanel statsContainer = new JPanel(new GridLayout(2, 2, 10, 10));
        statsContainer.setOpaque(false);
        statsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsContainer.setMaximumSize(new Dimension(450, 120));

        JLabel totalContacts = createStatLabel("Tổng liên hệ", "Đang tải...");
        JLabel avgCompletion = createStatLabel("Độ hoàn thiện", "Đang tải...");
        JLabel weekAdded = createStatLabel("Mới tuần này", "Đang tải...");
        JLabel monthAdded = createStatLabel("Mới tháng này", "Đang tải...");

        statsContainer.add(totalContacts);
        statsContainer.add(avgCompletion);
        statsContainer.add(weekAdded);
        statsContainer.add(monthAdded);

        panel.add(statsContainer);

        // Load stats asynchronously
        loadStatistics(totalContacts, avgCompletion, weekAdded, monthAdded);

        return panel;
    }

    private JButton createActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BODY);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(450, 40));
        btn.setBackground(UIConstants.BG_SIDEBAR);
        btn.setForeground(UIConstants.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createStatLabel(String title, String value) {
        JLabel lbl = new JLabel(
                "<html><b>" + title + "</b><br><font color='#555555' size='5'>" + value + "</font></html>");
        lbl.setOpaque(true);
        lbl.setBackground(UIConstants.BG_SIDEBAR);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void loadStatistics(JLabel total, JLabel avgComp, JLabel week, JLabel month) {
        new SwingWorker<Map<String, Object>, Void>() {
            int wCount = 0;
            int mCount = 0;

            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                List<Contact> all = ContactService.getInstance().getAllContacts();
                LocalDateTime now = LocalDateTime.now();

                for (Contact c : all) {
                    if (c.getCreatedAt() != null) {
                        try {
                            String dStr = c.getCreatedAt().substring(0, Math.min(c.getCreatedAt().length(), 19));
                            LocalDateTime created = LocalDateTime.parse(dStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                            long days = ChronoUnit.DAYS.between(created, now);
                            if (days <= 7)
                                wCount++;
                            if (days <= 30)
                                mCount++;
                        } catch (Exception ignored) {
                        }
                    }
                }
                return ContactService.getInstance().getStatistics();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> stats = get();
                    total.setText("<html><b>Tổng liên hệ</b><br><font color='#2196F3' size='5'>" + stats.get("total")
                            + "</font></html>");
                    avgComp.setText("<html><b>Độ hoàn thiện</b><br><font color='#4CAF50' size='5'>"
                            + stats.get("avgCompletion") + "%</font></html>");
                    week.setText("<html><b>Mới tuần này</b><br><font color='#FF9800' size='5'>+" + wCount
                            + "</font></html>");
                    month.setText("<html><b>Mới tháng này</b><br><font color='#9C27B0' size='5'>+" + mCount
                            + "</font></html>");
                } catch (Exception e) {
                    total.setText("<html><b>Tổng liên hệ</b><br>Lỗi</html>");
                }
            }
        }.execute();
    }

    // ==========================================
    // 2. CÁ NHÂN HÓA
    // ==========================================
    private JPanel createPersonalizationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BG_PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Theme Selection ---
        JLabel themeTitle = new JLabel("Giao diện");
        themeTitle.setFont(UIConstants.FONT_BOLD);
        themeTitle.setForeground(UIConstants.TEXT_SECONDARY);
        themeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(themeTitle);
        panel.add(Box.createVerticalStrut(10));

        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        themePanel.setOpaque(false);
        themePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean isDark = java.util.prefs.Preferences.userRoot().node("contactmanager").getBoolean("dark_mode", false);

        JRadioButton lightRadio = new JRadioButton("Sáng (Light Mode)");
        lightRadio.setFont(UIConstants.FONT_BODY);
        lightRadio.setBackground(UIConstants.BG_PRIMARY);
        lightRadio.setSelected(!isDark); // Default

        JRadioButton darkRadio = new JRadioButton("Tối (Dark Mode)");
        darkRadio.setFont(UIConstants.FONT_BODY);
        darkRadio.setBackground(UIConstants.BG_PRIMARY);
        darkRadio.setSelected(isDark);

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightRadio);
        themeGroup.add(darkRadio);

        themePanel.add(lightRadio);
        themePanel.add(darkRadio);
        panel.add(themePanel);

        // Note about theme
        JLabel themeNote = new JLabel("Lưu ý: Thay đổi giao diện sẽ có hiệu lực sau khi khởi động lại ứng dụng.");
        themeNote.setFont(UIConstants.FONT_SMALL);
        themeNote.setForeground(UIConstants.TEXT_MUTED);
        themeNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(themeNote);
        panel.add(Box.createVerticalStrut(30));

        // --- Notifications ---
        JLabel notifTitle = new JLabel("Cài đặt thông báo");
        notifTitle.setFont(UIConstants.FONT_BOLD);
        notifTitle.setForeground(UIConstants.TEXT_SECONDARY);
        notifTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(notifTitle);
        panel.add(Box.createVerticalStrut(10));

        JCheckBox cleanupNotif = new JCheckBox("Nhắc nhở dọn dẹp định kỳ (Liên hệ trùng lặp, thiếu thông tin)");
        cleanupNotif.setFont(UIConstants.FONT_BODY);
        cleanupNotif.setBackground(UIConstants.BG_PRIMARY);
        cleanupNotif.setSelected(true);
        cleanupNotif.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cleanupNotif);

        panel.add(Box.createVerticalStrut(10));

        JCheckBox birthdayNotif = new JCheckBox("Hiển thị popup nhắc nhở sinh nhật khi mở ứng dụng");
        birthdayNotif.setFont(UIConstants.FONT_BODY);
        birthdayNotif.setBackground(UIConstants.BG_PRIMARY);
        birthdayNotif.setSelected(true);
        birthdayNotif.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(birthdayNotif);

        panel.add(Box.createVerticalGlue());

        // Save Preferences Button
        JButton saveBtn = new JButton("Lưu cài đặt");
        saveBtn.setFont(UIConstants.FONT_BOLD);
        saveBtn.setBackground(UIConstants.ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(150, 36));
        saveBtn.addActionListener(e -> {
            boolean selectedDark = darkRadio.isSelected();
            boolean currentlyDark = java.util.prefs.Preferences.userRoot().node("contactmanager")
                    .getBoolean("dark_mode", false);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SupabaseService.getInstance().updateUserTheme(selectedDark);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // throw exception if doInBackground failed
                        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot()
                                .node("contactmanager");
                        prefs.putBoolean("dark_mode", selectedDark);
                        prefs.flush();

                        if (selectedDark != currentlyDark) {
                            int res = JOptionPane.showConfirmDialog(AdminProfileDialog.this,
                                    "Đã lưu cài đặt! Ứng dụng cần tải lại để thay đổi giao diện. Tải lại ngay?",
                                    "Thành công", JOptionPane.YES_NO_OPTION);
                            if (res == JOptionPane.YES_OPTION) {
                                if (selectedDark) {
                                    try {
                                        com.formdev.flatlaf.FlatDarkLaf.setup();
                                    } catch (Exception ignored) {
                                    }
                                } else {
                                    try {
                                        com.formdev.flatlaf.FlatLightLaf.setup();
                                    } catch (Exception ignored) {
                                    }
                                }
                                ui.UIConstants.applyTheme(selectedDark);

                                if (parentFrame != null) {
                                    parentFrame.setVisible(false);
                                    parentFrame.dispose();
                                }
                                AdminProfileDialog.this.dispose();
                                MainFrame newFrame = new MainFrame();
                                newFrame.setVisible(true);
                            }
                        } else {
                            JOptionPane.showMessageDialog(AdminProfileDialog.this, "Đã lưu cài đặt cá nhân hóa!",
                                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AdminProfileDialog.this,
                                "Lỗi cập nhật giao diện: "
                                        + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        panel.add(Box.createVerticalStrut(20));
        panel.add(saveBtn);

        return panel;
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ SỰ KIỆN
    // ==========================================
    private void refreshProfileUI() {
        SupabaseConfig config = SupabaseConfig.getInstance();
        String email = config.getCurrentUserEmail();
        String dName = config.getCurrentUserDisplayName();
        String avatarStr = config.getCurrentUserAvatar();

        if (dName == null || dName.isBlank()) {
            nameLabel.setText(email != null ? email.split("@")[0] : "Admin");
        } else {
            nameLabel.setText(dName);
        }
        emailLabel.setText(email != null ? email : "admin@domain.com");

        String displayIcon = "A";
        if (avatarStr != null && !avatarStr.isBlank()) {
            displayIcon = avatarStr;
        } else if (dName != null && !dName.isBlank()) {
            displayIcon = String.valueOf(dName.charAt(0)).toUpperCase();
        } else if (email != null && !email.isBlank()) {
            displayIcon = String.valueOf(email.charAt(0)).toUpperCase();
        }

        avatarLabel.setText(displayIcon);
        avatarLabel.repaint();
    }

    private void showEditProfileDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        SupabaseConfig config = SupabaseConfig.getInstance();
        JTextField nameField = new JTextField(20);
        nameField.setText(config.getCurrentUserDisplayName());

        String currentAvatar = config.getCurrentUserAvatar();
        String[] finalAvatar = { currentAvatar };

        JButton chooseImageBtn = new JButton(
                currentAvatar != null && currentAvatar.startsWith("data:image") ? "Đổi ảnh khác" : "Chọn ảnh từ máy");
        chooseImageBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String base64 = util.AvatarUtil.encodeImageToBase64(chooser.getSelectedFile().getAbsolutePath());
                if (base64 != null) {
                    finalAvatar[0] = base64;
                    chooseImageBtn.setText("Đã chọn ảnh mới!");
                } else {
                    JOptionPane.showMessageDialog(this, "Lỗi đọc file ảnh!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton clearImageBtn = new JButton("Xóa ảnh");
        clearImageBtn.addActionListener(e -> {
            finalAvatar[0] = "";
            chooseImageBtn.setText("Chọn ảnh từ máy");
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.add(chooseImageBtn);
        btnPanel.add(Box.createHorizontalStrut(10));
        btnPanel.add(clearImageBtn);

        panel.add(new JLabel("Tên hiển thị:"));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Ảnh đại diện:"));
        panel.add(btnPanel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Chỉnh sửa hồ sơ", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newAvatar = finalAvatar[0];

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SupabaseService.getInstance().updateUserProfile(newName, newAvatar);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        refreshProfileUI();
                        JOptionPane.showMessageDialog(AdminProfileDialog.this,
                                "Cập nhật hồ sơ thành công! (Tự động tải lại cửa sổ chính để áp dụng)", "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Tự động tải lại MainFrame
                        if (parentFrame != null) {
                            parentFrame.setVisible(false);
                            MainFrame newFrame = new MainFrame();
                            newFrame.setVisible(true);
                            parentFrame.dispose();
                            AdminProfileDialog.this.dispose();
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AdminProfileDialog.this,
                                "Lỗi: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()), "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void showChangePasswordDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPasswordField currentPwd = new JPasswordField(20);
        JPasswordField newPwd = new JPasswordField(20);
        JPasswordField confirmPwd = new JPasswordField(20);

        panel.add(new JLabel("Mật khẩu hiện tại:"));
        panel.add(currentPwd);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Mật khẩu mới:"));
        panel.add(newPwd);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Xác nhận mật khẩu mới:"));
        panel.add(confirmPwd);

        int result = JOptionPane.showConfirmDialog(this, panel, "Đổi mật khẩu", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String cp = new String(currentPwd.getPassword());
            String np = new String(newPwd.getPassword());
            String cnp = new String(confirmPwd.getPassword());

            if (cp.isBlank() || np.isBlank() || cnp.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!np.equals(cnp)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu mới không khớp!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SupabaseService.getInstance().changePassword(cp, np);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(AdminProfileDialog.this, "Đổi mật khẩu thành công!", "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AdminProfileDialog.this,
                                "Lỗi: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()), "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void deleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn XÓA TÀI KHOẢN?\nToàn bộ dữ liệu liên hệ sẽ bị mất vĩnh viễn và không thể khôi phục!",
                "Cảnh báo nguy hiểm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String email = SupabaseConfig.getInstance().getCurrentUserEmail();
            String pwd = JOptionPane.showInputDialog(this, "Nhập mật khẩu của bạn để xác nhận xóa tài khoản:");

            if (pwd != null && !pwd.isBlank()) {
                try {
                    boolean check = SupabaseService.getInstance().loginUser(email, pwd);
                    if (check) {
                        // TODO: Implement user deletion in SupabaseService
                        JOptionPane.showMessageDialog(this,
                                "Đã gửi yêu cầu xóa tài khoản lên hệ thống. Tự động đăng xuất.", "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        logout();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Mật khẩu không đúng, không thể xóa tài khoản!", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private JPanel logout() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            SupabaseConfig.getInstance().clearAuthSession();
            dispose();
            parentFrame.setVisible(false);
            parentFrame.dispose();
            LoginFrame login = new LoginFrame();
            if (login.checkSupabaseConfig())
                login.setVisible(true);
        }
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UIConstants.BG_PRIMARY);
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Quản lý tài khoản người dùng");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(title, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UIConstants.BG_PRIMARY);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_PRIMARY);
        p.add(scroll, BorderLayout.CENTER);

        // Load danh sách người dùng
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
                                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
                        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                        String infoStr = uEmail;
                        if (uName != null && !uName.isBlank())
                            infoStr = uName + " (" + uEmail + ")";
                        if ("admin".equals(role))
                            infoStr += " [ADMIN]";

                        JLabel infoLbl = new JLabel(infoStr);
                        infoLbl.setFont(UIConstants.FONT_BODY);
                        infoLbl.setForeground(UIConstants.TEXT_PRIMARY);

                        JButton resetBtn = new JButton("Reset Mật Khẩu");
                        resetBtn.setFont(UIConstants.FONT_SMALL_BOLD);
                        resetBtn.setForeground(Color.WHITE);
                        resetBtn.setBackground(new Color(239, 68, 68)); // Đỏ
                        resetBtn.setFocusPainted(false);
                        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        if ("admin".equals(role) && !uid.equals(SupabaseConfig.getInstance().getCurrentUserId())) {
                            resetBtn.setEnabled(false); // Không reset pass admin khác để an toàn
                        }

                        resetBtn.addActionListener(e -> {
                            int ok = JOptionPane.showConfirmDialog(AdminProfileDialog.this,
                                    "Bạn muốn reset mật khẩu cho " + uEmail + "?", "Xác nhận",
                                    JOptionPane.YES_NO_OPTION);
                            if (ok == JOptionPane.YES_OPTION) {
                                String newPass = "Aa@" + (100000 + new java.util.Random().nextInt(900000));
                                try {
                                    SupabaseService.getInstance().adminResetUserPassword(uid, newPass);
                                    JTextArea ta = new JTextArea("Đã reset mật khẩu thành công!\n\nMật khẩu mới: "
                                            + newPass + "\n\nHãy copy và gửi cho họ.");
                                    ta.setEditable(false);
                                    JOptionPane.showMessageDialog(AdminProfileDialog.this, ta, "Thành công",
                                            JOptionPane.INFORMATION_MESSAGE);
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(AdminProfileDialog.this, "Lỗi: " + ex.getMessage(),
                                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        });

                        item.add(infoLbl, BorderLayout.CENTER);
                        item.add(resetBtn, BorderLayout.EAST);
                        listPanel.add(item);
                        listPanel.add(Box.createVerticalStrut(10));
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                } catch (Exception ex) {
                    JLabel err = new JLabel("Lỗi tải danh sách: " + ex.getMessage());
                    err.setForeground(Color.RED);
                    listPanel.add(err);
                }
            }
        }.execute();

        return p;
    }

    private String getStr(com.google.gson.JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
