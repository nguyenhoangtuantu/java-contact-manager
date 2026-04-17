package ui;

import model.Contact;
import model.ContactGroup;
import model.GroupInfo;
import service.ContactService;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Panel chính hiển thị danh sách liên hệ.
 * Light-theme redesign: header trắng, bảng card trắng, drop-zone CSV, status bar.
 */
public class ContactPanel extends JPanel {

    private final ContactService contactService;
    private JTable              contactTable;
    private DefaultTableModel   tableModel;
    private JTextField          searchField;
    // Lazy-load pagination
    private final int pageSize = 50; // số bản ghi mỗi trang
    private int currentPage = 1;
    private JPanel paginationPanel;
    private JComboBox<String>   groupFilter;
    private JLabel              statusLabel;
    private JLabel              titleLabel;
    private JButton             addBtn;
    private JButton             editBtn;
    private JButton             deleteBtn;

    private List<Contact>   currentContacts = new ArrayList<>();
    private List<GroupInfo> allGroups       = new ArrayList<>();
    private Runnable        onRefresh;
    private boolean         isTrashMode     = false;

    public ContactPanel() {
        this.contactService = ContactService.getInstance();
        setBackground(UIConstants.BG_PRIMARY);
        setLayout(new BorderLayout(0, 0));
        initComponents();
    }

    public void setOnRefresh(Runnable r) { this.onRefresh = r; }

    // =========================================================
    //  INIT
    // =========================================================
    private void initComponents() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── TOP HEADER BAR ──────────────────────────────────────
    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(UIConstants.BG_SECONDARY);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        // LEFT: title + filter dropdown
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        titleLabel = new JLabel("Danh bạ");
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        loadGroupFilter();
        groupFilter = new JComboBox<>(new String[]{"Tất cả"});
        groupFilter.setFont(UIConstants.FONT_BODY);
        groupFilter.setPreferredSize(new Dimension(130, 32));
        groupFilter.addActionListener(e -> filterByGroup());

        left.add(titleLabel);
        left.add(groupFilter);

