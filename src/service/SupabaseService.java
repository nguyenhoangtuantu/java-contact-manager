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
import java.util.*;

/**
 * Service kết nối và thao tác CRUD với Supabase REST API (PostgREST).
 * Hỗ trợ 3 bảng: contacts, contact_groups, companies (3NF).
 */
public class SupabaseService {
    private final HttpClient httpClient;
    private final SupabaseConfig config;

    private static SupabaseService instance;

    private SupabaseService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.config = SupabaseConfig.getInstance();
    }

    public static SupabaseService getInstance() {
        if (instance == null) {
            instance = new SupabaseService();
        }
        return instance;
    }

    // ==================== CONTACTS ====================

    /**
     * Lấy tất cả liên hệ (JOIN contact_groups và companies).
     */
    public List<Contact> getAllContacts() throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&is_deleted=eq.false&order=name.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Lấy liên hệ theo nhóm ưu tiên (group_id).
     */
    public List<Contact> getContactsByGroup(int groupId) throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&group_id=eq." + groupId + "&is_deleted=eq.false&order=name.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
                + "&is_deleted=eq.false&order=name.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
        String url = config.getRestUrl() + "contacts?id=eq." + contact.getId();
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
        String url = config.getRestUrl() + "contacts?id=eq." + id;
        String json = "{\"is_deleted\": true}";

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
    }

    /**
     * Khôi phục liên hệ từ Thùng rác.
     */
    public void restoreContact(String id) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + id;
        String json = "{\"is_deleted\": false}";

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
    }

    /**
     * Xóa vĩnh viễn liên hệ theo ID.
     */
    public void permanentlyDeleteContact(String id) throws Exception {
        String url = config.getRestUrl() + "contacts?id=eq." + id;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
    }

    /**
     * Lấy các liên hệ trong Thùng rác.
     */
    public List<Contact> getDeletedContacts() throws Exception {
        String url = config.getRestUrl()
                + "contacts?select=*,contact_groups(name,display_name,icon),companies(name)"
                + "&is_deleted=eq.true&order=name.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseContactList(response.body());
    }

    /**
     * Xóa nhiều liên hệ theo danh sách ID (soft delete).
     */
    public void deleteContacts(List<String> ids) throws Exception {
        for (String id : ids) {
            deleteContact(id);
        }
    }

    // ==================== CONTACT_GROUPS ====================

    /**
     * Lấy tất cả nhóm ưu tiên.
     */
    public List<GroupInfo> getAllGroups() throws Exception {
        String url = config.getRestUrl() + "contact_groups?select=*&order=id.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseGroupList(response.body());
    }

    /**
     * Thêm nhóm ưu tiên mới.
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

    /**
     * Xóa nhóm ưu tiên theo ID.
     */
    public void deleteGroup(int groupId) throws Exception {
        String url = config.getRestUrl() + "contact_groups?id=eq." + groupId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.getSupabaseKey())
                .header("Authorization", "Bearer " + config.getSupabaseKey())
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
    }

    // ==================== COMPANIES ====================

    /**
     * Lấy tất cả công ty.
     */
    public List<Company> getAllCompanies() throws Exception {
        String url = config.getRestUrl() + "companies?select=*&order=name.asc";
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return parseCompanyList(response.body());
    }

    /**
     * Thêm công ty mới.
     */
    public Company insertCompany(Company company) throws Exception {
        String url = config.getRestUrl() + "companies";
        JsonObject json = new JsonObject();
        json.addProperty("name", company.getName());
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

    /**
     * Tìm công ty theo tên (hoặc tạo mới nếu chưa có).
     */
    public Company findOrCreateCompany(String companyName) throws Exception {
        if (companyName == null || companyName.isBlank()) return null;

        String encoded = URLEncoder.encode(companyName.trim(), StandardCharsets.UTF_8);
        String url = config.getRestUrl() + "companies?name=eq." + encoded;
        HttpRequest request = buildGetRequest(url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        List<Company> existing = parseCompanyList(response.body());

        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        // Tạo mới
        Company newCompany = new Company(companyName.trim());
        return insertCompany(newCompany);
    }

    // ==================== TEST CONNECTION ====================

    /**
     * Kiểm tra kết nối Supabase.
     */
    public boolean testConnection() {
        try {
            String url = config.getRestUrl() + "contact_groups?select=id&limit=1";
            HttpRequest request = buildGetRequest(url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Supabase: " + e.getMessage());
            return false;
        }
    }

    // ==================== PRIVATE HELPERS ====================

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
        return json.toString();
    }

    /**
     * Parse contact list với JOIN data (contact_groups, companies).
     */
    private List<Contact> parseContactList(String jsonBody) {
        List<Contact> contacts = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            Contact c = parseContactBase(obj);

            // Parse joined contact_groups
            if (obj.has("contact_groups") && !obj.get("contact_groups").isJsonNull()) {
                JsonObject groupObj = obj.getAsJsonObject("contact_groups");
                c.setContactGroupName(getStr(groupObj, "name"));
            }

            // Parse joined companies
            if (obj.has("companies") && !obj.get("companies").isJsonNull()) {
                JsonObject companyObj = obj.getAsJsonObject("companies");
                c.setCompanyName(getStr(companyObj, "name"));
            }

            contacts.add(c);
        }
        return contacts;
    }

    /**
     * Parse contact list đơn giản (không JOIN).
     */
    private List<Contact> parseContactListSimple(String jsonBody) {
        List<Contact> contacts = new ArrayList<>();
        JsonArray array = JsonParser.parseString(jsonBody).getAsJsonArray();
        for (JsonElement el : array) {
            contacts.add(parseContactBase(el.getAsJsonObject()));
        }
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
        if (obj.has("is_deleted") && !obj.get("is_deleted").isJsonNull()) {
            c.setDeleted(obj.get("is_deleted").getAsBoolean());
        }
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
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    private int getInt(JsonObject obj, String key, int defaultVal) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return defaultVal;
    }

    private void checkResponse(HttpResponse<String> response) throws Exception {
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new Exception("Supabase API Error [" + code + "]: " + response.body());
        }
    }
}
