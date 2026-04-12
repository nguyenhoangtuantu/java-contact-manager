package ui;

import config.SupabaseConfig;
import service.SupabaseService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * LoginFrame — 2 screen riêng biệt:
 *   • Login  : LEFT=Form (light glass)   | RIGHT=Branding (light blobs)
 *   • Register: LEFT=Branding (dark grad) | RIGHT=Form (4 fields)
 * Chuyển đổi qua CardLayout.
 */
public class LoginFrame extends JFrame {

    // ── Màu Login screen (light) ──────────────────────────────────────────────
    private static final Color L_BG         = new Color(235, 237, 245);
    private static final Color L_CARD       = new Color(255, 255, 255, 218);
    private static final Color L_BLOB_PINK  = new Color(255,  90, 155);
    private static final Color L_BLOB_BLUE  = new Color( 70, 185, 255);
    private static final Color L_BLOB_PUR   = new Color(160,  90, 255);
    private static final Color L_BTN        = new Color( 15,  15,  35);
    private static final Color L_BTN_HOVER  = new Color( 40,  40,  70);
    private static final Color L_ACCENT     = new Color( 65, 125, 255);

    // ── Màu Register screen (dark left) ──────────────────────────────────────
    private static final Color R_BG         = new Color( 18,  20,  40);
    private static final Color R_BLOB_A     = new Color( 90,  60, 200);
    private static final Color R_BLOB_B     = new Color( 40, 160, 230);
    private static final Color R_CARD       = new Color( 28,  32,  58, 235);
    private static final Color R_BTN_FROM   = new Color( 80, 120, 255);
    private static final Color R_BTN_TO     = new Color(150,  80, 255);
    private static final Color R_ACCENT     = new Color(120, 160, 255);

    // ── Màu chung ─────────────────────────────────────────────────────────────
    private static final Color INP_BG       = new Color(255, 255, 255);
    private static final Color INP_BG_DARK  = new Color( 38,  44,  74);
    private static final Color INP_BD       = new Color(210, 215, 232);
    private static final Color INP_BD_DARK  = new Color( 60,  68, 110);
    private static final Color INP_FOCUS    = new Color( 70, 130, 255);
    private static final Color TXT_TITLE    = new Color( 15,  20,  40);
    private static final Color TXT_BODY     = new Color( 80,  88, 120);
    private static final Color TXT_MUTED    = new Color(140, 148, 175);
    private static final Color TXT_WHITE    = new Color(225, 228, 248);
    private static final Color TXT_WHITE_MUT= new Color(150, 158, 195);
    private static final Color SHADOW_L     = new Color(100, 115, 170,  45);
    private static final Color SHADOW_D     = new Color(  0,   0,  20,  80);

    // ── Widgets Login ─────────────────────────────────────────────────────────
    private JTextField     lEmailField;
    private JPasswordField lPassField;
    private JButton        lLoginBtn;
    private JLabel         lStatusLabel;

    // ── Widgets Register ──────────────────────────────────────────────────────
    private JTextField     rNameField;
    private JTextField     rEmailField;
    private JPasswordField rPassField;
    private JPasswordField rConfirmField;
    private JButton        rRegisterBtn;
    private JLabel         rStatusLabel;
    private JComboBox<String> rQuestionCombo;
    private JTextField     rAnswerField;

    // ── Widgets Forgot Password ──────────────────────────────────────────────
    private JTextField     fEmailField;
    private JButton        fCheckBtn;
    private JLabel         fQuestionLabel;
    private JTextField     fAnswerField;
    private JPasswordField fNewPassField;
    private JPasswordField fConfirmPassField;
    private JButton        fResetBtn;
    private JLabel         fStatusLabel;
    private JPanel         fStep2Panel;
    private String         fCurrentEmail;

    // ── Câu hỏi bảo mật ──────────────────────────────────────────────────────
    private static final String[] SECURITY_QUESTIONS = {
        "Tên trường tiểu học của bạn là gì?",
        "Tên thú cưng đầu tiên của bạn là gì?",
        "Món ăn yêu thích của bạn là gì?",
        "Tên người bạn thân nhất thời thơ ấu?",
        "Thành phố nơi bạn sinh ra?"
    };

    // ── Layout ────────────────────────────────────────────────────────────────
    private CardLayout    cardLayout;
    private JPanel        cardRoot;
    private BufferedImage bannerImage;

