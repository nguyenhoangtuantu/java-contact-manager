package ui;

import model.Contact;
import model.GroupInfo;
import service.ContactService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog thêm/sửa liên hệ chi tiết với form nhập liệu.
 * Hỗ trợ nhập tên công ty (sẽ tự động tạo/liên kết trong bảng companies).
 * Load nhóm ưu tiên từ DB (dynamic, không giới hạn).
 */
public class ContactDialog extends JDialog {
    private Contact contact;
    private boolean saved = false;
    private String companyName = "";
    private List<GroupInfo> groups = new ArrayList<>();

    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField birthdayField;
    private JTextField companyField;
    private JTextArea notesArea;
    private JComboBox<String> groupCombo;

    public ContactDialog(Frame parent, Contact existing) {
        super(parent, existing == null ? "Thêm liên hệ mới" : "Sửa liên hệ", true);
        this.contact = existing != null ? existing : new Contact();

        // Load groups from DB
        try {
            groups = ContactService.getInstance().getAllGroups();
        } catch (Exception e) {
            // Fallback
        }

        setSize(520, 620);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(UIConstants.BG_PRIMARY);

        initComponents();
        if (existing != null) {
            populateFields();
        }
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(UIConstants.BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Header
        JLabel header = new JLabel(contact.getName() != null ? "✏️ Sửa liên hệ" : "➕ Thêm liên hệ mới");
        header.setFont(UIConstants.FONT_SUBTITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(header);
        mainPanel.add(Box.createVerticalStrut(20));

        // Form fields
        nameField = addFormField(mainPanel, "Họ và tên *", "Nhập họ và tên...");
        phoneField = addFormField(mainPanel, "Số điện thoại", "VD: 0901234567");
        emailField = addFormField(mainPanel, "Email", "VD: example@gmail.com");
        addressField = addFormField(mainPanel, "Địa chỉ", "Nhập địa chỉ...");
        birthdayField = addFormField(mainPanel, "Ngày sinh", "VD: 27/11/2000 hoặc 2000-11-27");
        companyField = addFormField(mainPanel, "Công ty", "Nhập tên công ty...");

        // Group combo (dynamic from DB)
        JLabel groupLabel = new JLabel("Nhóm ưu tiên");
        groupLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        groupLabel.setForeground(UIConstants.TEXT_SECONDARY);
        groupLabel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(groupLabel);
        mainPanel.add(Box.createVerticalStrut(4));

        groupCombo = new JComboBox<>();
        for (GroupInfo g : groups) {
            groupCombo.addItem(g.getIcon() + " " + g.getDisplayName());
        }
        groupCombo.setFont(UIConstants.FONT_BODY);
        groupCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        groupCombo.setAlignmentX(LEFT_ALIGNMENT);
        // Default: select last group (OTHER)
        if (!groups.isEmpty()) {
            groupCombo.setSelectedIndex(groups.size() - 1);
        }
        mainPanel.add(groupCombo);
        mainPanel.add(Box.createVerticalStrut(12));

        // Notes
        JLabel notesLabel = new JLabel("Ghi chú");
        notesLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        notesLabel.setForeground(UIConstants.TEXT_SECONDARY);
        notesLabel.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(notesLabel);
        mainPanel.add(Box.createVerticalStrut(4));

        notesArea = new JTextArea(3, 20);
        notesArea.setFont(UIConstants.FONT_BODY);
        notesArea.setBackground(UIConstants.BG_INPUT);
        notesArea.setForeground(UIConstants.TEXT_PRIMARY);
        notesArea.setCaretColor(UIConstants.TEXT_PRIMARY);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        notesScroll.setAlignmentX(LEFT_ALIGNMENT);
        mainPanel.add(notesScroll);
        mainPanel.add(Box.createVerticalStrut(24));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton cancelBtn = createButton("Hủy", UIConstants.BG_CARD);
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = createButton("💾 Lưu", UIConstants.ACCENT);
        saveBtn.addActionListener(e -> saveContact());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        mainPanel.add(buttonPanel);

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_PRIMARY);
        setContentPane(scroll);
    }

    private JTextField addFormField(JPanel parent, String labelText, String placeholder) {
        JLabel label = new JLabel(labelText);
        label.setFont(UIConstants.FONT_SMALL_BOLD);
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(4));

        JTextField field = new JTextField();
        field.setFont(UIConstants.FONT_BODY);
        field.setBackground(UIConstants.BG_INPUT);
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setCaretColor(UIConstants.TEXT_PRIMARY);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // Placeholder
        field.setText(placeholder);
        field.setForeground(UIConstants.TEXT_MUTED);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });

        parent.add(field);
        parent.add(Box.createVerticalStrut(12));
        return field;
    }

    private void populateFields() {
        setFieldValue(nameField, contact.getName(), "Nhập họ và tên...");
        setFieldValue(phoneField, contact.getPhone(), "VD: 0901234567");
        setFieldValue(emailField, contact.getEmail(), "VD: example@gmail.com");
        setFieldValue(addressField, contact.getAddress(), "Nhập địa chỉ...");
        setFieldValue(birthdayField, contact.getBirthday(), "VD: 27/11/2000 hoặc 2000-11-27");
        setFieldValue(companyField, contact.getCompanyName(), "Nhập tên công ty...");
        if (contact.getNotes() != null) notesArea.setText(contact.getNotes());

        // Select correct group in combo
        int groupId = contact.getGroupId();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getId() == groupId) {
                groupCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setFieldValue(JTextField field, String value, String placeholder) {
        if (value != null && !value.isBlank()) {
            field.setText(value);
            field.setForeground(UIConstants.TEXT_PRIMARY);
        }
    }

    private String getFieldValue(JTextField field, String placeholder) {
        String text = field.getText().trim();
        if (text.equals(placeholder) || text.isEmpty()) return "";
        return text;
    }

    private void saveContact() {
        String name = getFieldValue(nameField, "Nhập họ và tên...");
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ và tên!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        contact.setName(name);
        contact.setPhone(getFieldValue(phoneField, "VD: 0901234567"));
        contact.setEmail(getFieldValue(emailField, "VD: example@gmail.com"));
        contact.setAddress(getFieldValue(addressField, "Nhập địa chỉ..."));

        // Chuyển đổi ngày sinh sang format yyyy-MM-dd
        String rawBirthday = getFieldValue(birthdayField, "VD: 27/11/2000 hoặc 2000-11-27");
        contact.setBirthday(convertDateFormat(rawBirthday));

        contact.setNotes(notesArea.getText().trim());

        // Set group_id từ combo selection
        int selectedIdx = groupCombo.getSelectedIndex();
        if (selectedIdx >= 0 && selectedIdx < groups.size()) {
            contact.setGroupId(groups.get(selectedIdx).getId());
        }

        // Lưu tên công ty
        companyName = getFieldValue(companyField, "Nhập tên công ty...");

        saved = true;
        dispose();
    }

    /**
     * Chuyển đổi ngày từ nhiều format sang yyyy-MM-dd (PostgreSQL).
     */
    private String convertDateFormat(String date) {
        if (date == null || date.isBlank()) return "";
        date = date.trim();

        if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            String[] parts = date.split("-");
            return parts[0] + "-" + padZero(parts[1]) + "-" + padZero(parts[2]);
        }
        if (date.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] parts = date.split("/");
            return parts[2] + "-" + padZero(parts[1]) + "-" + padZero(parts[0]);
        }
        if (date.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
            String[] parts = date.split("-");
            return parts[2] + "-" + padZero(parts[1]) + "-" + padZero(parts[0]);
        }
        if (date.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            String[] parts = date.split("/");
            return expandYear(parts[2]) + "-" + padZero(parts[1]) + "-" + padZero(parts[0]);
        }
        if (date.matches("\\d{1,2}-\\d{1,2}-\\d{2}")) {
            String[] parts = date.split("-");
            return expandYear(parts[2]) + "-" + padZero(parts[1]) + "-" + padZero(parts[0]);
        }
        JOptionPane.showMessageDialog(this,
                "Định dạng ngày không hợp lệ: " + date,
                "Lỗi ngày sinh", JOptionPane.WARNING_MESSAGE);
        return "";
    }

    private String expandYear(String yy) {
        int y = Integer.parseInt(yy);
        return (y <= 30 ? "20" : "19") + padZero(yy);
    }

    private String padZero(String s) {
        return s.length() == 1 ? "0" + s : s;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 38));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    public boolean isSaved() { return saved; }
    public Contact getContact() { return contact; }
    public String getCompanyName() { return companyName; }
}
