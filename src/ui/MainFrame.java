package ui;

import config.SupabaseConfig;
import model.GroupInfo;
import service.ContactService;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cửa sổ chính — sidebar trắng bên trái, nội dung bên phải.
 */
public class MainFrame extends JFrame {

    private JPanel       contentPanel;
    private ContactPanel contactPanel;
    private JPanel       groupListPanel;
    private JButton      activeNavButton = null;
    private List<GroupInfo> groups = new ArrayList<>();

    public MainFrame() {
        setTitle("Quản lý Danh bạ");
        setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));
        getContentPane().setBackground(UIConstants.BG_PRIMARY);
        initComponents();
        contactPanel.loadContacts();
        refreshGroupList();
        checkUpcomingBirthdays();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        add(createSidebar(), BorderLayout.WEST);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(UIConstants.BG_PRIMARY);
        contactPanel = new ContactPanel();
        contactPanel.setOnRefresh(this::refreshGroupList);
        contentPanel.add(contactPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    // =========================================================
    //  SIDEBAR
    // =========================================================
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIConstants.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));

        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);

        // ── App title with logo ──
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 8, 16, 8));
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        // Logo
        JLabel logoLabel = new JLabel();
        logoLabel.setPreferredSize(new Dimension(36, 36));
        try {
            BufferedImage logoImg = ImageIO.read(new java.io.File("resources/logo.jpg"));
            if (logoImg != null) {
                Image scaled = logoImg.getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ex) {
            logoLabel.setText("📇");
            logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("Quản lý Danh bạ");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Quản lý liên hệ");
        subLbl.setFont(UIConstants.FONT_SMALL);
        subLbl.setForeground(UIConstants.TEXT_MUTED);

        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subLbl);

        titlePanel.add(logoLabel);
        titlePanel.add(textPanel);
        nav.add(titlePanel);

        // ── CHỨC NĂNG section ──
        nav.add(sectionLabel("CHỨC NĂNG"));

        JButton allBtn = navButton("\uD83D\uDC64", "Tất cả liên hệ");  // 👤
        allBtn.addActionListener(e -> { activateNav(allBtn); contactPanel.setTrashMode(false); });
        nav.add(allBtn);
        activateNav(allBtn);

        JButton recentBtn = navButton("\uD83D\uDD51", "Gần đây");  // 🕑
        recentBtn.addActionListener(e -> { activateNav(recentBtn); contactPanel.filterByRecent(); });
        nav.add(recentBtn);




        JButton cleanBtn = navButton("\uD83E\uDDF9", "Dọn dẹp định kỳ");  // 🧹
        cleanBtn.addActionListener(e -> {
            CleanupDialog dlg = new CleanupDialog(this);
            dlg.setVisible(true);
            if (dlg.isChanged()) { contactPanel.loadContacts(); refreshGroupList(); }
        });
        nav.add(cleanBtn);

        JButton trashBtn = navButton("\uD83D\uDDD1", "Thùng rác");  // 🗑
        trashBtn.addActionListener(e -> { activateNav(trashBtn); contactPanel.setTrashMode(true); });
        nav.add(trashBtn);

        nav.add(Box.createVerticalStrut(8));
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        nav.add(sep);
        nav.add(Box.createVerticalStrut(4));

        // ── NHÓM ƯU TIÊN section ──
        JPanel groupHeader = new JPanel(new BorderLayout());
        groupHeader.setOpaque(false);
        groupHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        groupHeader.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 12));

        JLabel nhomLbl = new JLabel("NHÓM ƯU TIÊN");
        nhomLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        nhomLbl.setForeground(UIConstants.TEXT_MUTED);

        JButton addGrp = new JButton("+");
        addGrp.setFont(new Font("Segoe UI", Font.BOLD, 16));
        addGrp.setForeground(UIConstants.ACCENT);
        addGrp.setContentAreaFilled(false);
        addGrp.setBorderPainted(false);
        addGrp.setFocusPainted(false);
        addGrp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addGrp.setPreferredSize(new Dimension(24, 20));
        addGrp.setToolTipText("Thêm nhóm mới");
        addGrp.addActionListener(e -> showAddGroupDialog());

        groupHeader.add(nhomLbl, BorderLayout.WEST);
        groupHeader.add(addGrp, BorderLayout.EAST);
        nav.add(groupHeader);

        groupListPanel = new JPanel();
        groupListPanel.setLayout(new BoxLayout(groupListPanel, BoxLayout.Y_AXIS));
        groupListPanel.setOpaque(false);
        nav.add(groupListPanel);

        nav.add(Box.createVerticalGlue());

        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(createUserPanel(), BorderLayout.SOUTH);
        return sidebar;
    }

    // ── Helpers ──────────────────────────────────────────

    private JPanel sectionLabel(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JButton navButton(String emoji, String text) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(8, 0));
        btn.setBackground(UIConstants.BG_SIDEBAR);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        JLabel iconLbl = new JLabel(emoji);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLbl.setPreferredSize(new Dimension(22, 22));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel textLbl = new JLabel(text);
        textLbl.setFont(UIConstants.FONT_SIDEBAR);
        textLbl.setForeground(UIConstants.TEXT_SECONDARY);

        btn.add(iconLbl, BorderLayout.WEST);
        btn.add(textLbl, BorderLayout.CENTER);

        btn.putClientProperty("textLbl", textLbl);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn != activeNavButton) btn.setBackground(UIConstants.BG_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn != activeNavButton) btn.setBackground(UIConstants.BG_SIDEBAR);
            }
        });
        return btn;
    }

    private void activateNav(JButton btn) {
        if (activeNavButton != null && activeNavButton != btn) {
            activeNavButton.setBackground(UIConstants.BG_SIDEBAR);
            JLabel tl = (JLabel) activeNavButton.getClientProperty("textLbl");
            if (tl != null) tl.setForeground(UIConstants.TEXT_SECONDARY);
        }
        activeNavButton = btn;
        btn.setBackground(UIConstants.BG_SELECTED);
        JLabel tl = (JLabel) btn.getClientProperty("textLbl");
        if (tl != null) tl.setForeground(UIConstants.ACCENT);
    }

    // ── GROUP LIST ─────────────────────────────────────────
    private void refreshGroupList() {
        new SwingWorker<Void, Void>() {
            List<GroupInfo> loaded;
            Map<Integer, Integer> counts;
            @Override protected Void doInBackground() throws Exception {
                loaded = ContactService.getInstance().getAllGroups();
                counts = ContactService.getInstance().getGroupCountsById();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    groups = loaded;
                    groupListPanel.removeAll();
                    for (GroupInfo g : groups) {
                        int cnt = counts != null ? counts.getOrDefault(g.getId(), 0) : 0;
                        groupListPanel.add(createGroupRow(g, cnt));
                    }
                    groupListPanel.revalidate();
                    groupListPanel.repaint();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JPanel createGroupRow(GroupInfo group, int count) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setBorder(BorderFactory.createEmptyBorder(3, 16, 3, 12));

        // Left: emoji icon + name
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JLabel emojiLbl = new JLabel(group.getIcon());
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));

        JLabel nameLbl = new JLabel(group.getDisplayName());
        nameLbl.setFont(UIConstants.FONT_SMALL);
        nameLbl.setForeground(UIConstants.TEXT_SECONDARY);

        left.add(emojiLbl);
        left.add(nameLbl);

        row.add(left, BorderLayout.WEST);

        // Right: count badge
        if (count > 0) {
            JLabel badge = new JLabel(String.valueOf(count));
            badge.setFont(UIConstants.FONT_SMALL_BOLD);
            badge.setForeground(Color.WHITE);
            badge.setOpaque(true);
            badge.setBackground(UIConstants.ACCENT);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setPreferredSize(new Dimension(22, 18));
            badge.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
            row.add(badge, BorderLayout.EAST);
        }

        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    contactPanel.resetToNormalMode();
                    contactPanel.filterByGroupId(group.getId(), group.getDisplayName());
                }
            }
            @Override public void mousePressed(MouseEvent e)  { showGroupPopup(e, group); }
            @Override public void mouseReleased(MouseEvent e) { showGroupPopup(e, group); }
            @Override public void mouseEntered(MouseEvent e) {
                row.setOpaque(true); row.setBackground(UIConstants.BG_HOVER); row.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setOpaque(false); row.repaint();
            }
        });
        return row;
    }

    private void showGroupPopup(MouseEvent e, GroupInfo group) {
        if (!e.isPopupTrigger()) return;
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("✖ Xóa nhóm \"" + group.getDisplayName() + "\"");
        deleteItem.setForeground(UIConstants.DANGER);
        deleteItem.addActionListener(ev -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Xóa nhóm \"" + group.getDisplayName() + "\"?\nCác liên hệ trong nhóm cần được chuyển trước.",
                    "Xác nhận xóa nhóm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        ContactService.getInstance().deleteGroup(group.getId());
                        return null;
                    }
                    @Override protected void done() {
                        try {
                            get();
                            refreshGroupList();
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Đã xóa nhóm!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                                    "Không thể xóa", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }.execute();
            }
        });
        menu.add(deleteItem);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // ── USER PANEL ─────────────────────────────────────────
    private JPanel createUserPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UIConstants.BG_SIDEBAR);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        String email = SupabaseConfig.getInstance().getCurrentUserEmail();
        String initial = (email != null && !email.isEmpty())
                ? String.valueOf(email.charAt(0)).toUpperCase() : "U";

        // Avatar circle
        JLabel avatar = new JLabel(initial) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(30, 30));
        avatar.setMinimumSize(new Dimension(30, 30));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel emailLbl = new JLabel(email != null ? email : "");
        emailLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLbl.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel roleLbl = new JLabel("Admin Plan");
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLbl.setForeground(UIConstants.TEXT_MUTED);

        info.add(emailLbl);
        info.add(roleLbl);

        JButton logoutBtn = new JButton("\u2192"); // →
        logoutBtn.setToolTipText("Đăng xuất");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoutBtn.setForeground(UIConstants.DANGER);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn đăng xuất?", "Đăng xuất", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                SupabaseConfig.getInstance().clearAuthSession();
                setVisible(false); dispose();
                LoginFrame login = new LoginFrame();
                if (login.checkSupabaseConfig()) login.setVisible(true);
            }
        });

        p.add(avatar, BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        p.add(logoutBtn, BorderLayout.EAST);
        return p;
    }

    // ── ADD GROUP ──────────────────────────────────────────
    private void showAddGroupDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(380, 220));

        JTextField nameField  = new JTextField();
        JTextField iconField  = new JTextField("📌");
        JTextField colorField = new JTextField("#FF6B6B");
        JTextField descField  = new JTextField();

        String[][] fields = {{"Tên nhóm *", ""}, {"Icon (emoji)", "📌"}, {"Màu (hex)", "#FF6B6B"}, {"Mô tả", ""}};
        JTextField[] tfs = {nameField, iconField, colorField, descField};
        for (int i = 0; i < fields.length; i++) {
            JLabel lbl = new JLabel(fields[i][0]);
            lbl.setFont(UIConstants.FONT_SMALL);
            panel.add(lbl);
            panel.add(Box.createVerticalStrut(2));
            tfs[i].setText(fields[i][1]);
            tfs[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            panel.add(tfs[i]);
            panel.add(Box.createVerticalStrut(8));
        }

        JPanel preview = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel previewDot = new JLabel("● Nhóm mới");
        previewDot.setForeground(Color.decode("#FF6B6B"));
        preview.add(new JLabel("Xem trước: "));
        preview.add(previewDot);
        panel.add(preview);

        colorField.addCaretListener(e -> {
            try { previewDot.setForeground(Color.decode(colorField.getText().trim())); } catch (Exception ignored) {}
        });
        nameField.addCaretListener(e ->
            previewDot.setText("● " + (nameField.getText().isEmpty() ? "Nhóm mới" : nameField.getText())));

        int res = JOptionPane.showConfirmDialog(this, panel, "Thêm nhóm ưu tiên mới",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String dn = nameField.getText().trim();
            if (dn.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!", "Lỗi", JOptionPane.WARNING_MESSAGE); return; }
            GroupInfo ng = new GroupInfo(dn, iconField.getText().trim(), colorField.getText().trim());
            ng.setDescription(descField.getText().trim());
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    ContactService.getInstance().addGroup(ng); return null;
                }
                @Override protected void done() {
                    try {
                        get(); refreshGroupList();
                        JOptionPane.showMessageDialog(MainFrame.this, "Đã thêm nhóm \"" + dn + "\"!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    // ── BIRTHDAY CHECK ────────────────────────────────────
    private void checkUpcomingBirthdays() {
        new SwingWorker<List<model.Contact>, Void>() {
            @Override protected List<model.Contact> doInBackground() throws Exception {
                return ContactService.getInstance().getUpcomingBirthdays(7);
            }
            @Override protected void done() {
                try {
                    List<model.Contact> bdays = get();
                    if (bdays != null && !bdays.isEmpty()) {
                        StringBuilder sb = new StringBuilder("Sắp tới sinh nhật:\n\n");
                        for (model.Contact c : bdays) {
                            String d = c.getBirthday();
                            if (d != null && d.length() >= 10) d = d.substring(8, 10) + "/" + d.substring(5, 7);
                            sb.append("🎂 ").append(c.getName()).append(" (").append(d).append(")\n");
                        }
                        JTextArea ta = new JTextArea(sb.toString());
                        ta.setEditable(false); ta.setOpaque(false); ta.setFont(UIConstants.FONT_BODY);
                        JOptionPane.showMessageDialog(MainFrame.this, ta, "🎉 Nhắc nhở Sinh nhật", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}