    public LoginFrame() {
        setTitle("Danh Bạ — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Tải ảnh banner
        try {
            bannerImage = ImageIO.read(new File("resources/login_banner.jpg"));
        } catch (Exception e) {
            System.err.println("Không tải được banner: " + e.getMessage());
            bannerImage = null;
        }

        cardLayout = new CardLayout();
        cardRoot   = new JPanel(cardLayout);
        cardRoot.setPreferredSize(new Dimension(960, 720));

        cardRoot.add(buildLoginScreen(),          "LOGIN");
        cardRoot.add(buildRegisterScreen(),       "REGISTER");
        cardRoot.add(buildForgotPasswordScreen(), "FORGOT");
        cardLayout.show(cardRoot, "LOGIN");

        add(cardRoot);
        pack();
        setLocationRelativeTo(null);
    }

    // =========================================================================
    //  SCREEN 1: LOGIN  [Form LEFT | Branding RIGHT]
    // =========================================================================
    private JPanel buildLoginScreen() {
        JPanel screen = new JPanel(new GridLayout(1, 2, 0, 0));
        screen.add(buildLoginFormPanel());
        screen.add(buildLoginBrandPanel());
        return screen;
    }

    // ── Login: panel trái — glassmorphism form ──────────────────────────────
    private JPanel buildLoginFormPanel() {
        JPanel bg = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(L_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                blob(g2, -50,  -60, 240, L_BLOB_PINK, 55);
                blob(g2, getWidth() - 60, getHeight() - 70, 210, L_BLOB_BLUE, 50);
                blob(g2, getWidth() / 2 - 40, getHeight() - 110, 170, L_BLOB_PUR, 35);
                g2.dispose();
            }
        };
        bg.setLayout(new GridBagLayout());

        JPanel card = glassCard(L_CARD, SHADOW_L, false);
        card.setPreferredSize(new Dimension(355, 430));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(38, 42, 32, 42));

        card.add(lbl("Đăng nhập", 25, Font.BOLD, TXT_TITLE));
        card.add(Box.createVerticalStrut(4));
        card.add(lbl("Nhập thông tin tài khoản của bạn", 13, Font.PLAIN, TXT_BODY));
        card.add(Box.createVerticalStrut(26));

        card.add(lbl("Email", 13, Font.BOLD, TXT_BODY));
        card.add(Box.createVerticalStrut(6));
        lEmailField = new JTextField();
        styleFieldLight(lEmailField);
        card.add(lEmailField);
        card.add(Box.createVerticalStrut(14));

        card.add(lbl("Mật khẩu", 13, Font.BOLD, TXT_BODY));
        card.add(Box.createVerticalStrut(6));
        lPassField = new JPasswordField();
        styleFieldLight(lPassField);
        lPassField.addActionListener(e -> doLogin());
        card.add(lPassField);
        card.add(Box.createVerticalStrut(10));

