package ui;

import model.Contact;
import model.ContactGroup;
import model.GroupInfo;
import service.ContactService;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Panel chính hiển thị danh sách liên hệ với bảng, thanh tìm kiếm, và bộ lọc nhóm.
 */
public class ContactPanel extends JPanel {
    private final ContactService contactService;
    private JTable contactTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> groupFilter;
    private JLabel statusLabel;
    private List<Contact> currentContacts = new ArrayList<>();
    private List<GroupInfo> allGroups = new ArrayList<>();
    private Runnable onRefresh;

    public ContactPanel() {
        this.contactService = ContactService.getInstance();
        setBackground(UIConstants.BG_PRIMARY);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        initComponents();
    }

    public void setOnRefresh(Runnable onRefresh) {
        this.onRefresh = onRefresh;
    }

    private void initComponents() {
        // === TOP BAR ===
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Title
        JLabel titleLabel = new JLabel("📒 Danh bạ");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        // Search
        JPanel searchPanel = createSearchPanel();

        // Filter + Actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        groupFilter = new JComboBox<>(new String[]{"Tất cả"});
        groupFilter.setFont(UIConstants.FONT_BODY);
        groupFilter.setPreferredSize(new Dimension(160, 36));
        groupFilter.addActionListener(e -> filterByGroup());
        loadGroupFilter();

        JButton addBtn = createStyledButton("+ Thêm liên hệ", UIConstants.ACCENT);
        addBtn.addActionListener(e -> showAddDialog());

        actionsPanel.add(groupFilter);
        actionsPanel.add(addBtn);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleLabel, BorderLayout.WEST);
        titleRow.add(actionsPanel, BorderLayout.EAST);

        topBar.add(titleRow, BorderLayout.NORTH);
        topBar.add(searchPanel, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);

        // === TABLE ===
        String[] columns = {"", "Tên", "SĐT", "Email", "Công ty", "Nhóm", "Hoàn thiện"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        contactTable = new JTable(tableModel);
        contactTable.setFont(UIConstants.FONT_TABLE);
        contactTable.setRowHeight(44);
        contactTable.setBackground(UIConstants.BG_SECONDARY);
        contactTable.setForeground(UIConstants.TEXT_PRIMARY);
        contactTable.setSelectionBackground(UIConstants.BG_SELECTED);
        contactTable.setSelectionForeground(UIConstants.TEXT_PRIMARY);
        contactTable.setGridColor(UIConstants.BORDER);
        contactTable.setShowHorizontalLines(true);
        contactTable.setShowVerticalLines(false);
        contactTable.setIntercellSpacing(new Dimension(0, 1));
        contactTable.setFillsViewportHeight(true);

        // Column widths
        contactTable.getColumnModel().getColumn(0).setMaxWidth(40);   // index
        contactTable.getColumnModel().getColumn(1).setPreferredWidth(160); // name
        contactTable.getColumnModel().getColumn(2).setPreferredWidth(120); // phone
        contactTable.getColumnModel().getColumn(3).setPreferredWidth(160); // email
        contactTable.getColumnModel().getColumn(4).setPreferredWidth(120); // company
        contactTable.getColumnModel().getColumn(5).setPreferredWidth(100); // group
        contactTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // completion

        // Header styling
        JTableHeader header = contactTable.getTableHeader();
        header.setFont(UIConstants.FONT_TABLE_HEADER);
        header.setBackground(UIConstants.BG_CARD);
        header.setForeground(UIConstants.TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.BORDER));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        // Custom renderer for group column
        contactTable.getColumnModel().getColumn(5).setCellRenderer(new GroupCellRenderer());
        contactTable.getColumnModel().getColumn(6).setCellRenderer(new CompletionCellRenderer());
        contactTable.getColumnModel().getColumn(0).setCellRenderer(new IndexCellRenderer());

