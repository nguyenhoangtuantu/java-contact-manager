package config;

import java.io.*;
import java.util.Properties;

/**
 * Quản lý cấu hình kết nối Supabase.
 * Đọc từ file config.properties.
 * Phiên đăng nhập được lưu dưới dạng userId (UUID) thay vì JWT token.
 */
public class SupabaseConfig {
    private static final String CONFIG_FILE = "config.properties";
    private String supabaseUrl;
    private String supabaseKey;

    // Phiên đăng nhập người dùng
    private String currentUserId;    // UUID từ bảng users
    private String currentUserEmail;
    private String currentUserDisplayName;
    private String currentUserAvatar;
    private boolean currentUserDarkMode;
    private boolean currentUserCleanupReminder = true;
    private boolean currentUserBirthdayReminder = true;
    private String currentUserRole;

    // Biến tạm thời để Admin quản lý (impersonate) User
    private String targetUserId;
    private String targetUserEmail;
    private String targetUserDisplayName;

    private static SupabaseConfig instance;

    private SupabaseConfig() {
        loadConfig();
    }

    public static SupabaseConfig getInstance() {
        if (instance == null) {
            instance = new SupabaseConfig();
        }
        return instance;
    }

    private void loadConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                supabaseUrl = props.getProperty("SUPABASE_URL", "").trim();
                supabaseKey = props.getProperty("SUPABASE_API_KEY", "").trim();
            } catch (IOException e) {
                System.err.println("Lỗi đọc file config: " + e.getMessage());
            }
        }
    }

    public void saveConfig(String url, String key) {
        this.supabaseUrl = url;
        this.supabaseKey = key;
        Properties props = new Properties();
        props.setProperty("SUPABASE_URL", url);
        props.setProperty("SUPABASE_API_KEY", key);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Supabase Configuration - Quan ly Danh ba");
        } catch (IOException e) {
            System.err.println("Lỗi ghi file config: " + e.getMessage());
        }
    }

    public String getSupabaseUrl() { return supabaseUrl; }
    public String getSupabaseKey() { return supabaseKey; }

    /** Lưu phiên đăng nhập sau khi xác thực thành công. */
    public void setAuthSession(String userId, String email, String displayName, String avatar, boolean isDarkMode, boolean cleanupReminder, boolean birthdayReminder, String role) {
        this.currentUserId = userId;
        this.currentUserEmail = email;
        this.currentUserDisplayName = displayName;
        this.currentUserAvatar = avatar;
        this.currentUserDarkMode = isDarkMode;
        this.currentUserCleanupReminder = cleanupReminder;
        this.currentUserBirthdayReminder = birthdayReminder;
        this.currentUserRole = role;
    }

    public void setAuthSession(String userId, String email) {
        setAuthSession(userId, email, null, null, false, true, true, "user");
    }
    
    public void updateUserProfile(String displayName, String avatar) {
        this.currentUserDisplayName = displayName;
        this.currentUserAvatar = avatar;
    }

    public void updateUserTheme(boolean isDark) {
        this.currentUserDarkMode = isDark;
    }

    public void updateUserNotifications(boolean cleanupReminder, boolean birthdayReminder) {
        this.currentUserCleanupReminder = cleanupReminder;
        this.currentUserBirthdayReminder = birthdayReminder;
    }

    /** Xóa phiên đăng nhập (đăng xuất). */
    public void clearAuthSession() {
        this.currentUserId = null;
        this.currentUserEmail = null;
        this.currentUserDisplayName = null;
        this.currentUserAvatar = null;
        this.currentUserRole = null;
        clearTargetUser();
    }

    public String getCurrentUserId() { return targetUserId != null ? targetUserId : currentUserId; }
    public String getCurrentUserEmail() { return targetUserEmail != null ? targetUserEmail : currentUserEmail; }
    public String getCurrentUserDisplayName() { return targetUserDisplayName != null ? targetUserDisplayName : currentUserDisplayName; }
    public String getCurrentUserAvatar() { return currentUserAvatar; }

    public void setTargetUser(String uid, String email, String dName) {
        this.targetUserId = uid;
        this.targetUserEmail = email;
        this.targetUserDisplayName = dName;
    }

    public void clearTargetUser() {
        this.targetUserId = null;
        this.targetUserEmail = null;
        this.targetUserDisplayName = null;
    }

    public boolean isImpersonating() {
        return targetUserId != null;
    }
    public boolean isCurrentUserDarkMode() { return currentUserDarkMode; }
    public boolean isCurrentUserCleanupReminder() { return currentUserCleanupReminder; }
    public boolean isCurrentUserBirthdayReminder() { return currentUserBirthdayReminder; }
    public String getCurrentUserRole() { return currentUserRole; }
    public boolean isAdmin() { return "admin".equalsIgnoreCase(currentUserRole); }

    /** Kiểm tra đã đăng nhập chưa. */
    public boolean isLoggedIn() {
        return currentUserId != null && !currentUserId.isEmpty();
    }

    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty()
                && supabaseKey != null && !supabaseKey.isEmpty();
    }

    /** Trả về REST API base URL (PostgREST). */
    public String getRestUrl() {
        if (supabaseUrl == null) return "";
        String url = supabaseUrl.endsWith("/") ? supabaseUrl : supabaseUrl + "/";
        if (url.contains("supabase.co")) {
            return url + "rest/v1/";
        }
        return url; // PostgREST local không dùng /rest/v1/
    }
}
