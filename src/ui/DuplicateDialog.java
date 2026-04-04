package ui;

import model.Contact;
import service.ContactService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Dialog phát hiện và hợp nhất liên hệ trùng lặp.
 */
public class DuplicateDialog extends JDialog {
    private final ContactService contactService;
    private JPanel duplicateListPanel;
    private JLabel statusLabel;
    private List<List<Contact>> duplicateGroups;
    private boolean changed = false;

    public DuplicateDialog(Frame parent) {
        super(parent, "🔀 Hợp nhất liên hệ trùng lặp", true);
        this.contactService = ContactService.getInstance();
        setSize(650, 550);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);
        initComponents();
        loadDuplicates();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIConstants.BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("🔀 Phát hiện liên hệ trùng lặp");
        title.setFont(UIConstants.FONT_SUBTITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel desc = new JLabel("Các liên hệ có cùng tên hoặc số điện thoại sẽ được nhóm lại.");
        desc.setFont(UIConstants.FONT_SMALL);
        desc.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(desc);

        headerPanel.add(headerText, BorderLayout.CENTER);

        JButton refreshBtn = createButton("🔍 Quét lại", UIConstants.ACCENT);
        refreshBtn.addActionListener(e -> loadDuplicates());
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Duplicate list
        duplicateListPanel = new JPanel();
        duplicateListPanel.setLayout(new BoxLayout(duplicateListPanel, BoxLayout.Y_AXIS));
        duplicateListPanel.setBackground(UIConstants.BG_PRIMARY);

        JScrollPane scrollPane = new JScrollPane(duplicateListPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        scrollPane.getViewport().setBackground(UIConstants.BG_PRIMARY);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        statusLabel = new JLabel("Đang quét...");
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomActions.setOpaque(false);

        JButton mergeAllBtn = createButton("🔀 Hợp nhất tất cả", UIConstants.SUCCESS);
        mergeAllBtn.addActionListener(e -> mergeAll());

        JButton closeBtn = createButton("Đóng", UIConstants.BG_CARD);
        closeBtn.addActionListener(e -> dispose());

        bottomActions.add(mergeAllBtn);
        bottomActions.add(closeBtn);

        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(bottomActions, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void loadDuplicates() {
        duplicateListPanel.removeAll();
        statusLabel.setText("Đang quét...");

        SwingWorker<List<List<Contact>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<List<Contact>> doInBackground() throws Exception {
                return contactService.findDuplicates();
            }
            @Override
            protected void done() {
                try {
                    duplicateGroups = get();
                    displayDuplicates();
                    statusLabel.setText("Tìm thấy " + duplicateGroups.size() + " nhóm trùng lặp");
                } catch (Exception e) {
                    statusLabel.setText("❌ Lỗi: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void displayDuplicates() {
        duplicateListPanel.removeAll();

        if (duplicateGroups.isEmpty()) {
            JLabel noData = new JLabel("✅ Không có liên hệ trùng lặp nào!");
            noData.setFont(UIConstants.FONT_BODY);
            noData.setForeground(UIConstants.SUCCESS);
            noData.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));
            noData.setAlignmentX(CENTER_ALIGNMENT);
            duplicateListPanel.add(noData);
        } else {
            for (int i = 0; i < duplicateGroups.size(); i++) {
                duplicateListPanel.add(createDuplicateCard(duplicateGroups.get(i), i + 1));
                duplicateListPanel.add(Box.createVerticalStrut(8));
            }
        }

        duplicateListPanel.revalidate();
        duplicateListPanel.repaint();
    }

    private JPanel createDuplicateCard(List<Contact> group, int index) {
        JPanel card = new JPanel(new BorderLayout(10, 6));
        card.setBackground(UIConstants.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Left: info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel groupTitle = new JLabel("Nhóm #" + index + " — " + group.size() + " liên hệ trùng");
        groupTitle.setFont(UIConstants.FONT_BODY_BOLD);
        groupTitle.setForeground(UIConstants.WARNING);
        infoPanel.add(groupTitle);
        infoPanel.add(Box.createVerticalStrut(6));

        for (Contact c : group) {
            String info = "• " + safe(c.getName()) + "  |  " + safe(c.getPhone()) + "  |  " + safe(c.getEmail());
            JLabel contactLabel = new JLabel(info);
            contactLabel.setFont(UIConstants.FONT_SMALL);
            contactLabel.setForeground(UIConstants.TEXT_SECONDARY);
            infoPanel.add(contactLabel);
        }

        card.add(infoPanel, BorderLayout.CENTER);

        // Right: merge button
        JButton mergeBtn = createButton("Hợp nhất", UIConstants.ACCENT);
        mergeBtn.setPreferredSize(new Dimension(100, 34));
        mergeBtn.addActionListener(e -> mergeGroup(group));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(mergeBtn);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    private void mergeGroup(List<Contact> group) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hợp nhất " + group.size() + " liên hệ thành 1?\nGiữ lại: " + group.get(0).getName(),
                "Xác nhận hợp nhất", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                contactService.mergeContacts(group);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    changed = true;
                    loadDuplicates();
                    JOptionPane.showMessageDialog(DuplicateDialog.this,
                            "Đã hợp nhất thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DuplicateDialog.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void mergeAll() {
        if (duplicateGroups == null || duplicateGroups.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Hợp nhất tất cả " + duplicateGroups.size() + " nhóm trùng lặp?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (List<Contact> group : duplicateGroups) {
                    contactService.mergeContacts(group);
                }
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    changed = true;
                    loadDuplicates();
                    JOptionPane.showMessageDialog(DuplicateDialog.this,
                            "Đã hợp nhất tất cả!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DuplicateDialog.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
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
