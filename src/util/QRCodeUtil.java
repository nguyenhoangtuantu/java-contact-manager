package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import model.Contact;

import java.awt.image.BufferedImage;

public class QRCodeUtil {
    public static BufferedImage generateQRCode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
    
    public static String generateVCard(Contact contact) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\n");
        sb.append("VERSION:3.0\r\n");
        sb.append("FN:").append(contact.getName() != null ? contact.getName() : "").append("\r\n");
        if (contact.getPhone() != null && !contact.getPhone().isEmpty()) {
            sb.append("TEL:").append(contact.getPhone()).append("\r\n");
        }
        if (contact.getEmail() != null && !contact.getEmail().isEmpty()) {
            sb.append("EMAIL:").append(contact.getEmail()).append("\r\n");
        }
        if (contact.getCompanyName() != null && !contact.getCompanyName().isEmpty()) {
            sb.append("ORG:").append(contact.getCompanyName()).append("\r\n");
        }
        sb.append("END:VCARD\r\n");
        return sb.toString();
    }
}
