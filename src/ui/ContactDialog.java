package ui;

import model.Contact;
import model.GroupInfo;
import service.ContactService;

import javax.swing.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
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
    private JLabel avatarLabel;
    private String tempAvatarBase64;

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

        // Avatar section
        tempAvatarBase64 = contact.getAvatar();
        JPanel avatarPanel = createAvatarPanel();
        mainPanel.add(avatarPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Form fields
        nameField = addFormField(mainPanel, "Họ và tên *", "Nhập họ và tên...");
        phoneField = addFormField(mainPanel, "Số điện thoại", "VD: 0901234567");
        emailField = addFormField(mainPanel, "Email", "VD: example@gmail.com");
        addressField = addFormField(mainPanel, "Địa chỉ", "Nhập địa chỉ...");
        birthdayField = addFormField(mainPanel, "Ngày sinh", "VD: 01/01/2000");
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

        // Notes Header
        JPanel notesHeader = new JPanel(new BorderLayout());
        notesHeader.setOpaque(false);
        notesHeader.setAlignmentX(LEFT_ALIGNMENT);
        notesHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel notesLabel = new JLabel("Ghi chú & Lịch sử tương tác");
        notesLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        notesLabel.setForeground(UIConstants.TEXT_SECONDARY);
        notesHeader.add(notesLabel, BorderLayout.WEST);

        JButton timeBtn = new JButton("🕒 Thêm ngày giờ");
        timeBtn.setFont(UIConstants.FONT_SMALL);
        timeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        timeBtn.setBackground(UIConstants.BG_CARD);
        timeBtn.setFocusPainted(false);
        timeBtn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        timeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        timeBtn.addActionListener(e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String prefix = notesArea.getText().trim().isEmpty() ? "" : "\n\n-------------------\n";
            notesArea.append(prefix + "[" + now.format(formatter) + "] - ");
            notesArea.requestFocus();
        });
        notesHeader.add(timeBtn, BorderLayout.EAST);

        mainPanel.add(notesHeader);
        mainPanel.add(Box.createVerticalStrut(4));

        notesArea = new JTextArea(6, 20);
        notesArea.setFont(UIConstants.FONT_BODY);
        notesArea.setBackground(UIConstants.BG_INPUT);
        notesArea.setForeground(UIConstants.TEXT_PRIMARY);
        notesArea.setCaretColor(UIConstants.TEXT_PRIMARY);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
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
        
        setupAutoFormatting();
    }

    private void setupAutoFormatting() {
        // Birthday Auto-Slash
        birthdayField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private boolean isUpdating = false;

            private void handle() {
                if (isUpdating) return;
                SwingUtilities.invokeLater(() -> {
                    if (isUpdating) return;
                    isUpdating = true;
                    String text = birthdayField.getText();
                    if (!text.startsWith("VD:") && !text.isEmpty()) {
                        if (text.length() == 2 && !text.contains("/")) {
                            birthdayField.setText(text + "/");
                        } else if (text.length() == 5 && text.charAt(2) == '/' && text.indexOf('/', 3) == -1) {
                            birthdayField.setText(text + "/");
                        }
                    }
                    isUpdating = false;
                });
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { handle(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });

        // Email Autocomplete
        emailField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private boolean isUpdating = false;

            private void handle() {
                if (isUpdating) return;
                SwingUtilities.invokeLater(() -> {
                    if (isUpdating) return;
                    isUpdating = true;
                    String text = emailField.getText();
                    if (!text.startsWith("VD:") && !text.isEmpty()) {
                        int atIndex = text.indexOf('@');
                        if (atIndex != -1) {
                            String prefix = text.substring(0, atIndex);
                            String suffix = text.substring(atIndex);
                            String[] domains = {"@gmail.com", "@yahoo.com", "@outlook.com", "@hotmail.com", "@icloud.com"};
                            for (String domain : domains) {
                                if (domain.toLowerCase().startsWith(suffix.toLowerCase()) && !domain.equalsIgnoreCase(suffix)) {
                                    emailField.setText(prefix + domain);
                                    emailField.setSelectionStart(prefix.length() + suffix.length());
                                    emailField.setSelectionEnd(emailField.getText().length());
                                    break;
                                }
                            }
                        }
                    }
                    isUpdating = false;
                });
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { handle(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
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
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

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
        setFieldValue(birthdayField, contact.getBirthday(), "VD: 01/01/2000");
        setFieldValue(companyField, contact.getCompanyName(), "Nhập tên công ty...");
        if (contact.getNotes() != null)
            notesArea.setText(contact.getNotes());

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
        if (text.equals(placeholder) || text.isEmpty())
            return "";
        return text;
    }

    private void saveContact() {
        String name = getFieldValue(nameField, "Nhập họ và tên...");
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ và tên!", "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }

        contact.setName(name);
        contact.setPhone(getFieldValue(phoneField, "VD: 0901234567"));
        contact.setEmail(getFieldValue(emailField, "VD: example@gmail.com"));
        contact.setAddress(getFieldValue(addressField, "Nhập địa chỉ..."));

        // Chuyển đổi ngày sinh sang format yyyy-MM-dd
        String rawBirthday = getFieldValue(birthdayField, "VD: 01/01/2000");
        contact.setBirthday(convertDateFormat(rawBirthday));

        contact.setAvatar(tempAvatarBase64);
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
        if (date == null || date.isBlank())
            return "";
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
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // --- AVATAR HELPERS ---
    private JPanel createAvatarPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);

        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(80, 80));
        avatarLabel.setMinimumSize(new Dimension(80, 80));
        avatarLabel.setMaximumSize(new Dimension(80, 80));
        updateAvatarDisplay();

        JButton chooseBtn = createButton("🖼 Chọn ảnh", UIConstants.BG_CARD);
        chooseBtn.setPreferredSize(new Dimension(100, 36));
        chooseBtn.addActionListener(e -> chooseAvatar());

        panel.add(avatarLabel);
        panel.add(chooseBtn);
        return panel;
    }

    private void updateAvatarDisplay() {
        try {
            if (tempAvatarBase64 != null && !tempAvatarBase64.isBlank()) {
                byte[] decodedBytes = Base64.getDecoder().decode(tempAvatarBase64);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(decodedBytes));
                if (img != null) {
                    BufferedImage circleImg = makeRoundedImage(img, 80);
                    avatarLabel.setIcon(new ImageIcon(circleImg));
                    avatarLabel.setText("");
                    return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Fallback or empty
        avatarLabel.setIcon(null);
        avatarLabel.setText("👤");
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(UIConstants.BG_INPUT);
        avatarLabel.setForeground(UIConstants.TEXT_MUTED);
        avatarLabel.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
    }

    private void chooseAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh đại diện");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    // Cắt ảnh thành hình vuông trước khi thu nhỏ
                    int minDim = Math.min(img.getWidth(), img.getHeight());
                    int x = (img.getWidth() - minDim) / 2;
                    int y = (img.getHeight() - minDim) / 2;
                    BufferedImage cropped = img.getSubimage(x, y, minDim, minDim);

                    // Thu nhỏ về 160x160 (chất lượng cao)
                    Image scaled = cropped.getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                    BufferedImage resizedImg = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = resizedImg.createGraphics();
                    g2.drawImage(scaled, 0, 0, null);
                    g2.dispose();

                    // Convert ra Base64 png (để hỗ trợ nền trong suốt nếu có)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(resizedImg, "png", baos);
                    tempAvatarBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    updateAvatarDisplay();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể đọc file ảnh", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private BufferedImage makeRoundedImage(BufferedImage img, int size) {
        BufferedImage rounded = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = rounded.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        g2.fill(new Ellipse2D.Float(0, 0, size, size));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(img, 0, 0, size, size, null);
        g2.dispose();
        return rounded;
    }

    public boolean isSaved() {
        return saved;
    }

    public Contact getContact() {
        return contact;
    }

    public String getCompanyName() {
        return companyName;
    }
}
