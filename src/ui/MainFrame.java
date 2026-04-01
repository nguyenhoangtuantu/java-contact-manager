package ui;

import config.SupabaseConfig;
import model.GroupInfo;
import service.ContactService;
import service.SupabaseService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cửa sổ chính của ứng dụng Quản lý Danh bạ.
 * Gồm sidebar bên trái và content panel bên phải.
 * Hỗ trợ nhóm ưu tiên dynamic (không giới hạn).
 */
public class MainFrame extends JFrame {
    private JPanel contentPanel;
    private ContactPanel contactPanel;
    private JPanel sidebarPanel;
    private JPanel groupListPanel;
    private JButton activeButton = null;
    private List<GroupInfo> groups = new ArrayList<>();

    public MainFrame() {
        setTitle("📱 Quản lý Danh bạ");
        setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));
        getContentPane().setBackground(UIConstants.BG_PRIMARY);

        if (!checkSupabaseConfig()) {
            System.exit(0);
        }

        initComponents();
        contactPanel.loadContacts();
        refreshGroupList();
        checkUpcomingBirthdays();
    }

    private boolean checkSupabaseConfig() {
        SupabaseConfig config = SupabaseConfig.getInstance();
        if (!config.isConfigured()) {
            return showConfigDialog(config);
        }
        if (!SupabaseService.getInstance().testConnection()) {
            int retry = JOptionPane.showConfirmDialog(this,
                    "Không thể kết nối Supabase!\nBạn có muốn nhập lại thông tin kết nối?",
                    "Lỗi kết nối", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (retry == JOptionPane.YES_OPTION) {
                return showConfigDialog(config);
            }
            return false;
        }
        return true;
    }

    private boolean showConfigDialog(SupabaseConfig config) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setPreferredSize(new Dimension(450, 140));
        JLabel urlLabel = new JLabel("Supabase URL:");
        JTextField urlField = new JTextField(config.getSupabaseUrl());
        JLabel keyLabel = new JLabel("Supabase API Key (anon):");
        JTextField keyField = new JTextField(config.getSupabaseKey());
        panel.add(urlLabel); panel.add(urlField);
        panel.add(keyLabel); panel.add(keyField);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "⚙️ Cấu hình Supabase", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String url = urlField.getText().trim();
            String key = keyField.getText().trim();
            if (url.isEmpty() || key.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập đầy đủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            config.saveConfig(url, key);
            return true;
        }
        return false;
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        sidebarPanel = createSidebar();
        add(sidebarPanel, BorderLayout.WEST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIConstants.BG_PRIMARY);
        contactPanel = new ContactPanel();
        contactPanel.setOnRefresh(this::refreshGroupList);
        contentPanel.add(contactPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, UIConstants.BG_SIDEBAR,
                        0, getHeight(), new Color(16, 16, 26));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));

        // App title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 8, 10, 8));

        JLabel appIcon = new JLabel("📱");
        appIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JPanel titleText = new JPanel();
        titleText.setLayout(new BoxLayout(titleText, BoxLayout.Y_AXIS));
        titleText.setOpaque(false);
        JLabel appName = new JLabel("Danh Bạ");
        appName.setFont(UIConstants.FONT_SUBTITLE);
        appName.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel appDesc = new JLabel("Quản lý liên hệ");
        appDesc.setFont(UIConstants.FONT_SMALL);
        appDesc.setForeground(UIConstants.TEXT_MUTED);
        titleText.add(appName);
        titleText.add(appDesc);
        titlePanel.add(appIcon);
        titlePanel.add(titleText);
        sidebar.add(titlePanel);

        sidebar.add(createSeparator());
        sidebar.add(Box.createVerticalStrut(8));

        // Navigation
        JLabel navLabel = new JLabel("  CHỨC NĂNG");
        navLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        navLabel.setForeground(UIConstants.TEXT_MUTED);
        navLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 0));
        navLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        sidebar.add(navLabel);

        JButton contactsBtn = createSidebarButton("📒", "Danh bạ");
        contactsBtn.addActionListener(e -> {
            setActiveButton(contactsBtn);
            contactPanel.setTrashMode(false);
        });
        sidebar.add(contactsBtn);
        activeButton = contactsBtn;
        contactsBtn.setBackground(UIConstants.BG_SELECTED);

        JButton duplicateBtn = createSidebarButton("🔄", "Hợp nhất trùng lặp");
        duplicateBtn.addActionListener(e -> {
            DuplicateDialog dialog = new DuplicateDialog(this);
            dialog.setVisible(true);
            if (dialog.isChanged()) {
                contactPanel.loadContacts();
                refreshGroupList();
            }
        });
        sidebar.add(duplicateBtn);

        JButton cleanupBtn = createSidebarButton("🧹", "Dọn dẹp định kỳ");
        cleanupBtn.addActionListener(e -> {
            CleanupDialog dialog = new CleanupDialog(this);
            dialog.setVisible(true);
            if (dialog.isChanged()) {
                contactPanel.loadContacts();
                refreshGroupList();
            }
        });
        sidebar.add(cleanupBtn);

        JButton trashBtn = createSidebarButton("🗑", "Thùng rác");
        trashBtn.addActionListener(e -> {
            setActiveButton(trashBtn);
            contactPanel.setTrashMode(true);
        });
        sidebar.add(trashBtn);

        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(createSeparator());
        sidebar.add(Box.createVerticalStrut(8));

        // Group header + Add button
        JPanel groupHeader = new JPanel(new BorderLayout());
        groupHeader.setOpaque(false);
        groupHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        groupHeader.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 12));

        JLabel groupLabel = new JLabel("NHÓM ƯU TIÊN");
        groupLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        groupLabel.setForeground(UIConstants.TEXT_MUTED);

        JButton addGroupBtn = new JButton("+");
        addGroupBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        addGroupBtn.setForeground(UIConstants.ACCENT);
        addGroupBtn.setBackground(UIConstants.BG_SIDEBAR);
        addGroupBtn.setBorderPainted(false);
        addGroupBtn.setFocusPainted(false);
        addGroupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addGroupBtn.setPreferredSize(new Dimension(28, 24));
        addGroupBtn.setToolTipText("Thêm nhóm mới");
        addGroupBtn.addActionListener(e -> showAddGroupDialog());
        addGroupBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { addGroupBtn.setForeground(UIConstants.ACCENT_HOVER); }
            @Override
            public void mouseExited(MouseEvent e) { addGroupBtn.setForeground(UIConstants.ACCENT); }
        });

        groupHeader.add(groupLabel, BorderLayout.WEST);
        groupHeader.add(addGroupBtn, BorderLayout.EAST);
        sidebar.add(groupHeader);

        // Dynamic group list
        groupListPanel = new JPanel();
        groupListPanel.setLayout(new BoxLayout(groupListPanel, BoxLayout.Y_AXIS));
        groupListPanel.setOpaque(false);
        sidebar.add(groupListPanel);

        sidebar.add(Box.createVerticalGlue());

        sidebar.add(Box.createVerticalStrut(12));

        return sidebar;
    }

    /**
     * Load nhóm ưu tiên từ DB và hiển thị trong sidebar.
     */
    private void refreshGroupList() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<GroupInfo> loadedGroups;
            Map<Integer, Integer> counts;

            @Override
            protected Void doInBackground() throws Exception {
                loadedGroups = ContactService.getInstance().getAllGroups();
                counts = ContactService.getInstance().getGroupCountsById();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    groups = loadedGroups;
                    groupListPanel.removeAll();

                    for (GroupInfo g : groups) {
                        int count = counts != null ? counts.getOrDefault(g.getId(), 0) : 0;
                        groupListPanel.add(createGroupRow(g, count));
                    }

                    groupListPanel.revalidate();
                    groupListPanel.repaint();
                } catch (Exception e) {
                    // Silently fail
                }
            }
        };
        worker.execute();
    }

    private JPanel createGroupRow(GroupInfo group, int count) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.PLAIN, 10));
        dot.setForeground(group.getColor());

        JLabel name = new JLabel(group.getDisplayName());
        name.setFont(UIConstants.FONT_SMALL);
        name.setForeground(UIConstants.TEXT_SECONDARY);

        left.add(dot);
        left.add(name);

        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        countLabel.setForeground(count > 0 ? UIConstants.TEXT_SECONDARY : UIConstants.TEXT_MUTED);

        row.add(left, BorderLayout.WEST);
        row.add(countLabel, BorderLayout.EAST);

        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Filter contacts by this group
                contactPanel.setTrashMode(false);
                contactPanel.filterByGroupId(group.getId(), group.getDisplayName());
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                row.setOpaque(true);
                row.setBackground(UIConstants.BG_HOVER);
                row.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                row.setOpaque(false);
                row.repaint();
            }
        });

        return row;
    }

    /**
     * Dialog thêm nhóm ưu tiên mới.
     */
    private void showAddGroupDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(380, 220));

        JTextField nameField = new JTextField();
        JTextField iconField = new JTextField("📌");
        JTextField colorField = new JTextField("#FF6B6B");
        JTextField descField = new JTextField();

        String[][] fields = {
                {"Tên nhóm *", ""},
                {"Icon (emoji)", "📌"},
                {"Màu (hex)", "#FF6B6B"},
                {"Mô tả", ""}
        };
        JTextField[] textFields = {nameField, iconField, colorField, descField};

        for (int i = 0; i < fields.length; i++) {
            JLabel label = new JLabel(fields[i][0]);
            label.setFont(UIConstants.FONT_SMALL);
            panel.add(label);
            panel.add(Box.createVerticalStrut(2));
            textFields[i].setText(fields[i][1]);
            textFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            panel.add(textFields[i]);
            panel.add(Box.createVerticalStrut(8));
        }

        // Color preview
        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel previewLabel = new JLabel("Xem trước: ");
        JLabel previewDot = new JLabel("● Nhóm mới");
        previewDot.setForeground(Color.decode("#FF6B6B"));
        previewPanel.add(previewLabel);
        previewPanel.add(previewDot);
        panel.add(previewPanel);

        colorField.addCaretListener(e -> {
            try {
                previewDot.setForeground(Color.decode(colorField.getText().trim()));
                previewDot.setText("● " + (nameField.getText().isEmpty() ? "Nhóm mới" : nameField.getText()));
            } catch (Exception ex) { /* ignore */ }
        });
        nameField.addCaretListener(e -> {
            previewDot.setText("● " + (nameField.getText().isEmpty() ? "Nhóm mới" : nameField.getText()));
        });

        int result = JOptionPane.showConfirmDialog(this, panel,
                "➕ Thêm nhóm ưu tiên mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String displayName = nameField.getText().trim();
            if (displayName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            GroupInfo newGroup = new GroupInfo(displayName, iconField.getText().trim(), colorField.getText().trim());
            newGroup.setDescription(descField.getText().trim());

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ContactService.getInstance().addGroup(newGroup);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        refreshGroupList();
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Đã thêm nhóm \"" + displayName + "\"!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    // === Utility ===

    private JButton createSidebarButton(String icon, String text) {
        JButton btn = new JButton(icon + "  " + text);
        btn.setFont(UIConstants.FONT_SIDEBAR);
        btn.setForeground(UIConstants.TEXT_SECONDARY);
        btn.setBackground(UIConstants.BG_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn != activeButton) { btn.setBackground(UIConstants.BG_HOVER); btn.setForeground(UIConstants.TEXT_PRIMARY); }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn != activeButton) { btn.setBackground(UIConstants.BG_SIDEBAR); btn.setForeground(UIConstants.TEXT_SECONDARY); }
            }
        });
        return btn;
    }

    private void setActiveButton(JButton btn) {
        if (activeButton != null) { activeButton.setBackground(UIConstants.BG_SIDEBAR); activeButton.setForeground(UIConstants.TEXT_SECONDARY); }
        activeButton = btn;
        btn.setBackground(UIConstants.BG_SELECTED);
        btn.setForeground(UIConstants.TEXT_PRIMARY);
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER);
        sep.setBackground(UIConstants.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private void checkUpcomingBirthdays() {
        SwingWorker<List<model.Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<model.Contact> doInBackground() throws Exception {
                return ContactService.getInstance().getUpcomingBirthdays(7);
            }

            @Override
            protected void done() {
                try {
                    List<model.Contact> birthdays = get();
                    if (birthdays != null && !birthdays.isEmpty()) {
                        StringBuilder sb = new StringBuilder("Sắp tới sinh nhật của các liên hệ sau:\n\n");
                        for (model.Contact c : birthdays) {
                            String date = c.getBirthday();
                            if (date != null && date.length() >= 10) {
                                // yyyy-MM-dd -> dd/MM
                                date = date.substring(8, 10) + "/" + date.substring(5, 7);
                            }
                            sb.append("🎂 ").append(c.getName()).append(" (Ngày ").append(date).append(")\n");
                        }

                        JTextArea ta = new JTextArea(sb.toString());
                        ta.setEditable(false);
                        ta.setBackground(new Color(0, 0, 0, 0));
                        ta.setOpaque(false);
                        ta.setFont(UIConstants.FONT_BODY);

                        JPanel panel = new JPanel(new BorderLayout());
                        panel.setOpaque(false);
                        panel.add(ta, BorderLayout.CENTER);

                        JOptionPane.showMessageDialog(MainFrame.this, panel,
                                "🎉 Nhắc nhở Sinh nhật", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    // Ignore silently
                }
            }
        };
        worker.execute();
    }
}
