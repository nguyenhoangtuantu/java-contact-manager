package config;

import java.io.*;
import java.util.Properties;

/**
 * Quản lý cấu hình kết nối Supabase.
 * Đọc từ file config.properties.
 */
public class SupabaseConfig {
    private static final String CONFIG_FILE = "config.properties";
    private String supabaseUrl;
    private String supabaseKey;

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

    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty()
                && supabaseKey != null && !supabaseKey.isEmpty();
    }

    /**
     * Trả về REST API base URL.
     */
    public String getRestUrl() {
        if (supabaseUrl == null) return "";
        String url = supabaseUrl.endsWith("/") ? supabaseUrl : supabaseUrl + "/";
        return url + "rest/v1/";
    }
}
