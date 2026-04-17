package ui;

import util.QRCodeUtil;
import model.Contact;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class QRCodeDialog extends JDialog {
    public QRCodeDialog(Frame parent, Contact contact) {
        super(parent, "Chia sẻ mã QR", true);
        
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIConstants.BG_PRIMARY);
        
        JLabel title = new JLabel("Quét mã QR để lưu liên hệ", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        add(title, BorderLayout.NORTH);
        
        try {
            String vcard = QRCodeUtil.generateVCard(contact);
            BufferedImage qrImage = QRCodeUtil.generateQRCode(vcard, 250, 250);
            JLabel qrLabel = new JLabel(new ImageIcon(qrImage));
            qrLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            add(qrLabel, BorderLayout.CENTER);
        } catch (Exception e) {
            add(new JLabel("Lỗi tạo mã QR", SwingConstants.CENTER), BorderLayout.CENTER);
        }
        
        JLabel subTitle = new JLabel(contact.getName(), SwingConstants.CENTER);
        subTitle.setFont(UIConstants.FONT_BODY_BOLD);
        subTitle.setForeground(UIConstants.TEXT_SECONDARY);
        subTitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JButton closeBtn = new JButton("ĐÓNG");
        closeBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(UIConstants.ACCENT);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UIConstants.BG_PRIMARY);
        bottom.add(subTitle, BorderLayout.NORTH);
        
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(UIConstants.BG_PRIMARY);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 10));
        btnPanel.add(closeBtn);
        bottom.add(btnPanel, BorderLayout.CENTER);
        
        add(bottom, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(parent);
    }
}
