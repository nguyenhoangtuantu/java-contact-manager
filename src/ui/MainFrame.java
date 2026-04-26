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
import util.AvatarUtil;

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
        if (SupabaseConfig.getInstance().isCurrentUserBirthdayReminder()) {
            checkUpcomingBirthdays();
        }
        if (SupabaseConfig.getInstance().isCurrentUserCleanupReminder()) {
            checkPeriodicCleanup();
        }
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
        
        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.setOpaque(false);
        
        if (SupabaseConfig.getInstance().isImpersonating()) {
            JButton backBtn = new JButton("⬅ Quay lại Admin Dashboard");
            backBtn.setBackground(new Color(239, 68, 68)); // Red color
            backBtn.setForeground(Color.WHITE);
            backBtn.setFont(UIConstants.FONT_SMALL_BOLD);
            backBtn.setFocusPainted(false);
            backBtn.setBorderPainted(false);
            backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
            backBtn.addActionListener(e -> {
                dispose(); // Closes the window and triggers windowClosed event
            });
            bottomWrap.add(backBtn, BorderLayout.NORTH);
        }
        
        bottomWrap.add(createUserPanel(), BorderLayout.SOUTH);
        sidebar.add(bottomWrap, BorderLayout.SOUTH);
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
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        JLabel iconLbl = new JLabel(emoji);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLbl.setPreferredSize(new Dimension(22, 22));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel textLbl = new JLabel("<html><div style='width: 130px;'>" + text + "</div></html>");
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
                    if (contactPanel != null) {
                        contactPanel.setAllGroups(groups);
                    }
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

        SupabaseConfig config = SupabaseConfig.getInstance();
        String email = config.getCurrentUserEmail();
        String dName = config.getCurrentUserDisplayName();
        String avatarStr = config.getCurrentUserAvatar();
        
        String displayIcon = "U";
        if (avatarStr != null && !avatarStr.isBlank()) {
            displayIcon = avatarStr;
        } else if (dName != null && !dName.isBlank()) {
            displayIcon = String.valueOf(dName.charAt(0)).toUpperCase();
        } else if (email != null && !email.isBlank()) {
            displayIcon = String.valueOf(email.charAt(0)).toUpperCase();
        }

        String displayName = (dName != null && !dName.isBlank()) ? dName : (email != null ? email.split("@")[0] : "");

        // Avatar circle
        JLabel avatar = new JLabel(displayIcon) {
            @Override protected void paintComponent(Graphics g) {
                AvatarUtil.drawAvatar((Graphics2D) g, getWidth(), getHeight(), getText(), "U");
            }
        };
        avatar.setPreferredSize(new Dimension(30, 30));
        avatar.setMinimumSize(new Dimension(30, 30));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel emailLbl = new JLabel(displayName);
        emailLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLbl.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel roleLbl = new JLabel(email);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLbl.setForeground(UIConstants.TEXT_MUTED);

        info.add(emailLbl);
        info.add(roleLbl);

        JButton profileBtn = new JButton("⚙"); 
        profileBtn.setToolTipText("Hồ sơ Admin");
        profileBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        profileBtn.setForeground(UIConstants.TEXT_SECONDARY);
        profileBtn.setContentAreaFilled(false);
        profileBtn.setBorderPainted(false);
        profileBtn.setFocusPainted(false);
        profileBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileBtn.addActionListener(e -> {
            AdminProfileDialog dialog = new AdminProfileDialog(MainFrame.this);
            dialog.setVisible(true);
        });

        p.add(avatar, BorderLayout.WEST);
        p.add(info, BorderLayout.CENTER);
        p.add(profileBtn, BorderLayout.EAST);
        
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                AdminProfileDialog dialog = new AdminProfileDialog(MainFrame.this);
                dialog.setVisible(true);
            }
            @Override public void mouseEntered(MouseEvent e) {
                p.setBackground(UIConstants.BG_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                p.setBackground(UIConstants.BG_SIDEBAR);
            }
        });
        
        return p;
    }

    // ── ADD GROUP ──────────────────────────────────────────
    private void showAddGroupDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(380, 260));

        // --- Auto Icon ---
        java.util.Set<String> usedIcons = groups.stream()
                .map(GroupInfo::getIcon)
                .collect(java.util.stream.Collectors.toSet());
        
        String[] emojiList = {"📌", "⭐", "📁", "🔥", "💎", "💡", "🚀", "🎉", "📚", "🎨", "🏆", "🌟", "💼", "🏢", "🏠", "🌍", "🌈", "🎵", "🎁", "☕"};
        String selectedIcon = emojiList[0];
        for (String em : emojiList) {
            if (!usedIcons.contains(em)) {
                selectedIcon = em;
                break;
            }
        }
        final String finalIcon = selectedIcon;

        // --- Fields ---
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JTextField descField = new JTextField();
        descField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Selected color state
        final String[] selectedColor = {"#FF6B6B"};

        JLabel previewDot = new JLabel("● Nhóm mới");
        previewDot.setForeground(Color.decode(selectedColor[0]));

        // --- Color Palette Panel ---
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        colorPanel.setOpaque(false);
        String[] palette = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#FDCB6E", "#6C5CE7", "#A8E6CF", "#FF8ED4", "#54A0FF", "#00B894", "#E17055"};
        
        List<JPanel> colorBtns = new ArrayList<>();
        
        for (String hex : palette) {
            JPanel btn = new JPanel();
            btn.setPreferredSize(new Dimension(24, 24));
            btn.setBackground(Color.decode(hex));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            if (hex.equals(selectedColor[0])) {
                btn.setBorder(BorderFactory.createLineBorder(UIConstants.TEXT_PRIMARY, 2));
            }
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedColor[0] = hex;
                    for (JPanel b : colorBtns) b.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
                    btn.setBorder(BorderFactory.createLineBorder(UIConstants.TEXT_PRIMARY, 2));
                    previewDot.setForeground(Color.decode(hex));
                }
            });
            colorBtns.add(btn);
            colorPanel.add(btn);
        }

        // --- Add to panel ---
        JLabel nameLbl = new JLabel("Tên nhóm *");
        nameLbl.setFont(UIConstants.FONT_SMALL);
        panel.add(nameLbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(12));

        JLabel descLbl = new JLabel("Mô tả");
        descLbl.setFont(UIConstants.FONT_SMALL);
        panel.add(descLbl);
        panel.add(Box.createVerticalStrut(4));
        panel.add(descField);
        panel.add(Box.createVerticalStrut(12));

        JLabel colorLbl = new JLabel("Chọn màu:");
        colorLbl.setFont(UIConstants.FONT_SMALL);
        panel.add(colorLbl);
        panel.add(Box.createVerticalStrut(4));
        
        JPanel colorWrap = new JPanel(new BorderLayout());
        colorWrap.setOpaque(false);
        colorWrap.add(colorPanel, BorderLayout.WEST);
        colorWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(colorWrap);
        
        panel.add(Box.createVerticalStrut(12));

        JPanel preview = new JPanel(new FlowLayout(FlowLayout.LEFT));
        preview.setOpaque(false);
        preview.add(new JLabel("Xem trước: " + finalIcon + " "));
        preview.add(previewDot);
        panel.add(preview);

        nameField.addCaretListener(e ->
            previewDot.setText("● " + (nameField.getText().isEmpty() ? "Nhóm mới" : nameField.getText())));

        int res = JOptionPane.showConfirmDialog(this, panel, "Thêm nhóm ưu tiên mới",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String dn = nameField.getText().trim();
            if (dn.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm!", "Lỗi", JOptionPane.WARNING_MESSAGE); return; }
            GroupInfo ng = new GroupInfo(dn, finalIcon, selectedColor[0]);
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

    // ── CLEANUP CHECK ────────────────────────────────────
    private void checkPeriodicCleanup() {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                List<model.Contact> all = ContactService.getInstance().getAllContacts();
                int missingCount = 0;
                for (model.Contact c : all) {
                    if (c.getPhone() == null || c.getPhone().isBlank() || c.getEmail() == null || c.getEmail().isBlank()) {
                        missingCount++;
                    }
                }
                return missingCount > 0;
            }
            @Override protected void done() {
                try {
                    boolean needsCleanup = get();
                    if (needsCleanup) {
                        int res = JOptionPane.showConfirmDialog(MainFrame.this, 
                                "Có liên hệ thiếu thông tin hoặc trùng lặp.\nBạn có muốn mở công cụ dọn dẹp không?", 
                                "Nhắc nhở dọn dẹp định kỳ", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (res == JOptionPane.YES_OPTION) {
                            CleanupDialog dlg = new CleanupDialog(MainFrame.this);
                            dlg.setVisible(true);
                            if (dlg.isChanged()) { 
                                contactPanel.loadContacts(); 
                                refreshGroupList(); 
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}