        // Alternating row colors
        contactTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
                }
                c.setForeground(UIConstants.TEXT_PRIMARY);
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        // Double-click to edit
        contactTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedContact();
                }
            }
        });

        // Right-click context menu
        contactTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = contactTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        contactTable.setRowSelectionInterval(row, row);
                        createContextMenu().show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(contactTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        scrollPane.getViewport().setBackground(UIConstants.BG_SECONDARY);
        add(scrollPane, BorderLayout.CENTER);

        // === BOTTOM STATUS BAR ===
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel = new JLabel("Đang tải...");
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomActions.setOpaque(false);

        JButton editBtn = createStyledButton("✏ Sửa", UIConstants.BG_CARD);
        editBtn.addActionListener(e -> editSelectedContact());

        JButton deleteBtn = createStyledButton("🗑 Xóa", UIConstants.DANGER);
        deleteBtn.addActionListener(e -> deleteSelectedContact());

        bottomActions.add(editBtn);
        bottomActions.add(deleteBtn);

        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(bottomActions, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.setBackground(UIConstants.BG_INPUT);
        searchField.setForeground(UIConstants.TEXT_PRIMARY);
        searchField.setCaretColor(UIConstants.TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Placeholder text
        searchField.setText("🔍 Tìm kiếm theo tên, SĐT, email...");
        searchField.setForeground(UIConstants.TEXT_MUTED);
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().startsWith("🔍")) {
                    searchField.setText("");
                    searchField.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("🔍 Tìm kiếm theo tên, SĐT, email...");
                    searchField.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });

        // Search on Enter
        searchField.addActionListener(e -> performSearch());

        JButton searchBtn = createStyledButton("Tìm", UIConstants.ACCENT);
        searchBtn.setPreferredSize(new Dimension(70, 38));
        searchBtn.addActionListener(e -> performSearch());

        panel.add(searchField, BorderLayout.CENTER);
        panel.add(searchBtn, BorderLayout.EAST);
        return panel;
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("✏ Sửa liên hệ");
        editItem.addActionListener(e -> editSelectedContact());
        JMenuItem deleteItem = new JMenuItem("🗑 Xóa liên hệ");
        deleteItem.addActionListener(e -> deleteSelectedContact());
        JMenu groupMenu = new JMenu("📂 Chuyển nhóm");
        for (GroupInfo g : allGroups) {
            JMenuItem gi = new JMenuItem(g.toString());
            gi.addActionListener(e -> changeSelectedGroupById(g.getId()));
            groupMenu.add(gi);
        }
        menu.add(editItem);
        menu.add(groupMenu);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    // === ACTIONS ===

    public void loadContacts() {
        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.getAllContacts();
            }
            @Override
            protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText("Tổng: " + currentContacts.size() + " liên hệ");
                } catch (Exception e) {
                    statusLabel.setText("❌ Lỗi tải dữ liệu: " + e.getMessage());
                    JOptionPane.showMessageDialog(ContactPanel.this,
                            "Không thể tải danh bạ từ Supabase.\n" + e.getMessage(),
                            "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.startsWith("🔍") || keyword.isEmpty()) {
            loadContacts();
            return;
        }

        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.searchContacts(keyword);
            }
            @Override
            protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText("Tìm thấy: " + currentContacts.size() + " liên hệ");
                } catch (Exception e) {
                    statusLabel.setText("❌ Lỗi tìm kiếm");
                }
            }
        };
        worker.execute();
    }

    /**
     * Load danh sách nhóm từ DB vào dropdown filter.
     */
    private void loadGroupFilter() {
        SwingWorker<List<GroupInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<GroupInfo> doInBackground() throws Exception {
                return contactService.getAllGroups();
            }
            @Override
            protected void done() {
                try {
                    allGroups = get();
                    groupFilter.removeAllItems();
                    groupFilter.addItem("Tất cả");
                    for (GroupInfo g : allGroups) {
                        groupFilter.addItem(g.getIcon() + " " + g.getDisplayName());
                    }
                } catch (Exception e) {
                    // Fallback: keep "Tất cả"
                }
            }
        };
        worker.execute();
    }

    private void filterByGroup() {
        int index = groupFilter.getSelectedIndex();
        if (index <= 0) {
            loadContacts();
            return;
        }

        if (index - 1 < allGroups.size()) {
            GroupInfo group = allGroups.get(index - 1);
            filterByGroupId(group.getId(), group.getDisplayName());
        }
    }

    /**
     * Filter contacts bằng group_id (gọi từ sidebar hoặc dropdown).
     */
    public void filterByGroupId(int groupId, String groupName) {
        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.getContactsByGroupId(groupId);
            }
            @Override
            protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText(groupName + ": " + currentContacts.size() + " liên hệ");
                } catch (Exception e) {
                    statusLabel.setText("❌ Lỗi lọc nhóm");
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        ContactDialog dialog = new ContactDialog((Frame) parent, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    contactService.addContact(dialog.getContact(), dialog.getCompanyName());
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        loadContacts();
                        if (onRefresh != null) onRefresh.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ContactPanel.this,
                                "Lỗi thêm liên hệ: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void editSelectedContact() {
        int row = contactTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một liên hệ.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Contact contact = currentContacts.get(row);
        Window parent = SwingUtilities.getWindowAncestor(this);
        ContactDialog dialog = new ContactDialog((Frame) parent, contact);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    contactService.updateContact(dialog.getContact(), dialog.getCompanyName());
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        loadContacts();
                        if (onRefresh != null) onRefresh.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ContactPanel.this,
                                "Lỗi cập nhật: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void deleteSelectedContact() {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact contact = currentContacts.get(row);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa liên hệ \"" + contact.getName() + "\"?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    contactService.deleteContact(contact.getId());
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        loadContacts();
                        if (onRefresh != null) onRefresh.run();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ContactPanel.this,
                                "Lỗi xóa: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void changeSelectedGroupById(int groupId) {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact contact = currentContacts.get(row);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                contactService.changeGroupById(contact, groupId);
                return null;
            }
            @Override
            protected void done() {
                loadContacts();
                if (onRefresh != null) onRefresh.run();
            }
        };
        worker.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < currentContacts.size(); i++) {
            Contact c = currentContacts.get(i);
            tableModel.addRow(new Object[]{
                    i + 1,
                    c.getName(),
                    c.getPhone() != null ? c.getPhone() : "",
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getCompanyName() != null ? c.getCompanyName() : "",
                    getGroupDisplayForContact(c),
                    c.getCompletionPercent()
            });
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, 36));

        btn.addMouseListener(new MouseAdapter() {
            Color original = bg;
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(original.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(original);
            }
        });
        return btn;
    }

    // === Custom Cell Renderers ===

    private static class IndexCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            setForeground(UIConstants.TEXT_MUTED);
            setFont(UIConstants.FONT_SMALL);
            if (!isSelected) {
                setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            return this;
        }
    }

    /**
     * Tìm GroupInfo display name cho contact từ allGroups list.
     */
    private String getGroupDisplayForContact(Contact c) {
        for (GroupInfo g : allGroups) {
            if (g.getId() == c.getGroupId()) {
                return g.toString();
            }
        }
        return c.getGroup().toString(); // fallback to enum
    }

    /**
     * Tìm Color cho group_id từ allGroups list.
     */
    private Color getGroupColorForId(int groupId) {
        for (GroupInfo g : allGroups) {
            if (g.getId() == groupId) {
                return g.getColor();
            }
        }
        return UIConstants.TEXT_MUTED;
    }

    private class GroupCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof String) {
                setText((String) value);
                // Find color from allGroups by matching display text
                if (row < currentContacts.size()) {
                    int gid = currentContacts.get(row).getGroupId();
                    setForeground(getGroupColorForId(gid));
                }
                setFont(UIConstants.FONT_SMALL_BOLD);
            }
            if (!isSelected) {
                setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
            }
            setHorizontalAlignment(CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            return this;
        }
    }

    private static class CompletionCellRenderer extends JPanel implements TableCellRenderer {
        private int percent;
        private boolean selected;

        public CompletionCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            this.percent = value instanceof Integer ? (Integer) value : 0;
            this.selected = isSelected;
            if (isSelected) {
                setBackground(UIConstants.BG_SELECTED);
            } else {
                setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 20;
            int h = 8;
            int x = 10;
            int y = (getHeight() - h) / 2;

            // Background bar
            g2.setColor(UIConstants.BG_HOVER);
            g2.fillRoundRect(x, y, w, h, h, h);

            // Progress bar
            Color barColor = percent >= 80 ? UIConstants.SUCCESS :
                    percent >= 50 ? UIConstants.WARNING : UIConstants.DANGER;
            g2.setColor(barColor);
            g2.fillRoundRect(x, y, (int) (w * percent / 100.0), h, h, h);

            // Text
            g2.setColor(UIConstants.TEXT_SECONDARY);
            g2.setFont(UIConstants.FONT_SMALL);
            String text = percent + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x + w + 2 - fm.stringWidth(text) - 2, y + h + fm.getAscent() - 2);

            g2.dispose();
        }
    }
}
