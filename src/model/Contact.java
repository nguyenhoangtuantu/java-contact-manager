package model;

import java.util.Objects;
import java.util.UUID;

/**
 * Model đại diện cho một liên hệ trong danh bạ.
 * Tham chiếu đến contact_groups (group_id) và companies (company_id).
 */
public class Contact {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String birthday;       // yyyy-MM-dd
    private String avatar;         // Base64 image string
    private String notes;
    private int groupId;           // FK → contact_groups.id
    private String companyId;      // FK → companies.id (UUID)
    private String createdAt;
    private String lastModified;
    private boolean isDeleted;

    // Transient fields (không lưu DB, chỉ dùng hiển thị)
    private String contactGroupName;   // Tên nhóm: FAVORITES, FAMILY, ...
    private String companyName;        // Tên công ty

    public Contact() {
        this.id = UUID.randomUUID().toString();
        this.groupId = 5; // OTHER
    }

    public Contact(String name, String phone) {
        this();
        this.name = name;
        this.phone = phone;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    // Transient
    public String getContactGroupName() { return contactGroupName; }
    public void setContactGroupName(String contactGroupName) { this.contactGroupName = contactGroupName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    /**
     * Lấy ContactGroup enum từ groupId.
     */
    public ContactGroup getGroup() {
        // Map group_id (1-5) → enum
        switch (groupId) {
            case 1: return ContactGroup.FAVORITES;
            case 2: return ContactGroup.FAMILY;
            case 3: return ContactGroup.WORK;
            case 4: return ContactGroup.FRIENDS;
            default: return ContactGroup.OTHER;
        }
    }

    /**
     * Set group từ enum.
     */
    public void setGroup(ContactGroup group) {
        switch (group) {
            case FAVORITES: this.groupId = 1; break;
            case FAMILY:    this.groupId = 2; break;
            case WORK:      this.groupId = 3; break;
            case FRIENDS:   this.groupId = 4; break;
            default:        this.groupId = 5; break;
        }
    }

    // --- Utility ---

    /**
     * Tính số trường thông tin đã được điền.
     */
    public int getFilledFieldCount() {
        int count = 0;
        if (name != null && !name.isBlank()) count++;
        if (phone != null && !phone.isBlank()) count++;
        if (email != null && !email.isBlank()) count++;
        if (address != null && !address.isBlank()) count++;
        if (birthday != null && !birthday.isBlank()) count++;
        if (avatar != null && !avatar.isBlank()) count++;
        if (companyId != null && !companyId.isBlank()) count++;
        if (notes != null && !notes.isBlank()) count++;
        return count;
    }

    /**
     * Tổng số trường có thể điền (không tính id, timestamps, group).
     */
    public int getTotalFields() {
        return 8; // name, phone, email, address, birthday, avatar, company, notes
    }

    /**
     * Phần trăm hoàn thiện thông tin.
     */
    public int getCompletionPercent() {
        return (int) ((getFilledFieldCount() / (double) getTotalFields()) * 100);
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Objects.equals(id, contact.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + phone + ")";
    }
}
