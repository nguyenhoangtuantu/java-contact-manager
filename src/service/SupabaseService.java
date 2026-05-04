package service;

import com.google.gson.*;
import config.SupabaseConfig;
import model.Company;
import model.Contact;
import model.GroupInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/**
 * Service kết nối và thao tác CRUD với Supabase REST API (PostgREST).
 * Xác thực người dùng qua bảng users tự tạo (không dùng Supabase Auth).
 * Phân quyền dữ liệu bằng cách lọc user_id trực tiếp trong query.
 */
public class SupabaseService {
    private final HttpClient httpClient;
    private final SupabaseConfig config;

    private static SupabaseService instance;

    private SupabaseService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))  // #12: tránh hang vô hạn
                .build();
        this.config = SupabaseConfig.getInstance();
    }

    public static SupabaseService getInstance() {
        if (instance == null) {
            instance = new SupabaseService();
        }
        return instance;
    }

    // ==================== XÁC THỰC (AUTH) ====================

    /**
     * Đăng nhập: tìm user theo email, so sánh password hash ở client.
     */
    public boolean loginUser(String email, String password) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        // Lưu ý: Cần chạy file migration_user_avatar.sql trên Supabase trước để thêm cột avatar
        // Cần thêm cột is_deleted kiểu boolean (mặc định false) vào bảng users
        String url = config.getRestUrl() + "users?email=eq." + encodedEmail + "&select=*";

        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            System.err.println("[Login Error " + response.statusCode() + "]: " + response.body());
            throw new Exception("Lỗi kết nối cơ sở dữ liệu (Vui lòng đảm bảo đã chạy migration_user_avatar.sql)!");
        }

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        if (arr.isEmpty()) {
            throw new Exception("Email không tồn tại trong hệ thống!");
        }

        JsonObject user = arr.get(0).getAsJsonObject();

        boolean isDeleted = false;
        if (user.has("is_deleted") && !user.get("is_deleted").isJsonNull()) {
            isDeleted = user.get("is_deleted").getAsBoolean();
        }
        if (isDeleted) {
            throw new Exception("Tài khoản của bạn đã bị xóa!");
        }

        String storedHash = user.get("password").getAsString();
        String inputHash  = sha256(password);

        if (!MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                                   inputHash.getBytes(StandardCharsets.UTF_8))) {
            throw new Exception("Mật khẩu không chính xác!");
        }

        String dName = getStr(user, "display_name");
        String avatar = getStr(user, "avatar");
        boolean isDarkMode = false;
        if (user.has("is_dark_mode") && !user.get("is_dark_mode").isJsonNull()) {
            isDarkMode = user.get("is_dark_mode").getAsBoolean();
        }
        
        boolean isCleanup = true;
        if (user.has("is_cleanup_reminder") && !user.get("is_cleanup_reminder").isJsonNull()) {
            isCleanup = user.get("is_cleanup_reminder").getAsBoolean();
        }
        
        boolean isBirthday = true;
        if (user.has("is_birthday_reminder") && !user.get("is_birthday_reminder").isJsonNull()) {
            isBirthday = user.get("is_birthday_reminder").getAsBoolean();
        }

        String role = getStr(user, "role");
        if (role == null || role.isBlank()) role = "user";

        // Lưu phiên đăng nhập
        config.setAuthSession(user.get("id").getAsString(), email, dName, avatar, isDarkMode, isCleanup, isBirthday, role);
        return true;
    }

    /**
     * Đăng ký: kiểm tra email chưa tồn tại, rồi tạo user mới.
     */
    public boolean registerUser(String email, String password) throws Exception {
        return registerUser(email, password, null, null, null);
    }

    public boolean registerUser(String email, String password, String displayName,
                                String securityQuestion, String securityAnswer) throws Exception {
        // Kiểm tra email đã tồn tại chưa
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String checkUrl = config.getRestUrl() + "users?email=eq." + encodedEmail + "&select=id";
        HttpResponse<String> checkRes = httpClient.send(buildGetRequest(checkUrl),
                HttpResponse.BodyHandlers.ofString());
        if (checkRes.statusCode() < 400) {
            JsonArray existing = JsonParser.parseString(checkRes.body()).getAsJsonArray();
            if (!existing.isEmpty()) {
                throw new Exception("Email này đã được đăng ký. Vui lòng dùng email khác!");
            }
        }

        // Tạo user mới
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", sha256(password));
        if (displayName != null && !displayName.isBlank())
            body.addProperty("display_name", displayName);
        if (securityQuestion != null && !securityQuestion.isBlank())
            body.addProperty("security_question", securityQuestion);
        if (securityAnswer != null && !securityAnswer.isBlank())
            body.addProperty("security_answer", sha256(securityAnswer.trim().toLowerCase()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getRestUrl() + "users"))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            System.err.println("[Register Error " + response.statusCode() + "]: " + response.body());
            throw new Exception("Đăng ký thất bại: " + response.body());
        }

        // Đăng nhập tự động sau khi đăng ký
        return loginUser(email, password);
    }

    // ==================== QUÊN MẬT KHẨU ====================

    /**
     * Lấy câu hỏi bảo mật của user theo email.
     * @return câu hỏi bảo mật, hoặc null nếu email không tồn tại hoặc chưa thiết lập
     */
    public String getSecurityQuestion(String email) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String url = config.getRestUrl() + "users?email=eq." + encodedEmail + "&select=security_question";

        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Lỗi kết nối cơ sở dữ liệu!");
        }

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        if (arr.isEmpty()) {
            return null;
        }

        JsonObject user = arr.get(0).getAsJsonObject();
        return getStr(user, "security_question");
    }

    /**
     * Đặt lại mật khẩu bằng câu trả lời bảo mật.
     * So sánh SHA-256 hash của câu trả lời (lowercase).
     */
    public boolean resetPassword(String email, String securityAnswer, String newPassword) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String url = config.getRestUrl() + "users?email=eq." + encodedEmail + "&select=id,security_answer";

        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Lỗi kết nối cơ sở dữ liệu!");
        }

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        if (arr.isEmpty()) {
            throw new Exception("Email không tồn tại trong hệ thống!");
        }

        JsonObject user = arr.get(0).getAsJsonObject();
        String storedAnswerHash = getStr(user, "security_answer");
        String inputAnswerHash  = sha256(securityAnswer.trim().toLowerCase());

        if (storedAnswerHash == null || !MessageDigest.isEqual(
                storedAnswerHash.getBytes(StandardCharsets.UTF_8),
                inputAnswerHash.getBytes(StandardCharsets.UTF_8))) {
            throw new Exception("Câu trả lời bảo mật không chính xác!");
        }

        // Cập nhật mật khẩu mới
        String userId = user.get("id").getAsString();
        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("password", sha256(newPassword));

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật mật khẩu thất bại!");
        }

        return true;
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập bằng mật khẩu hiện tại.
     */
    public boolean changePassword(String currentPassword, String newPassword) throws Exception {
        String email = config.getCurrentUserEmail();
        if (email == null) throw new Exception("Không tìm thấy thông tin phiên đăng nhập!");

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String url = config.getRestUrl() + "users?email=eq." + encodedEmail + "&select=id,password";

        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Lỗi kết nối cơ sở dữ liệu!");
        }

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        if (arr.isEmpty()) throw new Exception("Tài khoản không tồn tại!");

        JsonObject user = arr.get(0).getAsJsonObject();
        String storedHash = getStr(user, "password");
        String inputHash  = sha256(currentPassword);

        if (storedHash == null || !MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                inputHash.getBytes(StandardCharsets.UTF_8))) {
            throw new Exception("Mật khẩu hiện tại không chính xác!");
        }

        String userId = user.get("id").getAsString();
        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("password", sha256(newPassword));

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật mật khẩu thất bại!");
        }

        return true;
    }

    /**
     * Cập nhật thông tin hồ sơ (tên hiển thị, avatar).
     */
    public boolean updateUserProfile(String displayName, String avatarUrl) throws Exception {
        String userId = config.getCurrentUserId();
        if (userId == null) throw new Exception("Không tìm thấy thông tin phiên đăng nhập!");

        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("display_name", displayName);
        if (avatarUrl != null) {
            body.addProperty("avatar", avatarUrl);
        } else {
            body.add("avatar", JsonNull.INSTANCE);
        }

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật hồ sơ thất bại: " + updateRes.body());
        }

        // Cập nhật session tại client
        config.updateUserProfile(displayName, avatarUrl);
        return true;
    }

    /**
     * Cập nhật giao diện (Dark/Light mode).
     */
    public boolean updateUserTheme(boolean isDarkMode) throws Exception {
        String userId = config.getCurrentUserId();
        if (userId == null) throw new Exception("Không tìm thấy thông tin phiên đăng nhập!");

        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("is_dark_mode", isDarkMode);

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật giao diện thất bại: " + updateRes.body());
        }

        config.updateUserTheme(isDarkMode);
        return true;
    }

    /**
     * Cập nhật thông báo (Cleanup / Birthday).
     */
    public boolean updateUserNotifications(boolean isCleanupReminder, boolean isBirthdayReminder) throws Exception {
        String userId = config.getCurrentUserId();
        if (userId == null) throw new Exception("Không tìm thấy thông tin phiên đăng nhập!");

        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("is_cleanup_reminder", isCleanupReminder);
        body.addProperty("is_birthday_reminder", isBirthdayReminder);

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật thông báo thất bại: " + updateRes.body());
        }

        config.updateUserNotifications(isCleanupReminder, isBirthdayReminder);
        return true;
    }

    // ==================== QUẢN LÝ NGƯỜI DÙNG (ADMIN ONLY) ====================

    public JsonArray getAllUsers(String keyword, boolean fetchDeleted) throws Exception {
        if (!config.isAdmin()) throw new Exception("Bạn không có quyền xem danh sách người dùng!");
        String url = config.getRestUrl() + "users?select=*&order=created_at.desc";
        if (keyword != null && !keyword.trim().isEmpty()) {
            String encoded = URLEncoder.encode("%" + keyword.trim() + "%", StandardCharsets.UTF_8);
            url += "&or=(display_name.ilike." + encoded + ",email.ilike." + encoded + ")";
        }
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new Exception("Lỗi lấy danh sách người dùng!");
        }
        
        JsonArray allUsers = JsonParser.parseString(response.body()).getAsJsonArray();
        JsonArray filtered = new JsonArray();
        for (JsonElement el : allUsers) {
            JsonObject u = el.getAsJsonObject();
            boolean isDeleted = false;
            if (u.has("is_deleted") && !u.get("is_deleted").isJsonNull()) {
                isDeleted = u.get("is_deleted").getAsBoolean();
            }
            if (isDeleted == fetchDeleted) {
                filtered.add(u);
            }
        }
        return filtered;
    }

    public void adminResetUserPassword(String userId, String newPassword) throws Exception {
        if (!config.isAdmin()) throw new Exception("Bạn không có quyền thực hiện thao tác này!");
        
        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("password", sha256(newPassword));

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Cập nhật mật khẩu thất bại!");
        }
    }

    public void deleteUserAccount(String userId) throws Exception {
        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("is_deleted", true);

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Xóa tài khoản thất bại! " + updateRes.body());
        }
    }

    public void adminRestoreUser(String userId) throws Exception {
        if (!config.isAdmin()) throw new Exception("Bạn không có quyền thực hiện thao tác này!");
        String updateUrl = config.getRestUrl() + "users?id=eq." + userId;
        JsonObject body = new JsonObject();
        body.addProperty("is_deleted", false);

        HttpRequest updateReq = HttpRequest.newBuilder()
                .uri(URI.create(updateUrl))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> updateRes = httpClient.send(updateReq, HttpResponse.BodyHandlers.ofString());
        if (updateRes.statusCode() >= 400) {
            throw new Exception("Khôi phục tài khoản thất bại! " + updateRes.body());
        }
    }

    // ==================== CONTACTS ====================

    /**
     * Lấy tất cả liên hệ của user hiện tại.
     */
    public List<Contact> getAllContacts() throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=id,name,phone,email,address,birthday,notes,group_id,company_id,created_at,last_modified,is_deleted,avatar,user_id,contact_groups(name,display_name,icon),companies(name)"
                + "&is_deleted=eq.false"
                + "&user_id=eq." + currentUserId()
                + "&order=name.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Lấy liên hệ theo nhóm.
     */
    public List<Contact> getContactsByGroup(int groupId) throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&group_id=eq." + groupId
                + "&is_deleted=eq.false"
                + "&user_id=eq." + currentUserId()
                + "&order=name.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Tìm kiếm liên hệ theo tên, SĐT hoặc email.
     */
    public List<Contact> searchContacts(String keyword) throws Exception {
        String encoded = URLEncoder.encode("%" + keyword + "%", StandardCharsets.UTF_8);
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&or=(name.ilike." + encoded + ",phone.ilike." + encoded + ",email.ilike." + encoded + ")"
                + "&is_deleted=eq.false"
                + "&user_id=eq." + currentUserId()
                + "&order=name.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Thêm liên hệ mới.
     */
    public Contact insertContact(Contact contact) throws Exception {
        String url = config.getRestUrl() + "contacts";
        String json = buildContactInsertJson(contact);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<Contact> result = parseContactListSimple(response.body());
        return result.isEmpty() ? contact : result.get(0);
    }

    /**
     * Cập nhật liên hệ.
     */
    public Contact updateContact(Contact contact) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + contact.getId()
                + "&user_id=eq." + currentUserId();
        String json = buildContactUpdateJson(contact);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<Contact> result = parseContactListSimple(response.body());
        return result.isEmpty() ? contact : result.get(0);
    }

    /**
     * Soft delete: Chuyển vào Thùng rác.
     */
    public void deleteContact(String id) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + id
                + "&user_id=eq." + currentUserId();
        String json = "{\"is_deleted\": true}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Khôi phục liên hệ từ Thùng rác.
     */
    public void restoreContact(String id) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + id
                + "&user_id=eq." + currentUserId();
        String json = "{\"is_deleted\": false}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Xóa vĩnh viễn liên hệ.
     */
    public void permanentlyDeleteContact(String id) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + id
                + "&user_id=eq." + currentUserId();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Lấy các liên hệ trong Thùng rác.
     */
    public List<Contact> getDeletedContacts() throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&is_deleted=eq.true"
                + "&user_id=eq." + currentUserId()
                + "&order=name.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Xóa nhiều liên hệ (soft delete).
     */
    public void deleteContacts(List<String> ids) throws Exception {
        for (String id : ids) deleteContact(id);
    }

    // ==================== CONTACT_GROUPS ====================

    /**
     * Lấy tất cả nhóm.
     */
    public List<GroupInfo> getAllGroups() throws Exception {
        String url = config.getRestUrl() + "contact_groups?select=*&order=id.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseGroupList(response.body());
    }

    /**
     * Thêm nhóm mới.
     */
    public GroupInfo insertGroup(GroupInfo group) throws Exception {
        String url = config.getRestUrl() + "contact_groups";
        JsonObject json = new JsonObject();
        json.addProperty("name", group.getName());
        json.addProperty("display_name", group.getDisplayName());
        if (group.getIcon() != null) json.addProperty("icon", group.getIcon());
        if (group.getColorHex() != null) json.addProperty("color_hex", group.getColorHex());
        if (group.getDescription() != null) json.addProperty("description", group.getDescription());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<GroupInfo> result = parseGroupList(response.body());
        return result.isEmpty() ? group : result.get(0);
    }

    public void deleteGroup(int groupId) throws Exception {
        String url = config.getRestUrl() + "contact_groups?id=eq." + groupId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ==================== COMPANIES ====================

    public List<Company> getAllCompanies() throws Exception {
        String url = config.getRestUrl() + "companies?select=*"
                + "&user_id=eq." + currentUserId()
                + "&order=name.asc";
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseCompanyList(response.body());
    }

    public Company insertCompany(Company company) throws Exception {
        String url = config.getRestUrl() + "companies";
        JsonObject json = new JsonObject();
        json.addProperty("name", company.getName());
        json.addProperty("user_id", currentUserId());
        if (company.getAddress() != null) json.addProperty("address", company.getAddress());
        if (company.getPhone() != null) json.addProperty("phone", company.getPhone());
        if (company.getWebsite() != null) json.addProperty("website", company.getWebsite());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<Company> result = parseCompanyList(response.body());
        return result.isEmpty() ? company : result.get(0);
    }

    public Company findOrCreateCompany(String companyName) throws Exception {
        if (companyName == null || companyName.isBlank()) return null;

        String encoded = URLEncoder.encode(companyName.trim(), StandardCharsets.UTF_8);
        String url = config.getRestUrl() + "companies?name=eq." + encoded
                + "&user_id=eq." + currentUserId();
        HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<Company> existing = parseCompanyList(response.body());
        if (!existing.isEmpty()) return existing.get(0);

        return insertCompany(new Company(companyName.trim()));
    }

    // ==================== TEST CONNECTION ====================

    public boolean testConnection() {
        try {
            String url = config.getRestUrl() + "contact_groups?select=id&limit=1";
            HttpResponse<String> response = httpClient.send(buildGetRequest(url), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Supabase: " + e.getMessage());
            return false;
        }
    }

    // ==================== PRIVATE HELPERS ====================

    /** ID của user đang đăng nhập. */
    private String currentUserId() {
        return config.getCurrentUserId();
    }

    /** Hash SHA-256 cho mật khẩu. */
    public static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private String buildContactInsertJson(Contact c) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", currentUserId());    // ← bắt buộc
        if (c.getName() != null) json.addProperty("name", c.getName());
        if (c.getPhone() != null) json.addProperty("phone", c.getPhone());
        if (c.getEmail() != null) json.addProperty("email", c.getEmail());
        if (c.getAddress() != null) json.addProperty("address", c.getAddress());
        if (c.getBirthday() != null && !c.getBirthday().isBlank())
            json.addProperty("birthday", c.getBirthday());
        if (c.getAvatar() != null && !c.getAvatar().isBlank())
            json.addProperty("avatar", c.getAvatar());
        if (c.getNotes() != null) json.addProperty("notes", c.getNotes());
        json.addProperty("group_id", c.getGroupId());
        if (c.getCompanyId() != null && !c.getCompanyId().isBlank())
            json.addProperty("company_id", c.getCompanyId());
        return json.toString();
    }

    private String buildContactUpdateJson(Contact c) {
        JsonObject json = new JsonObject();
        json.addProperty("name", c.getName());
        json.addProperty("phone", c.getPhone());
        json.addProperty("email", c.getEmail());
        json.addProperty("address", c.getAddress());
        if (c.getBirthday() != null && !c.getBirthday().isBlank()) {
            json.addProperty("birthday", c.getBirthday());
        } else {
            json.add("birthday", JsonNull.INSTANCE);
        }
        if (c.getAvatar() != null && !c.getAvatar().isBlank()) {
            json.addProperty("avatar", c.getAvatar());
        } else {
            json.add("avatar", JsonNull.INSTANCE);
        }
        json.addProperty("notes", c.getNotes());
        json.addProperty("group_id", c.getGroupId());
        if (c.getCompanyId() != null && !c.getCompanyId().isBlank()) {
            json.addProperty("company_id", c.getCompanyId());
        } else {
            json.add("company_id", JsonNull.INSTANCE);
        }
        // Tự động cập nhật last_modified (backup cho trigger DB)
        json.addProperty("last_modified", java.time.Instant.now().toString());
        return json.toString();
    }

    private List<Contact> parseContactList(String jsonBody) {
        List<Contact> contacts = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            Contact c = parseContactBase(obj);
            if (obj.has("contact_groups") && !obj.get("contact_groups").isJsonNull()) {
                c.setContactGroupName(getStr(obj.getAsJsonObject("contact_groups"), "display_name"));
            }
            if (obj.has("companies") && !obj.get("companies").isJsonNull()) {
                c.setCompanyName(getStr(obj.getAsJsonObject("companies"), "name"));
            }
            contacts.add(c);
        }
        return contacts;
    }

    private List<Contact> parseContactListSimple(String jsonBody) {
        List<Contact> contacts = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) contacts.add(parseContactBase(el.getAsJsonObject()));
        return contacts;
    }

    private Contact parseContactBase(JsonObject obj) {
        Contact c = new Contact();
        c.setId(getStr(obj, "id"));
        c.setName(getStr(obj, "name"));
        c.setPhone(getStr(obj, "phone"));
        c.setEmail(getStr(obj, "email"));
        c.setAddress(getStr(obj, "address"));
        c.setBirthday(getStr(obj, "birthday"));
        c.setAvatar(getStr(obj, "avatar"));
        c.setNotes(getStr(obj, "notes"));
        c.setGroupId(getInt(obj, "group_id", 5));
        c.setCompanyId(getStr(obj, "company_id"));
        c.setCreatedAt(getStr(obj, "created_at"));
        c.setLastModified(getStr(obj, "last_modified"));
        if (obj.has("is_deleted") && !obj.get("is_deleted").isJsonNull())
            c.setDeleted(obj.get("is_deleted").getAsBoolean());
        return c;
    }

    private List<Company> parseCompanyList(String jsonBody) {
        List<Company> companies = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            Company c = new Company();
            c.setId(getStr(obj, "id"));
            c.setName(getStr(obj, "name"));
            c.setAddress(getStr(obj, "address"));
            c.setPhone(getStr(obj, "phone"));
            c.setWebsite(getStr(obj, "website"));
            c.setCreatedAt(getStr(obj, "created_at"));
            companies.add(c);
        }
        return companies;
    }

    private List<GroupInfo> parseGroupList(String jsonBody) {
        List<GroupInfo> groups = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            GroupInfo g = new GroupInfo();
            g.setId(getInt(obj, "id", 0));
            g.setName(getStr(obj, "name"));
            g.setDisplayName(getStr(obj, "display_name"));
            g.setIcon(getStr(obj, "icon"));
            g.setColorHex(getStr(obj, "color_hex"));
            g.setDescription(getStr(obj, "description"));
            groups.add(g);
        }
        return groups;
    }

    private String getStr(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }

    private int getInt(JsonObject obj, String key, int defaultVal) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : defaultVal;
    }

    private void checkResponse(HttpResponse<String> response) throws Exception {
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new Exception("Supabase API Error [" + code + "]: " + response.body());
        }
    }
}
