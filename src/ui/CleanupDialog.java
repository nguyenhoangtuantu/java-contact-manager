package ui;

import model.Contact;
import service.ContactService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Dialog dọn dẹp định kỳ: tìm liên hệ thiếu thông tin, cũ, không có SĐT.
 */
public class CleanupDialog extends JDialog {
    private final ContactService contactService;
    private JTabbedPane tabbedPane;
    private boolean changed = false;

    // Tab panels
    private JPanel incompletePanel;
    private JPanel stalePanel;
    private JPanel noPhonePanel;

    public CleanupDialog(Frame parent) {
        super(parent, "Dọn dẹp danh bạ", true);
        this.contactService = ContactService.getInstance();
        setSize(700, 550);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);
        initComponents();
        loadAllTabs();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIConstants.BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Dọn dẹp danh bạ định kỳ");
        title.setFont(UIConstants.FONT_SUBTITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel desc = new JLabel("Tìm và xóa các liên hệ thiếu thông tin, lâu không cập nhật, hoặc không có SĐT.");
        desc.setFont(UIConstants.FONT_SMALL);
        desc.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(desc);
        headerPanel.add(headerText, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIConstants.FONT_BODY);
        tabbedPane.setBackground(UIConstants.BG_SECONDARY);
        tabbedPane.setForeground(UIConstants.TEXT_PRIMARY);

        incompletePanel = createTabPanel();
        stalePanel = createTabPanel();
        noPhonePanel = createTabPanel();

        tabbedPane.addTab("Thiếu thông tin", incompletePanel);
        tabbedPane.addTab("Lâu không cập nhật", stalePanel);
        tabbedPane.addTab("Không có SĐT", noPhonePanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton refreshBtn = createButton("Quét lại", UIConstants.ACCENT);
        refreshBtn.addActionListener(e -> loadAllTabs());

        JButton closeBtn = createButton("Đóng", UIConstants.BG_CARD);
        closeBtn.addActionListener(e -> dispose());

        bottomPanel.add(refreshBtn);
        bottomPanel.add(closeBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createTabPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BG_PRIMARY);
        return panel;
    }

    private void loadAllTabs() {
        loadIncomplete();
        loadStale();
        loadNoPhone();
    }

    private void loadIncomplete() {
        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.findIncompleteContacts();
            }
            @Override
            protected void done() {
                try {
                    List<Contact> list = get();
                    buildContactListPanel(incompletePanel, list,
                            "Liên hệ có ít hơn 3 trường thông tin được điền:");
                } catch (Exception e) {
                    showError(incompletePanel, e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadStale() {
        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.findStaleContacts(90); // 90 ngày
            }
            @Override
            protected void done() {
                try {
                    List<Contact> list = get();
                    buildContactListPanel(stalePanel, list,
                            "Liên hệ không được cập nhật trong 90 ngày:");
                } catch (Exception e) {
                    showError(stalePanel, e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadNoPhone() {
        SwingWorker<List<Contact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Contact> doInBackground() throws Exception {
                return contactService.findContactsWithoutPhone();
            }
            @Override
            protected void done() {
                try {
                    List<Contact> list = get();
                    buildContactListPanel(noPhonePanel, list,
                            "Liên hệ không có số điện thoại:");
                } catch (Exception e) {
                    showError(noPhonePanel, e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void buildContactListPanel(JPanel panel, List<Contact> contacts, String description) {
        panel.removeAll();

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(UIConstants.BG_PRIMARY);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Description + count
        JLabel descLabel = new JLabel(description + " (" + contacts.size() + " liên hệ)");
        descLabel.setFont(UIConstants.FONT_SMALL);
        descLabel.setForeground(UIConstants.TEXT_SECONDARY);
        content.add(descLabel, BorderLayout.NORTH);

        if (contacts.isEmpty()) {
            JLabel noData = new JLabel("Không có liên hệ nào cần dọn dẹp!");
            noData.setFont(UIConstants.FONT_BODY);
            noData.setForeground(UIConstants.SUCCESS);
            noData.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
            noData.setHorizontalAlignment(SwingConstants.CENTER);
            content.add(noData, BorderLayout.CENTER);
        } else {
            // Table
            String[] cols = {"✓", "Tên", "SĐT", "Email", "Hoàn thiện"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override
                public Class<?> getColumnClass(int col) {
                    return col == 0 ? Boolean.class : Object.class;
                }
                @Override
                public boolean isCellEditable(int row, int col) {
                    return col == 0;
                }
            };

            for (Contact c : contacts) {
                model.addRow(new Object[]{
                        false,
                        c.getName(),
                        safe(c.getPhone()),
                        safe(c.getEmail()),
                        c.getCompletionPercent() + "%"
                });
            }

            JTable table = new JTable(model);
            table.setFont(UIConstants.FONT_TABLE);
            table.setRowHeight(36);
            table.setBackground(UIConstants.BG_SECONDARY);
            table.setForeground(UIConstants.TEXT_PRIMARY);
            table.setSelectionBackground(UIConstants.BG_SELECTED);
            table.setGridColor(UIConstants.BORDER);
            table.getColumnModel().getColumn(0).setMaxWidth(40);

            JTableHeader header = table.getTableHeader();
            header.setFont(UIConstants.FONT_TABLE_HEADER);
            header.setBackground(UIConstants.BG_CARD);
            header.setForeground(UIConstants.TEXT_SECONDARY);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
            scroll.getViewport().setBackground(UIConstants.BG_SECONDARY);
            content.add(scroll, BorderLayout.CENTER);

            // Delete selected button
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actionPanel.setOpaque(false);

            JButton selectAllBtn = createButton("Chọn tất cả", UIConstants.BG_CARD);
            selectAllBtn.addActionListener(e -> {
                for (int i = 0; i < model.getRowCount(); i++) {
                    model.setValueAt(true, i, 0);
                }
            });

            JButton deleteBtn = createButton("Xóa đã chọn", UIConstants.DANGER);
            deleteBtn.addActionListener(e -> {
                List<Contact> toDelete = new ArrayList<>();
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (Boolean.TRUE.equals(model.getValueAt(i, 0))) {
                        toDelete.add(contacts.get(i));
                    }
                }
                if (toDelete.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn liên hệ cần xóa.");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Xóa " + toDelete.size() + " liên hệ đã chọn?",
                        "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteContacts(toDelete, panel);
                }
            });

            actionPanel.add(selectAllBtn);
            actionPanel.add(deleteBtn);
            content.add(actionPanel, BorderLayout.SOUTH);
        }

        panel.add(content, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    private void deleteContacts(List<Contact> contacts, JPanel tabPanel) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                contactService.deleteMultiple(contacts);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    changed = true;
                    loadAllTabs();
                    JOptionPane.showMessageDialog(CleanupDialog.this,
                            "Đã xóa " + contacts.size() + " liên hệ!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CleanupDialog.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showError(JPanel panel, String message) {
        panel.removeAll();
        JLabel errorLabel = new JLabel("❌ Lỗi: " + message);
        errorLabel.setFont(UIConstants.FONT_BODY);
        errorLabel.setForeground(UIConstants.DANGER);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(errorLabel);
        panel.revalidate();
        panel.repaint();
    }

    private String safe(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    public boolean isChanged() { return changed; }
}
