package model;

/**
 * Model đại diện cho một công ty / tổ chức.
 * Tương ứng bảng companies trong Supabase.
 */
public class Company {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String website;
    private String createdAt;

    public Company() {}

    public Company(String name) {
        this.name = name;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name != null ? name : "(Không có)";
    }
}
