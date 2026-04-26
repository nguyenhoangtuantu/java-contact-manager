package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class AvatarUtil {
    
    public static String encodeImageToBase64(String imagePath) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.File(imagePath));
            if (originalImage == null) return null;
            
            // Cắt ảnh thành hình vuông (Crop center)
            int minSide = Math.min(originalImage.getWidth(), originalImage.getHeight());
            int x = (originalImage.getWidth() - minSide) / 2;
            int y = (originalImage.getHeight() - minSide) / 2;
            BufferedImage cropped = originalImage.getSubimage(x, y, minSide, minSide);

            // Resize ảnh xuống 128x128 để tiết kiệm dung lượng
            Image tmp = cropped.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static BufferedImage decodeBase64ToImage(String base64String) {
        if (base64String == null || !base64String.startsWith("data:image")) return null;
        try {
            String base64Image = base64String.split(",")[1];
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void drawAvatar(Graphics2D g2, int width, int height, String avatarStr, String fallbackText) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        BufferedImage img = decodeBase64ToImage(avatarStr);
        if (img != null) {
            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, width, height));
            g2.drawImage(img, 0, 0, width, height, null);
        } else {
            g2.setColor(ui.UIConstants.ACCENT);
            g2.fillOval(0, 0, width, height);
            g2.setColor(Color.WHITE);
            int fontSize = Math.max(12, width / 2 - 2);
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            
            String t = (avatarStr != null && avatarStr.length() <= 2 && !avatarStr.isBlank()) ? avatarStr : fallbackText;
            if (t != null && !t.isEmpty()) {
                g2.drawString(t, (width - fm.stringWidth(t)) / 2, (height + fm.getAscent() - fm.getDescent()) / 2);
            }
        }
    }
}