        // RIGHT: search + add
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setPreferredSize(new Dimension(240, 32));
        searchField.setBackground(UIConstants.BG_INPUT);
        searchField.setForeground(UIConstants.TEXT_PRIMARY);
        searchField.setCaretColor(UIConstants.ACCENT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        // Placeholder
        final String PLACEHOLDER = "Tìm kiếm theo tên, SĐT, email...";
        searchField.setText(PLACEHOLDER);
        searchField.setForeground(UIConstants.TEXT_MUTED);
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(PLACEHOLDER);
                    searchField.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });
        searchField.addActionListener(e -> performSearch());

        JButton searchBtn = buildBtn("Tìm", UIConstants.BG_SECONDARY, UIConstants.TEXT_SECONDARY, false);
        searchBtn.setPreferredSize(new Dimension(56, 32));
        searchBtn.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1, true));
        searchBtn.addActionListener(e -> performSearch());

        addBtn = buildBtn("+ Thêm liên hệ", UIConstants.ACCENT, Color.WHITE, true);
        addBtn.setPreferredSize(new Dimension(140, 32));
        addBtn.addActionListener(e -> showAddDialog());
        // Nút Export CSV được thêm vào status bar, không cần ở đây

        right.add(searchField);
        right.add(searchBtn);
        right.add(addBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── CENTER (table card + csv zone) ──────────────────────
    private JPanel buildCenter() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(UIConstants.BG_PRIMARY);
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        // ── TABLE CARD ──
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIConstants.BG_SECONDARY);
        card.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1, true));

        String[] cols = {"", "ANH", "TÊN LIÊN HỆ", "SỐ ĐIỆN THOẠI", "EMAIL", "CÔNG TY", "NHÓM", "HOÀN THIỆN"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        contactTable = new JTable(tableModel);
        contactTable.setFont(UIConstants.FONT_TABLE);
        contactTable.setRowHeight(56);
        contactTable.setBackground(UIConstants.BG_SECONDARY);
        contactTable.setForeground(UIConstants.TEXT_PRIMARY);
        contactTable.setSelectionBackground(UIConstants.BG_HOVER);
        contactTable.setSelectionForeground(UIConstants.TEXT_PRIMARY);
        contactTable.setGridColor(UIConstants.BORDER_LIGHT);
        contactTable.setShowHorizontalLines(true);
        contactTable.setShowVerticalLines(false);
        contactTable.setIntercellSpacing(new Dimension(0, 0));
        contactTable.setFillsViewportHeight(true);
        // Enable drag-and-drop row reordering
        contactTable.setDragEnabled(true);
        contactTable.setDropMode(DropMode.INSERT_ROWS);
        contactTable.setTransferHandler(new TableRowTransferHandler(contactTable));

        // Column widths
        contactTable.getColumnModel().getColumn(0).setMaxWidth(36);
        contactTable.getColumnModel().getColumn(0).setMinWidth(36);
        contactTable.getColumnModel().getColumn(1).setMaxWidth(56);
        contactTable.getColumnModel().getColumn(1).setMinWidth(56);
        contactTable.getColumnModel().getColumn(2).setPreferredWidth(170);
        contactTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        contactTable.getColumnModel().getColumn(4).setPreferredWidth(160);
        contactTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        contactTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        contactTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        // Header
        JTableHeader header = contactTable.getTableHeader();
        header.setFont(UIConstants.FONT_TABLE_HEADER);
        header.setBackground(UIConstants.BG_CARD);
        header.setForeground(UIConstants.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Renderers
        contactTable.getColumnModel().getColumn(0).setCellRenderer(new IndexRenderer());
        contactTable.getColumnModel().getColumn(1).setCellRenderer(new AvatarRenderer());
        contactTable.getColumnModel().getColumn(2).setCellRenderer(new NameRenderer());
        contactTable.getColumnModel().getColumn(6).setCellRenderer(new GroupBadgeRenderer());
        contactTable.getColumnModel().getColumn(7).setCellRenderer(new ProgressBarRenderer());

        // Default renderer for other columns
        contactTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setFont(UIConstants.FONT_TABLE);
                setForeground(UIConstants.TEXT_SECONDARY);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!sel) setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
                return this;
            }
        });

        // Double-click
        contactTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (isTrashMode) restoreSelected(); else editSelected();
                }
            }
        });
        // Right-click
        contactTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { tryPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { tryPopup(e); }
            private void tryPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = contactTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    contactTable.setRowSelectionInterval(row, row);
                    buildContextMenu().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane scroll = new JScrollPane(contactTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UIConstants.BG_SECONDARY);
        card.add(scroll, BorderLayout.CENTER);

        wrapper.add(card, BorderLayout.CENTER);
        
        JPanel bottomZone = new JPanel(new BorderLayout(0, 4));
        bottomZone.setOpaque(false);
        bottomZone.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        
        bottomZone.add(buildPaginationPanel(), BorderLayout.CENTER);
        
        wrapper.add(bottomZone, BorderLayout.SOUTH);
        return wrapper;
    }



    // ── STATUS BAR ──────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.BG_SECONDARY);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        statusLabel = new JLabel("TỔNG: 0 LIÊN HỆ");
        statusLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        statusLabel.setForeground(UIConstants.TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        editBtn = buildIconTextBtn("", "SỬA", UIConstants.TEXT_SECONDARY);
        editBtn.addActionListener(e -> { if (isTrashMode) restoreSelected(); else editSelected(); });

        deleteBtn = buildIconTextBtn("", "XÓA", UIConstants.DANGER);
        deleteBtn.addActionListener(e -> { if (isTrashMode) permanentlyDeleteSelected(); else deleteSelected(); });

        // Nút Import CSV
        JButton importBtn = new JButton("Import CSV");
        importBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        importBtn.setForeground(UIConstants.TEXT_PRIMARY);
        importBtn.setBackground(UIConstants.BG_HOVER);
        importBtn.setFocusPainted(false);
        importBtn.addActionListener(e -> openCsvFileChooser());

        // Nút Export CSV
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        exportBtn.setForeground(UIConstants.TEXT_PRIMARY);
        exportBtn.setBackground(UIConstants.BG_HOVER);
        exportBtn.setFocusPainted(false);
        exportBtn.addActionListener(e -> exportContacts());

        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(importBtn);
        actions.add(exportBtn);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    // ── BUTTON HELPERS ─────────────────────────────────────
    private JButton buildBtn(String text, Color bg, Color fg, boolean rounded) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(!rounded);
        if (!rounded) btn.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1, true));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private JButton buildIconTextBtn(String icon, String text, Color fg) {
        String label = icon == null || icon.isEmpty() ? text : (icon + " " + text);
        JButton btn = new JButton(label);
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(fg);
        btn.setBackground(UIConstants.BG_SECONDARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setOpaque(true); btn.setBackground(UIConstants.BG_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { btn.setOpaque(false); }
        });
        return btn;
    }

    // =========================================================
    //  PUBLIC API
    // =========================================================
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void loadContacts() {
        new SwingWorker<List<Contact>, Void>() {
            @Override protected List<Contact> doInBackground() throws Exception {
                return isTrashMode ? contactService.getDeletedContacts() : contactService.getAllContacts();
            }
            @Override protected void done() {
                try {
                    currentContacts = get();
                    currentPage = 1;
                    refreshTable();
                    updateStatus();
                } catch (Exception e) {
                    statusLabel.setText("Lỗi tải dữ liệu: " + e.getMessage());
                    JOptionPane.showMessageDialog(ContactPanel.this,
                            "Không thể tải danh bạ.\n" + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel buildPaginationPanel() {
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        paginationPanel.setOpaque(false);
        updatePaginationUI();
        return paginationPanel;
    }

    private void updatePaginationUI() {
        if (paginationPanel == null) return;
        paginationPanel.removeAll();
        
        int totalPages = (int) Math.ceil((double) currentContacts.size() / pageSize);
        if (totalPages <= 1) {
            paginationPanel.revalidate();
            paginationPanel.repaint();
            return;
        }

        JButton prevBtn = new JButton("<");
        prevBtn.setEnabled(currentPage > 1);
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) { currentPage--; refreshTable(); }
        });
        paginationPanel.add(stylePageButton(prevBtn, false));

        for (int i = 1; i <= totalPages; i++) {
            JButton pageBtn = new JButton(String.valueOf(i));
            boolean isCurrent = (i == currentPage);
            pageBtn.setEnabled(!isCurrent);
            final int page = i;
            pageBtn.addActionListener(e -> {
                currentPage = page;
                refreshTable();
            });
            paginationPanel.add(stylePageButton(pageBtn, isCurrent));
        }

        JButton nextBtn = new JButton(">");
        nextBtn.setEnabled(currentPage < totalPages);
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages) { currentPage++; refreshTable(); }
        });
        paginationPanel.add(stylePageButton(nextBtn, false));

        paginationPanel.revalidate();
        paginationPanel.repaint();
    }
    
    private JButton stylePageButton(JButton btn, boolean isCurrent) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(32, 26));
        if (isCurrent) {
            btn.setBackground(UIConstants.ACCENT);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createEmptyBorder());
        } else {
            btn.setBackground(UIConstants.BG_SECONDARY);
            btn.setForeground(UIConstants.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        }
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void setTrashMode(boolean trash) {
        this.isTrashMode = trash;
        if (trash) {
            titleLabel.setText("Thùng rác");
            addBtn.setVisible(false);
            groupFilter.setVisible(false);
            editBtn.setText("KHÔI PHỤC");
            deleteBtn.setText("XÓA VĨNH VIỄN");
        } else {
            titleLabel.setText("Danh bạ");
            addBtn.setVisible(true);
            groupFilter.setVisible(true);
            editBtn.setText("SỬA");
            deleteBtn.setText("XÓA");
        }
        loadContacts();
    }

    /**
     * Chỉ reset UI về chế độ bình thường (không load data).
     * Dùng khi sẽ gọi filterByGroupId() ngay sau đó.
     */
    public void resetToNormalMode() {
        this.isTrashMode = false;
        addBtn.setVisible(true);
        groupFilter.setVisible(true);
        editBtn.setText("SỬA");
        deleteBtn.setText("XÓA");
    }

    public void filterByGroupId(int groupId, String groupName) {
        titleLabel.setText("Nhóm: " + groupName);
        new SwingWorker<List<Contact>, Void>() {
            @Override protected List<Contact> doInBackground() throws Exception {
                return contactService.getContactsByGroupId(groupId);
            }
            @Override protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText("NHÓM: " + groupName.toUpperCase() + "  —  " + currentContacts.size() + " LIÊN HỆ");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void filterByRecent() {
        new SwingWorker<List<Contact>, Void>() {
            @Override protected List<Contact> doInBackground() throws Exception {
                return contactService.getRecentContacts(30);
            }
            @Override protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText("GẦN ĐÂY  —  " + currentContacts.size() + " LIÊN HỆ");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================
    //  PRIVATE ACTIONS
    // =========================================================
    private void performSearch() {
        String kw = searchField.getText().trim();
        if (kw.equals("Tìm kiếm theo tên, SĐT, email...") || kw.isEmpty()) { loadContacts(); return; }
        new SwingWorker<List<Contact>, Void>() {
            @Override protected List<Contact> doInBackground() throws Exception {
                return contactService.searchContacts(kw);
            }
            @Override protected void done() {
                try {
                    currentContacts = get();
                    refreshTable();
                    statusLabel.setText("KẾT QUẢ: " + currentContacts.size() + " LIÊN HỆ");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadGroupFilter() {
        new SwingWorker<List<GroupInfo>, Void>() {
            @Override protected List<GroupInfo> doInBackground() throws Exception {
                return contactService.getAllGroups();
            }
            @Override protected void done() {
                try {
                    allGroups = get();
                    if (groupFilter == null) return;
                    groupFilter.removeAllItems();
                    groupFilter.addItem("Tất cả");
                    for (GroupInfo g : allGroups)
                        groupFilter.addItem(g.getIcon() + " " + g.getDisplayName());
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterByGroup() {
        int idx = groupFilter.getSelectedIndex();
        if (idx <= 0) { loadContacts(); return; }
        if (idx - 1 < allGroups.size()) {
            GroupInfo g = allGroups.get(idx - 1);
            filterByGroupId(g.getId(), g.getDisplayName());
        }
    }

    private void showAddDialog() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        Frame frame = (parent instanceof Frame) ? (Frame) parent : null;
        ContactDialog dlg = new ContactDialog(frame, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    contactService.addContact(dlg.getContact(), dlg.getCompanyName()); return null;
                }
                @Override protected void done() {
                    try { get(); loadContacts(); if (onRefresh != null) onRefresh.run(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(ContactPanel.this,
                            "Lỗi thêm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            }.execute();
        }
    }

    private void editSelected() {
        int row = contactTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn liên hệ.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        Contact c = currentContacts.get(row);
        Window parent = SwingUtilities.getWindowAncestor(this);
        Frame frame = (parent instanceof Frame) ? (Frame) parent : null;
        ContactDialog dlg = new ContactDialog(frame, c);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    contactService.updateContact(dlg.getContact(), dlg.getCompanyName()); return null;
                }
                @Override protected void done() {
                    try { get(); loadContacts(); if (onRefresh != null) onRefresh.run(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(ContactPanel.this,
                            "Lỗi cập nhật: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            }.execute();
        }
    }

    private void shareSelected() {
        int row = contactTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn liên hệ.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
        Contact c = currentContacts.get(row);
        Window parent = SwingUtilities.getWindowAncestor(this);
        Frame frame = (parent instanceof Frame) ? (Frame) parent : null;
        QRCodeDialog dlg = new QRCodeDialog(frame, c);
        dlg.setVisible(true);
    }

    private void deleteSelected() {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact c = currentContacts.get(row);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa liên hệ \"" + c.getName() + "\"?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    contactService.deleteContact(c.getId()); return null;
                }
                @Override protected void done() {
                    try { get(); loadContacts(); if (onRefresh != null) onRefresh.run(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(ContactPanel.this,
                            "Lỗi xóa: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            }.execute();
        }
    }

    private void restoreSelected() {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact c = currentContacts.get(row);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                contactService.restoreContact(c.getId()); return null;
            }
            @Override protected void done() {
                try { get(); loadContacts(); if (onRefresh != null) onRefresh.run(); }
                catch (Exception e) { JOptionPane.showMessageDialog(ContactPanel.this,
                        "Lỗi khôi phục: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void permanentlyDeleteSelected() {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact c = currentContacts.get(row);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa VĨNH VIỄN \"" + c.getName() + "\"?\nHành động không thể hoàn tác.",
                "Cảnh báo", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    contactService.permanentlyDeleteContact(c.getId()); return null;
                }
                @Override protected void done() {
                    try { get(); loadContacts(); if (onRefresh != null) onRefresh.run(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(ContactPanel.this,
                            "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
                }
            }.execute();
        }
    }

    private void changeSelectedGroup(int groupId) {
        int row = contactTable.getSelectedRow();
        if (row < 0) return;
        Contact c = currentContacts.get(row);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                contactService.changeGroupById(c, groupId); return null;
            }
            @Override protected void done() { loadContacts(); if (onRefresh != null) onRefresh.run(); }
        }.execute();
    }

    // ── CSV IMPORT ──────────────────────────────────────────
    private void openCsvFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn file CSV");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            importCsvFile(fc.getSelectedFile());
    }

    private void importCsvFile(File file) {
        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() throws Exception {
                int count = 0;
                int skipped = 0;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), "UTF-8"))) {
                    String line;
                    boolean firstLine = true;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        if (firstLine) { firstLine = false;
                            if (line.toLowerCase().contains("name") || line.toLowerCase().contains("tên")) continue;
                        }
                        List<String> parts = parseCsvLine(line);
                        if (parts.isEmpty()) continue;

                        Contact c = new Contact();
                        c.setName(csvVal(parts, 0));
                        c.setPhone(csvVal(parts, 1));
                        c.setEmail(csvVal(parts, 2));
                        c.setAddress(csvVal(parts, 3));
                        c.setBirthday(convertCsvDate(csvVal(parts, 4)));
                        c.setNotes(csvVal(parts, 5));

                        String groupStr = csvVal(parts, 6);
                        c.setGroupId(mapGroupId(groupStr));

                        String companyName = csvVal(parts, 7);

                        if (!c.getName().isEmpty()) {
                            if (!c.getPhone().isEmpty()) {
                                Contact dup = contactService.findDuplicateByPhone(c.getPhone(), null);
                                if (dup != null) { skipped++; continue; }
                            }
                            contactService.addContact(c, companyName);
                            count++;
                        }
                    }
                }
                return new int[]{count, skipped};
            }
            @Override protected void done() {
                try {
                    int[] result = get();
                    int n = result[0];
                    int s = result[1];
                    loadContacts();
                    if (onRefresh != null) onRefresh.run();
                    String msg = "Đã nhập " + n + " liên hệ thành công!";
                    if (s > 0) msg += "\n⚠ Bỏ qua " + s + " liên hệ do trùng số điện thoại.";
                    JOptionPane.showMessageDialog(ContactPanel.this, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ContactPanel.this, "Lỗi nhập file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Export toàn bộ danh bạ hiện tại ra file CSV.
     */
    private void exportContacts() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn vị trí lưu CSV");
        fc.setSelectedFile(new File("contacts_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"))) {
                // Header
                pw.println("Tên,SĐT,Email,Địa chỉ,Ngày sinh,Ghi chú,Nhóm,Công ty");
                for (Contact c : currentContacts) {
                    String line = String.join(",",
                            escapeCsv(c.getName()),
                            escapeCsv(c.getPhone()),
                            escapeCsv(c.getEmail()),
                            escapeCsv(c.getAddress()),
                            escapeCsv(c.getBirthday()),
                            escapeCsv(c.getNotes()),
                            escapeCsv(getGroupForContact(c)),
                            escapeCsv(c.getCompanyName())
                    );
                    pw.println(line);
                }
                JOptionPane.showMessageDialog(this, "Export thành công tới " + out.getAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi export: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Escape CSV field nếu chứa dấu phẩy hoặc dấu ngoặc kép */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Parse 1 dòng CSV, hỗ trợ trường trong ngoặc kép chứa dấu phẩy. */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        fields.add(sb.toString().trim());
        return fields;
    }

    /** Đọc giá trị cột an toàn, trả "" nếu vượt index. */
    private String csvVal(List<String> parts, int idx) {
        if (idx >= parts.size()) return "";
        String v = parts.get(idx).trim();
        // Bỏ ngoặc kép bọc ngoài nếu có
        if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        return v;
    }

    /** Chuyển dd/MM/yyyy | yyyy-MM-dd sang yyyy-MM-dd (PostgreSQL format). */
    private String convertCsvDate(String date) {
        if (date == null || date.isBlank()) return "";
        date = date.trim();
        // Đã đúng format yyyy-MM-dd
        if (date.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) return date;
        // dd/MM/yyyy
        if (date.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] p = date.split("/");
            return p[2] + "-" + pad2(p[1]) + "-" + pad2(p[0]);
        }
        return date;
    }

    private String pad2(String s) { return s.length() == 1 ? "0" + s : s; }

    /** Map tên nhóm (FAVORITES, FAMILY, WORK, FRIENDS, OTHER) → group_id (1-5). */
    private int mapGroupId(String g) {
        if (g == null || g.isBlank()) return 5;
        g = g.trim().toUpperCase();
        switch (g) {
            case "FAVORITES": case "1": case "YÊU THÍCH":   return 1;
            case "FAMILY":    case "2": case "GIA ĐÌNH":    return 2;
            case "WORK":      case "3": case "CÔNG VIỆC":   return 3;
            case "FRIENDS":   case "4": case "BẠN BÈ":      return 4;
            default:                                         return 5;
        }
    }

    // ── CONTEXT MENU ────────────────────────────────────────
    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        if (isTrashMode) {
            JMenuItem restore = new JMenuItem("Khôi phục");
            restore.addActionListener(e -> restoreSelected());
            JMenuItem del = new JMenuItem("Xóa vĩnh viễn");
            del.addActionListener(e -> permanentlyDeleteSelected());
            menu.add(restore); menu.addSeparator(); menu.add(del);
        } else {
            JMenuItem edit = new JMenuItem("Sửa liên hệ");
            edit.addActionListener(e -> editSelected());
            JMenuItem share = new JMenuItem("Chia sẻ (Mã QR)");
            share.addActionListener(e -> shareSelected());
            JMenuItem del = new JMenuItem("Xóa liên hệ");
            del.addActionListener(e -> deleteSelected());
            JMenu groupMenu = new JMenu("Chuyển nhóm");
            for (GroupInfo g : allGroups) {
                JMenuItem gi = new JMenuItem(g.getDisplayName());
                gi.addActionListener(e -> changeSelectedGroup(g.getId()));
                groupMenu.add(gi);
            }
            menu.add(edit); menu.add(share); menu.add(groupMenu); menu.addSeparator(); menu.add(del);
        }
        return menu;
    }

    // ── TABLE REFRESH ────────────────────────────────────────
    private void refreshTable() {
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, currentContacts.size());
        for (int i = start; i < end; i++) {
            Contact c = currentContacts.get(i);
            tableModel.addRow(new Object[]{
                    i + 1,
                    c,                                                       // avatar
                    c,                                                       // name+subtitle
                    c.getPhone()  != null ? c.getPhone()  : "",
                    c.getEmail()  != null ? c.getEmail()  : "",
                    c.getCompanyName() != null ? c.getCompanyName() : "",
                    getGroupForContact(c),                    c.getCompletionPercent()
            });
        }
        updatePaginationUI();
    }

    private void updateStatus() {
        statusLabel.setText("TỔNG: " + currentContacts.size() + " LIÊN HỆ");
    }

    private String getGroupForContact(Contact c) {
        for (GroupInfo g : allGroups)
            if (g.getId() == c.getGroupId()) return g.getDisplayName();
        return c.getGroup().toString();
    }

    private Color getGroupColorForId(int id) {
        for (GroupInfo g : allGroups)
            if (g.getId() == id) return g.getColor();
        return UIConstants.TEXT_MUTED;
    }

    // =========================================================
    //  CELL RENDERERS
    // =========================================================

    private static class IndexRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(UIConstants.FONT_SMALL);
            setForeground(UIConstants.TEXT_MUTED);
            if (!sel) setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
            setBorder(BorderFactory.createEmptyBorder());
            return this;
        }
    }

    private static class AvatarRenderer extends DefaultTableCellRenderer {
        private final Map<String, ImageIcon> cache = new HashMap<>();
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setText(""); setIcon(null);
            setHorizontalAlignment(CENTER);
            if (!sel) setBackground(row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT);
            else setBackground(UIConstants.BG_HOVER);
            if (v instanceof Contact) {
                Contact c = (Contact) v;
                if (c.getAvatar() != null && !c.getAvatar().isBlank()) {
                    if (cache.containsKey(c.getId())) {
                        setIcon(cache.get(c.getId()));
                    } else {
                        try {
                            byte[] b = java.util.Base64.getDecoder().decode(c.getAvatar());
                            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(b));
                            if (img != null) {
                                ImageIcon ic = new ImageIcon(makeCircle(img, 36));
                                cache.put(c.getId(), ic);
                                setIcon(ic);
                            }
                        } catch (Exception ignored) { drawInitial(c); }
                    }
                } else { drawInitial(c); }
            }
            setBorder(BorderFactory.createEmptyBorder());
            return this;
        }
        private void drawInitial(Contact c) {
            String init = (c.getName() != null && !c.getName().isBlank())
                    ? String.valueOf(c.getName().trim().charAt(0)).toUpperCase() : "?";
            setText(init);
            setFont(UIConstants.FONT_SMALL_BOLD);
            setForeground(UIConstants.ACCENT);
        }
        private java.awt.image.BufferedImage makeCircle(java.awt.image.BufferedImage img, int size) {
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
            g2.setComposite(AlphaComposite.SrcAtop);
            g2.drawImage(img, 0, 0, size, size, null);
            g2.dispose();
            return out;
        }
    }

    private static class NameRenderer extends JPanel implements TableCellRenderer {
        private final JLabel nameLabel = new JLabel();
        private final JLabel subLabel  = new JLabel();
        NameRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            nameLabel.setFont(UIConstants.FONT_BODY_BOLD);
            nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
            subLabel.setFont(UIConstants.FONT_SMALL);
            subLabel.setForeground(UIConstants.TEXT_MUTED);
            add(nameLabel); add(subLabel);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            if (v instanceof Contact) {
                Contact c = (Contact) v;
                nameLabel.setText(c.getName() != null ? c.getName() : "");
                String sub = c.getCompanyName() != null && !c.getCompanyName().isBlank()
                        ? c.getCompanyName() : "";
                subLabel.setText(sub);
            }
            setBackground(sel ? UIConstants.BG_HOVER
                    : (row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT));
            return this;
        }
    }

    private class GroupBadgeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (row < currentContacts.size()) {
                Contact c = currentContacts.get(row);
                Color groupColor = getGroupColorForId(c.getGroupId());
                Color bgColor = UIConstants.withAlpha(groupColor, 30);
                String label = v instanceof String ? ((String) v).toUpperCase() : "";
                setText(label);
                setFont(new Font("Segoe UI", Font.BOLD, 10));
                setForeground(groupColor);
                setBackground(sel ? UIConstants.BG_HOVER : bgColor);
                setOpaque(true);
                setHorizontalAlignment(CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            }
            return this;
        }
    }

    private static class ProgressBarRenderer extends JPanel implements TableCellRenderer {
        private int pct;
        ProgressBarRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            pct = v instanceof Integer ? (Integer) v : 0;
            setBackground(sel ? UIConstants.BG_HOVER
                    : (row % 2 == 0 ? UIConstants.BG_SECONDARY : UIConstants.TABLE_ROW_ALT));
            return this;
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int barW = (int)(getWidth() * 0.60);
            int barH = 6;
            int x = 10, y = (getHeight() - barH) / 2;
            // track
            g2.setColor(UIConstants.BORDER);
            g2.fillRoundRect(x, y, barW, barH, barH, barH);
            // fill
            Color fill = pct >= 80 ? UIConstants.SUCCESS : pct >= 50 ? UIConstants.WARNING : UIConstants.DANGER;
            g2.setColor(fill);
            g2.fillRoundRect(x, y, (int)(barW * pct / 100.0), barH, barH, barH);
            // text
            g2.setFont(UIConstants.FONT_SMALL_BOLD);
            g2.setColor(UIConstants.TEXT_SECONDARY);
            String txt = pct + "%";
            g2.drawString(txt, x + barW + 6, y + barH);
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------------
    //  Drag-and-drop row reordering support
    // ---------------------------------------------------------------------
    private class TableRowTransferHandler extends TransferHandler {
        private DataFlavor localObjectFlavor;
        private final JTable table;
        private int[] rows = null;
        private int addIndex = -1; // Location where rows were added
        private int addCount = 0;  // Number of rows added.

        public TableRowTransferHandler(JTable table) {
            this.table = table;
            try {
                localObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.List");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            rows = table.getSelectedRows();
            List<Contact> transfered = new ArrayList<>();
            for (int row : rows) {
                transfered.add(currentContacts.get(row));
            }
            return new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{localObjectFlavor}; }
                @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return localObjectFlavor.equals(flavor); }
                @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (isDataFlavorSupported(flavor)) return transfered;
                    throw new UnsupportedFlavorException(flavor);
                }
            };
        }

        @Override
        public boolean canImport(TransferSupport info) {
            return info.isDrop() && (info.isDataFlavorSupported(localObjectFlavor) || info.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferSupport info) {
            if (!canImport(info)) return false;
            
            // Handle file drops
            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                try {
                    List<File> files = (List<File>) info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) importCsvFile(files.get(0));
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
            
            // Handle row reordering
            JTable target = (JTable) info.getComponent();
            JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
            int index = dl.getRow();
            int max = table.getModel().getRowCount();
            if (index < 0 || index > max) index = max;
            try {
                List<Contact> data = (List<Contact>) info.getTransferable().getTransferData(localObjectFlavor);
                addIndex = index;
                addCount = data.size();
                // Adjust currentContacts order
                for (int i = 0; i < data.size(); i++) {
                    currentContacts.add(index + i, data.get(i));
                }
                // Remove original rows (if moving down, adjust index)
                if (rows != null) {
                    // If we moved rows down, their original indices have shifted
                    if (addIndex > rows[0]) {
                        for (int i = rows.length - 1; i >= 0; i--) {
                            currentContacts.remove(rows[i]);
                        }
                    } else {
                        for (int i = 0; i < rows.length; i++) {
                            currentContacts.remove(rows[i] + addCount);
                        }
                    }
                }
                refreshTable();
                
                // Update table selections
                target.getSelectionModel().clearSelection();
                for (int i = 0; i < addCount; i++) {
                    target.getSelectionModel().addSelectionInterval(addIndex + i, addIndex + i);
                }

                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            rows = null;
            addIndex = -1;
            addCount = 0;
        }
    }
}