        // Forgot password row
        JPanel rowForgot = new JPanel(new BorderLayout());
        rowForgot.setOpaque(false); rowForgot.setAlignmentX(LEFT_ALIGNMENT);
        rowForgot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel forgotLbl = lbl("Quên mật khẩu?", 13, Font.BOLD, L_ACCENT);
        forgotLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchTo("FORGOT"); }
            @Override public void mouseEntered(MouseEvent e) { forgotLbl.setForeground(L_ACCENT.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { forgotLbl.setForeground(L_ACCENT); }
        });
        rowForgot.add(forgotLbl, BorderLayout.EAST);
        card.add(rowForgot);
        card.add(Box.createVerticalStrut(20));

        lLoginBtn = darkBtn("Đăng nhập", L_BTN, L_BTN_HOVER, Color.WHITE);
        lLoginBtn.addActionListener(e -> doLogin());
        card.add(lLoginBtn);
        card.add(Box.createVerticalStrut(10));

        lStatusLabel = statusLbl();
        card.add(lStatusLabel);
        card.add(Box.createVerticalGlue());

        // Switch to register
        card.add(switchRow("Chưa có tài khoản?", "Đăng ký ngay", () -> switchTo("REGISTER"), false));

        bg.add(card, new GridBagConstraints());
        return bg;
    }

    // ── Login: panel phải — ảnh banner + overlay sáng ───────────────────────
    private JPanel buildLoginBrandPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Vẽ ảnh banner kiểu cover fit
                drawBannerImage(g2, getWidth(), getHeight());
                // Overlay gradient tối nhẹ ở dưới để text rõ hơn
                GradientPaint ov = new GradientPaint(
                    0, getHeight() * 0.5f, new Color(0, 0, 0, 0),
                    0, getHeight(),        new Color(0, 0, 0, 160));
                g2.setPaint(ov);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Text thương hiệu ở dưới
                drawBrandBottom(g2, getWidth(), getHeight(), "Danh Bạ",
                    "Kết nối mọi người, mọi lúc");
                g2.dispose();
            }
        };
    }

    // =========================================================================
    //  SCREEN 2: REGISTER  [Branding LEFT | Form RIGHT]
    // =========================================================================
    private JPanel buildRegisterScreen() {
        JPanel screen = new JPanel(new GridLayout(1, 2, 0, 0));
        screen.add(buildRegisterBrandPanel());
        screen.add(buildRegisterFormPanel());
        return screen;
    }

    // ── Register: panel trái — ảnh banner + overlay tím tối ─────────────────
    private JPanel buildRegisterBrandPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // Vẽ ảnh banner
                drawBannerImage(g2, getWidth(), getHeight());
                // Overlay tím tối để phân biệt với login
                g2.setColor(new Color(30, 10, 70, 175));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Thêm blob màu để đẹp hơn
                blob(g2, -30, -30, 200, new Color(130, 80, 255), 40);
                blob(g2, getWidth() - 50, getHeight() - 50, 180, new Color(50, 150, 255), 35);
                // Text trắng ở dưới
                drawBrandBottom(g2, getWidth(), getHeight(), "Tạo tài khoản",
                    "Tham gia miễn phí ngay hôm nay");
                g2.dispose();
            }
        };
    }

    // ── Register: panel phải — dark form ────────────────────────────────────
    private JPanel buildRegisterFormPanel() {
        JPanel bg = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(R_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                blob(g2, getWidth() - 60, -30, 180, R_BLOB_B, 30);
                blob(g2, -30, getHeight() - 60, 160, R_BLOB_A, 28);
                g2.dispose();
            }
        };
        bg.setLayout(new GridBagLayout());

        JPanel card = glassCard(R_CARD, SHADOW_D, true);
        card.setPreferredSize(new Dimension(375, 660));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(34, 40, 30, 40));

        card.add(lbl("Tạo tài khoản " + UIConstants.ICON_SPARKLE, 23, Font.BOLD, TXT_WHITE));
        card.add(Box.createVerticalStrut(4));
        card.add(lbl("Điền thông tin để bắt đầu", 13, Font.PLAIN, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(22));

        // Tên hiển thị
        card.add(lbl("Tên hiển thị", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rNameField = new JTextField();
        styleFieldDark(rNameField);
        card.add(rNameField);
        card.add(Box.createVerticalStrut(12));

        // Email
        card.add(lbl("Email", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rEmailField = new JTextField();
        styleFieldDark(rEmailField);
        card.add(rEmailField);
        card.add(Box.createVerticalStrut(12));

        // Mật khẩu
        card.add(lbl("Mật khẩu", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rPassField = new JPasswordField();
        styleFieldDark(rPassField);
        card.add(rPassField);
        card.add(Box.createVerticalStrut(12));

        // Xác nhận mật khẩu
        card.add(lbl("Xác nhận mật khẩu", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rConfirmField = new JPasswordField();
        styleFieldDark(rConfirmField);
        card.add(rConfirmField);
        card.add(Box.createVerticalStrut(12));

        // Câu hỏi bảo mật
        card.add(lbl("Câu hỏi bảo mật", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rQuestionCombo = new JComboBox<>(SECURITY_QUESTIONS);
        rQuestionCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rQuestionCombo.setBackground(INP_BG_DARK);
        rQuestionCombo.setForeground(TXT_WHITE);
        rQuestionCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        rQuestionCombo.setAlignmentX(LEFT_ALIGNMENT);
        card.add(rQuestionCombo);
        card.add(Box.createVerticalStrut(12));

        // Câu trả lời bảo mật
        card.add(lbl("Câu trả lời", 13, Font.BOLD, TXT_WHITE_MUT));
        card.add(Box.createVerticalStrut(5));
        rAnswerField = new JTextField();
        styleFieldDark(rAnswerField);
        rAnswerField.addActionListener(e -> doRegister());
        card.add(rAnswerField);
        card.add(Box.createVerticalStrut(20));

        // Nút Đăng ký — gradient
        rRegisterBtn = gradientBtn("Tạo tài khoản", R_BTN_FROM, R_BTN_TO);
        rRegisterBtn.addActionListener(e -> doRegister());
        card.add(rRegisterBtn);
        card.add(Box.createVerticalStrut(10));

        rStatusLabel = statusLbl();
        rStatusLabel.setForeground(TXT_WHITE_MUT);
        card.add(rStatusLabel);
        card.add(Box.createVerticalGlue());

        // Switch về login
        card.add(switchRow("Đã có tài khoản?", "Đăng nhập ngay", () -> switchTo("LOGIN"), true));

        bg.add(card, new GridBagConstraints());
        return bg;
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================
    private void doLogin() {
        String email = lEmailField.getText().trim();
        String pass  = new String(lPassField.getPassword());
        if (email.isEmpty() || pass.isEmpty()) {
            lStatusLabel.setText("⚠  Vui lòng nhập Email và Mật khẩu.");
            lStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }
        if (!isValidEmail(email)) {
            lStatusLabel.setText("⚠  Địa chỉ email không hợp lệ.");
            lStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }
        lLoginBtn.setEnabled(false);
        lLoginBtn.setText("Đang đăng nhập...");
        lStatusLabel.setText("⏳  Đang kết nối..."); lStatusLabel.setForeground(TXT_MUTED);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SupabaseService.getInstance().loginUser(email, pass);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        lStatusLabel.setText("✓  Đăng nhập thành công!");
                        lStatusLabel.setForeground(new Color(50, 180, 100));
                        lPassField.setText(""); // #13: xóa password sau khi thành công
                        Timer t = new Timer(350, e -> { dispose(); new MainFrame().setVisible(true); });
                        t.setRepeats(false); t.start();
                    }
                } catch (Exception ex) {
                    String m = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lStatusLabel.setText("✗  " + m); lStatusLabel.setForeground(new Color(220, 60, 60));
                    lPassField.setText(""); // #13: xóa password khi sai
                } finally {
                    lLoginBtn.setEnabled(true); lLoginBtn.setText("Đăng nhập");
                }
            }
        }.execute();
    }

    private void doRegister() {
        String name    = rNameField.getText().trim();
        String email   = rEmailField.getText().trim();
        String pass    = new String(rPassField.getPassword());
        String confirm = new String(rConfirmField.getPassword());
        String securityQuestion = (String) rQuestionCombo.getSelectedItem();
        String securityAnswer   = rAnswerField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty() || securityAnswer.isEmpty()) {
            setRStatus("⚠  Vui lòng điền đầy đủ thông tin.", new Color(255, 170, 50));
            return;
        }
        if (!isValidEmail(email)) { // #11: validate email format
            setRStatus("⚠  Địa chỉ email không hợp lệ.", new Color(255, 170, 50));
            return;
        }
        if (!pass.equals(confirm)) {
            setRStatus("✗  Mật khẩu xác nhận không khớp!", new Color(220, 60, 60));
            rConfirmField.setText("");
            return;
        }
        if (pass.length() < 6) {
            setRStatus("✗  Mật khẩu phải từ 6 ký tự trở lên.", new Color(220, 60, 60));
            return;
        }

        rRegisterBtn.setEnabled(false);
        rRegisterBtn.setText("Đang tạo tài khoản...");
        setRStatus("⏳  Đang xử lý...", TXT_WHITE_MUT);

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SupabaseService.getInstance().registerUser(email, pass, name, securityQuestion, securityAnswer);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        setRStatus("✓  Tài khoản đã được tạo!", new Color(80, 210, 120));
                        rPassField.setText(""); rConfirmField.setText(""); // #13
                        Timer t = new Timer(350, e -> { dispose(); new MainFrame().setVisible(true); });
                        t.setRepeats(false); t.start();
                    }
                } catch (Exception ex) {
                    String m = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    setRStatus("✗  " + m, new Color(220, 60, 60));
                    rPassField.setText(""); rConfirmField.setText(""); // #13
                } finally {
                    rRegisterBtn.setEnabled(true); rRegisterBtn.setText("Tạo tài khoản");
                }
            }
        }.execute();
    }

    private void setRStatus(String msg, Color c) {
        rStatusLabel.setText(msg); rStatusLabel.setForeground(c);
    }

    private void switchTo(String card) {
        cardLayout.show(cardRoot, card);
        switch (card) {
            case "LOGIN":    setTitle(UIConstants.ICON_USER + " Đăng nhập"); break;
            case "REGISTER": setTitle(UIConstants.ICON_APP  + " Đăng ký"); break;
            case "FORGOT":   setTitle("Quên mật khẩu"); resetForgotForm(); break;
        }
    }

    /** Reset form quên mật khẩu về trạng thái ban đầu. */
    private void resetForgotForm() {
        if (fEmailField != null) { fEmailField.setText(""); fEmailField.setEditable(true); }
        if (fCheckBtn != null)   { fCheckBtn.setVisible(true); fCheckBtn.setEnabled(true); fCheckBtn.setText("Kiểm tra email"); }
        if (fStep2Panel != null) fStep2Panel.setVisible(false);
        if (fAnswerField != null) fAnswerField.setText("");
        if (fNewPassField != null) fNewPassField.setText("");
        if (fConfirmPassField != null) fConfirmPassField.setText("");
        if (fStatusLabel != null) { fStatusLabel.setText(" "); fStatusLabel.setForeground(TXT_MUTED); }
        fCurrentEmail = null;
    }

    // =========================================================================
    //  SCREEN 3: FORGOT PASSWORD  [Branding LEFT | Form RIGHT]
    // =========================================================================
    private JPanel buildForgotPasswordScreen() {
        JPanel screen = new JPanel(new GridLayout(1, 2, 0, 0));
        screen.add(buildForgotBrandPanel());
        screen.add(buildForgotFormPanel());
        return screen;
    }

    // ── Forgot: panel trái — ảnh banner + overlay xanh ──────────────────────
    private JPanel buildForgotBrandPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                drawBannerImage(g2, getWidth(), getHeight());
                g2.setColor(new Color(10, 25, 60, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                blob(g2, -20, getHeight() / 2, 200, new Color(60, 160, 255), 40);
                blob(g2, getWidth() - 40, getHeight() / 3, 170, new Color(255, 140, 60), 35);
                drawBrandBottom(g2, getWidth(), getHeight(), "Khôi phục mật khẩu",
                    "Trả lời câu hỏi bảo mật để đặt lại");
                g2.dispose();
            }
        };
    }

    // ── Forgot: panel phải — light form ─────────────────────────────────────
    private JPanel buildForgotFormPanel() {
        JPanel bg = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(L_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                blob(g2, getWidth() - 30, -40, 200, new Color(255, 160, 60), 40);
                blob(g2, -40, getHeight() - 50, 180, new Color(60, 130, 255), 35);
                g2.dispose();
            }
        };
        bg.setLayout(new GridBagLayout());

        JPanel card = glassCard(L_CARD, SHADOW_L, false);
        card.setPreferredSize(new Dimension(370, 560));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 38, 26, 38));

        card.add(lbl("Khôi phục mật khẩu", 23, Font.BOLD, TXT_TITLE));
        card.add(Box.createVerticalStrut(3));
        card.add(lbl("Nhập email để lấy câu hỏi bảo mật", 13, Font.PLAIN, TXT_BODY));
        card.add(Box.createVerticalStrut(20));

        // Email
        card.add(lbl("Email đã đăng ký", 13, Font.BOLD, TXT_BODY));
        card.add(Box.createVerticalStrut(5));
        fEmailField = new JTextField();
        styleFieldLight(fEmailField);
        card.add(fEmailField);
        card.add(Box.createVerticalStrut(10));

        // Nút Kiểm tra email
        fCheckBtn = darkBtn("Kiểm tra email", L_BTN, L_BTN_HOVER, Color.WHITE);
        fCheckBtn.addActionListener(e -> doCheckEmail());
        card.add(fCheckBtn);
        card.add(Box.createVerticalStrut(6));

        // ── Step 2: hiện sau khi kiểm tra email thành công ──
        fStep2Panel = new JPanel();
        fStep2Panel.setOpaque(false);
        fStep2Panel.setLayout(new BoxLayout(fStep2Panel, BoxLayout.Y_AXIS));
        fStep2Panel.setAlignmentX(LEFT_ALIGNMENT);

        fQuestionLabel = lbl(" ", 13, Font.BOLD, new Color(200, 120, 30));
        fStep2Panel.add(fQuestionLabel);
        fStep2Panel.add(Box.createVerticalStrut(8));

        fStep2Panel.add(lbl("Câu trả lời", 13, Font.BOLD, TXT_BODY));
        fStep2Panel.add(Box.createVerticalStrut(4));
        fAnswerField = new JTextField();
        styleFieldLight(fAnswerField);
        fStep2Panel.add(fAnswerField);
        fStep2Panel.add(Box.createVerticalStrut(8));

        fStep2Panel.add(lbl("Mật khẩu mới", 13, Font.BOLD, TXT_BODY));
        fStep2Panel.add(Box.createVerticalStrut(4));
        fNewPassField = new JPasswordField();
        styleFieldLight(fNewPassField);
        fStep2Panel.add(fNewPassField);
        fStep2Panel.add(Box.createVerticalStrut(8));

        fStep2Panel.add(lbl("Xác nhận mật khẩu mới", 13, Font.BOLD, TXT_BODY));
        fStep2Panel.add(Box.createVerticalStrut(4));
        fConfirmPassField = new JPasswordField();
        styleFieldLight(fConfirmPassField);
        fConfirmPassField.addActionListener(e -> doResetPassword());
        fStep2Panel.add(fConfirmPassField);
        fStep2Panel.add(Box.createVerticalStrut(12));

        fResetBtn = darkBtn("Đặt lại mật khẩu", new Color(200, 100, 20), new Color(230, 120, 30), Color.WHITE);
        fResetBtn.addActionListener(e -> doResetPassword());
        fStep2Panel.add(fResetBtn);

        fStep2Panel.setVisible(false);
        card.add(fStep2Panel);

        card.add(Box.createVerticalStrut(6));
        fStatusLabel = statusLbl();
        card.add(fStatusLabel);
        card.add(Box.createVerticalGlue());

        // Quay về đăng nhập
        card.add(switchRow("Nhớ mật khẩu rồi?", "Đăng nhập", () -> switchTo("LOGIN"), false));

        bg.add(card, new GridBagConstraints());
        return bg;
    }

    // ── ACTIONS: Quên mật khẩu ──────────────────────────────────────────────

    /** Bước 1: Kiểm tra email và lấy câu hỏi bảo mật. */
    private void doCheckEmail() {
        String email = fEmailField.getText().trim();
        if (email.isEmpty()) {
            fStatusLabel.setText("⚠  Vui lòng nhập email.");
            fStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }
        if (!isValidEmail(email)) {
            fStatusLabel.setText("⚠  Địa chỉ email không hợp lệ.");
            fStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }

        fCheckBtn.setEnabled(false);
        fCheckBtn.setText("Đang kiểm tra...");
        fStatusLabel.setText("⏳  Đang tìm tài khoản...");
        fStatusLabel.setForeground(TXT_MUTED);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return SupabaseService.getInstance().getSecurityQuestion(email);
            }
            @Override protected void done() {
                try {
                    String question = get();
                    if (question == null || question.isBlank()) {
                        fStatusLabel.setText("✗  Email không tồn tại hoặc chưa có câu hỏi bảo mật.");
                        fStatusLabel.setForeground(new Color(220, 60, 60));
                        fCheckBtn.setEnabled(true);
                        fCheckBtn.setText("Kiểm tra email");
                        return;
                    }
                    fCurrentEmail = email;
                    fQuestionLabel.setText("❓ " + question);
                    fCheckBtn.setVisible(false);
                    fEmailField.setEditable(false);
                    fStep2Panel.setVisible(true);
                    fStep2Panel.getParent().revalidate();
                    fStep2Panel.getParent().repaint();
                    fStatusLabel.setText("✓  Hãy trả lời câu hỏi bảo mật bên dưới.");
                    fStatusLabel.setForeground(new Color(50, 180, 100));
                } catch (Exception ex) {
                    String m = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    fStatusLabel.setText("✗  " + m);
                    fStatusLabel.setForeground(new Color(220, 60, 60));
                    fCheckBtn.setEnabled(true);
                    fCheckBtn.setText("Kiểm tra email");
                }
            }
        }.execute();
    }

    /** Bước 2: Xác minh câu trả lời và đặt lại mật khẩu. */
    private void doResetPassword() {
        String answer  = fAnswerField.getText().trim();
        String newPass = new String(fNewPassField.getPassword());
        String confirm = new String(fConfirmPassField.getPassword());

        if (answer.isEmpty()) {
            fStatusLabel.setText("⚠  Vui lòng nhập câu trả lời.");
            fStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }
        if (newPass.isEmpty() || confirm.isEmpty()) {
            fStatusLabel.setText("⚠  Vui lòng nhập mật khẩu mới.");
            fStatusLabel.setForeground(new Color(220, 120, 30));
            return;
        }
        if (!newPass.equals(confirm)) {
            fStatusLabel.setText("✗  Mật khẩu xác nhận không khớp!");
            fStatusLabel.setForeground(new Color(220, 60, 60));
            fConfirmPassField.setText("");
            return;
        }
        if (newPass.length() < 6) {
            fStatusLabel.setText("✗  Mật khẩu phải từ 6 ký tự trở lên.");
            fStatusLabel.setForeground(new Color(220, 60, 60));
            return;
        }

        fResetBtn.setEnabled(false);
        fResetBtn.setText("Đang xử lý...");
        fStatusLabel.setText("⏳  Đang đặt lại mật khẩu...");
        fStatusLabel.setForeground(TXT_MUTED);

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SupabaseService.getInstance().resetPassword(fCurrentEmail, answer, newPass);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        fStatusLabel.setText("✓  Mật khẩu đã được đặt lại thành công!");
                        fStatusLabel.setForeground(new Color(50, 180, 100));
                        Timer t = new Timer(1500, e -> switchTo("LOGIN"));
                        t.setRepeats(false); t.start();
                    }
                } catch (Exception ex) {
                    String m = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    fStatusLabel.setText("✗  " + m);
                    fStatusLabel.setForeground(new Color(220, 60, 60));
                    fAnswerField.setText("");
                } finally {
                    fResetBtn.setEnabled(true);
                    fResetBtn.setText("Đặt lại mật khẩu");
                }
            }
        }.execute();
    }

    // =========================================================================
    //  UI BUILDER HELPERS
    // =========================================================================

    /** Tạo glass card panel. */
    private JPanel glassCard(Color bg, Color shadow, boolean dark) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(shadow);
                g2.fill(new RoundRectangle2D.Float(4, 6, getWidth() - 5, getHeight() - 5, 22, 22));
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 5, getHeight() - 5, 22, 22));
                g2.setColor(dark ? new Color(255,255,255,18) : new Color(255,255,255,180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-6, getHeight()-6, 22, 22));
                g2.dispose();
            }
        };
    }

    /** Nút tối (login). */
    private JButton darkBtn(String text, Color base, Color hover, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? hover : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(fg); g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        styleBtnCommon(btn);
        return btn;
    }

    /** Nút gradient (register). */
    private JButton gradientBtn(String text, Color from, Color to) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color f = from, t2 = to;
                if (getModel().isRollover()) {
                    f = from.brighter(); t2 = to.brighter();
                }
                g2.setPaint(new GradientPaint(0, 0, f, getWidth(), 0, t2));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        styleBtnCommon(btn);
        return btn;
    }

    private void styleBtnCommon(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(LEFT_ALIGNMENT);
    }

    /** Input light (login). */
    private void styleFieldLight(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(INP_BG); f.setForeground(TXT_TITLE);
        f.setCaretColor(INP_FOCUS);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); f.setAlignmentX(LEFT_ALIGNMENT);
        f.setBorder(new CompoundBorder(new LineBorder(INP_BD, 1, true),
                BorderFactory.createEmptyBorder(8, 13, 8, 13)));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(INP_FOCUS, 2, true),
                        BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(INP_BD, 1, true),
                        BorderFactory.createEmptyBorder(8, 13, 8, 13)));
            }
        });
    }

    /** Input dark (register). */
    private void styleFieldDark(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(INP_BG_DARK); f.setForeground(TXT_WHITE);
        f.setCaretColor(R_ACCENT); f.setSelectionColor(new Color(80, 120, 255, 80));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); f.setAlignmentX(LEFT_ALIGNMENT);
        f.setBorder(new CompoundBorder(new LineBorder(INP_BD_DARK, 1, true),
                BorderFactory.createEmptyBorder(8, 13, 8, 13)));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(INP_FOCUS, 2, true),
                        BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(INP_BD_DARK, 1, true),
                        BorderFactory.createEmptyBorder(8, 13, 8, 13)));
            }
        });
    }

    /** Row chuyển Login↔Register. */
    private JPanel switchRow(String labelTxt, String linkTxt, Runnable action, boolean dark) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false); row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        Color cMuted = dark ? TXT_WHITE_MUT : TXT_MUTED;
        Color cAccent = dark ? R_ACCENT : L_ACCENT;
        row.add(lbl(labelTxt, 13, Font.PLAIN, cMuted));
        JLabel link = lbl(linkTxt, 13, Font.BOLD, cAccent);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) { link.setForeground(cAccent.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { link.setForeground(cAccent); }
        });
        row.add(link);
        return row;
    }

    // ── Drawing primitives ──────────────────────────────────────────────────
    /** Vẽ ảnh banner theo kiểu cover-fit (lấp đầy panel, không méo). */
    private void drawBannerImage(Graphics2D g2, int w, int h) {
        if (bannerImage == null) {
            // Fallback: gradient tím đẹp nếu không có ảnh
            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 20, 50), w, h, new Color(60, 30, 100));
            g2.setPaint(gp); g2.fillRect(0, 0, w, h);
            return;
        }
        int imgW = bannerImage.getWidth();
        int imgH = bannerImage.getHeight();
        // Cover: scale để lấp đầy, cắt phần dư
        float scaleX = (float) w / imgW;
        float scaleY = (float) h / imgH;
        float scale  = Math.max(scaleX, scaleY);
        int drawW = Math.round(imgW * scale);
        int drawH = Math.round(imgH * scale);
        int offX = (w - drawW) / 2;
        int offY = (h - drawH) / 2;
        g2.drawImage(bannerImage, offX, offY, drawW, drawH, null);
    }

    /** Vẽ text thương hiệu ở phần dưới panel (trên overlay). */
    private void drawBrandBottom(Graphics2D g2, int w, int h, String title, String subtitle) {
        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;
        // Shadow chữ
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(title, tx + 1, h - 52 + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(title, tx, h - 52);
        // Subtitle
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fm = g2.getFontMetrics();
        int sx = (w - fm.stringWidth(subtitle)) / 2;
        g2.setColor(new Color(220, 222, 240, 200));
        g2.drawString(subtitle, sx, h - 28);
    }

    private static void blob(Graphics2D g2, int cx, int cy, int r, Color base, int maxA) {
        for (int i = r; i > 0; i -= 16) {
            float ratio = 1f - (float) i / r;
            int a = (int) (maxA * ratio);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
            g2.fillOval(cx - i / 2, cy - i / 2, i, i);
        }
    }

    /** #11: Kiểm tra format email hợp lệ. */
    private static boolean isValidEmail(String email) {
        return email.matches("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$");
    }

    private static JLabel lbl(String t, int size, int style, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(c); l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel statusLbl() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TXT_MUTED); l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // =========================================================================
    //  Supabase config check (giữ nguyên)
    // =========================================================================
    public boolean checkSupabaseConfig() {
        SupabaseConfig cfg = SupabaseConfig.getInstance();
        if (!cfg.isConfigured()) return showConfigDialog(cfg);
        if (!SupabaseService.getInstance().testConnection()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "Không thể kết nối Supabase!\nBạn có muốn nhập lại thông tin kết nối?",
                    "Lỗi kết nối", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            return r == JOptionPane.YES_OPTION && showConfigDialog(cfg);
        }
        return true;
    }

    private boolean showConfigDialog(SupabaseConfig cfg) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setPreferredSize(new Dimension(450, 140));
        JTextField urlField = new JTextField(cfg.getSupabaseUrl());
        JTextField keyField = new JTextField(cfg.getSupabaseKey());
        panel.add(new JLabel("Supabase URL:")); panel.add(urlField);
        panel.add(new JLabel("Supabase API Key (anon):")); panel.add(keyField);
        int result = JOptionPane.showConfirmDialog(this, panel,
                UIConstants.ICON_SETTINGS + " Cấu hình Supabase", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String url = urlField.getText().trim(), key = keyField.getText().trim();
            if (url.isEmpty() || key.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            cfg.saveConfig(url, key);
            return true;
        }
        return false;
    }
}
