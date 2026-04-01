package service;

import model.Company;
import model.Contact;
import model.ContactGroup;
import model.GroupInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic cho quản lý danh bạ.
 */
public class ContactService {
    private final SupabaseService supabase;
    private static ContactService instance;

    private ContactService() {
        this.supabase = SupabaseService.getInstance();
    }

    public static ContactService getInstance() {
        if (instance == null) {
            instance = new ContactService();
        }
        return instance;
    }

    // ==================== CRUD ====================

    public List<Contact> getAllContacts() throws Exception {
        return supabase.getAllContacts();
    }

    public List<Contact> searchContacts(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            return getAllContacts();
        }
        return supabase.searchContacts(keyword.trim());
    }

    /**
     * Thêm liên hệ. Nếu có tên công ty mới → tạo trong bảng companies trước.
     */
    public Contact addContact(Contact contact, String companyName) throws Exception {
        if (companyName != null && !companyName.isBlank()) {
            Company company = supabase.findOrCreateCompany(companyName);
            if (company != null) {
                contact.setCompanyId(company.getId());
            }
        }
        return supabase.insertContact(contact);
    }

    /**
     * Cập nhật liên hệ. Xử lý tên công ty → company_id.
     */
    public Contact updateContact(Contact contact, String companyName) throws Exception {
        if (companyName != null && !companyName.isBlank()) {
            Company company = supabase.findOrCreateCompany(companyName);
            if (company != null) {
                contact.setCompanyId(company.getId());
            }
        } else {
            contact.setCompanyId(null);
        }
        return supabase.updateContact(contact);
    }

    public void deleteContact(String id) throws Exception {
        supabase.deleteContact(id);
    }

    public List<Contact> getDeletedContacts() throws Exception {
        return supabase.getDeletedContacts();
    }

    public void restoreContact(String id) throws Exception {
        supabase.restoreContact(id);
    }

    public void permanentlyDeleteContact(String id) throws Exception {
        supabase.permanentlyDeleteContact(id);
    }

    // ==================== COMPANIES ====================

    public List<Company> getAllCompanies() throws Exception {
        return supabase.getAllCompanies();
    }

    // ==================== NHÓM ƯU TIÊN (DYNAMIC) ====================

    /**
     * Lấy tất cả nhóm ưu tiên từ DB.
     */
    public List<GroupInfo> getAllGroups() throws Exception {
        return supabase.getAllGroups();
    }

    /**
     * Thêm nhóm ưu tiên mới.
     */
    public GroupInfo addGroup(GroupInfo group) throws Exception {
        return supabase.insertGroup(group);
    }

    /**
     * Xóa nhóm ưu tiên.
     */
    public void deleteGroup(int groupId) throws Exception {
        supabase.deleteGroup(groupId);
    }

    /**
     * Lấy liên hệ theo group_id.
     */
    public List<Contact> getContactsByGroupId(int groupId) throws Exception {
        return supabase.getContactsByGroup(groupId);
    }

    /**
     * Lấy liên hệ theo nhóm ưu tiên (enum, backward compat).
     */
    public List<Contact> getContactsByGroup(ContactGroup group) throws Exception {
        int groupId = groupToId(group);
        return supabase.getContactsByGroup(groupId);
    }

    /**
     * Thay đổi nhóm ưu tiên của liên hệ bằng group_id.
     */
    public Contact changeGroupById(Contact contact, int groupId) throws Exception {
        contact.setGroupId(groupId);
        return supabase.updateContact(contact);
    }

    /**
     * Thay đổi nhóm ưu tiên của liên hệ.
     */
    public Contact changeGroup(Contact contact, ContactGroup newGroup) throws Exception {
        contact.setGroup(newGroup);
        return supabase.updateContact(contact);
    }

    /**
     * Đếm số liên hệ trong từng nhóm (dynamic).
     */
    public Map<Integer, Integer> getGroupCountsById() throws Exception {
        List<Contact> all = getAllContacts();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Contact c : all) {
            int gid = c.getGroupId();
            counts.put(gid, counts.getOrDefault(gid, 0) + 1);
        }
        return counts;
    }

    // ==================== HỢP NHẤT TRÙNG LẶP ====================

    /**
     * Tìm các cặp liên hệ trùng lặp tiềm năng.
     */
    public List<List<Contact>> findDuplicates() throws Exception {
        List<Contact> all = getAllContacts();
        List<List<Contact>> duplicateGroups = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        for (int i = 0; i < all.size(); i++) {
            Contact a = all.get(i);
            if (processed.contains(a.getId())) continue;

            List<Contact> group = new ArrayList<>();
            group.add(a);

            for (int j = i + 1; j < all.size(); j++) {
                Contact b = all.get(j);
                if (processed.contains(b.getId())) continue;
                if (a.isPotentialDuplicate(b)) {
                    group.add(b);
                    processed.add(b.getId());
                }
            }

            if (group.size() > 1) {
                duplicateGroups.add(group);
                processed.add(a.getId());
            }
        }
        return duplicateGroups;
    }

    /**
     * Hợp nhất danh sách liên hệ trùng lặp thành một.
     */
    public Contact mergeContacts(List<Contact> duplicates) throws Exception {
        if (duplicates == null || duplicates.isEmpty()) return null;

        Contact primary = duplicates.get(0);

        for (int i = 1; i < duplicates.size(); i++) {
            Contact other = duplicates.get(i);
            if (isBlank(primary.getPhone()) && !isBlank(other.getPhone()))
                primary.setPhone(other.getPhone());
            if (isBlank(primary.getEmail()) && !isBlank(other.getEmail()))
                primary.setEmail(other.getEmail());
            if (isBlank(primary.getAddress()) && !isBlank(other.getAddress()))
                primary.setAddress(other.getAddress());
            if (isBlank(primary.getBirthday()) && !isBlank(other.getBirthday()))
                primary.setBirthday(other.getBirthday());
            if (isBlank(primary.getCompanyId()) && !isBlank(other.getCompanyId()))
                primary.setCompanyId(other.getCompanyId());
            if (isBlank(primary.getNotes()) && !isBlank(other.getNotes()))
                primary.setNotes(other.getNotes());

            supabase.deleteContact(other.getId());
        }

        return supabase.updateContact(primary);
    }

    // ==================== DỌN DẸP ĐỊNH KỲ ====================

    public List<Contact> findIncompleteContacts() throws Exception {
        return getAllContacts().stream()
                .filter(c -> c.getFilledFieldCount() < 3)
                .collect(Collectors.toList());
    }

    public List<Contact> findStaleContacts(int days) throws Exception {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return getAllContacts().stream()
                .filter(c -> {
                    if (c.getLastModified() == null) return true;
                    try {
                        String modified = c.getLastModified();
                        LocalDateTime lm = LocalDateTime.parse(
                                modified.substring(0, Math.min(modified.length(), 19)),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        );
                        return lm.isBefore(threshold);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<Contact> findContactsWithoutPhone() throws Exception {
        return getAllContacts().stream()
                .filter(c -> isBlank(c.getPhone()))
                .collect(Collectors.toList());
    }

    public void deleteMultiple(List<Contact> contacts) throws Exception {
        List<String> ids = contacts.stream().map(Contact::getId).collect(Collectors.toList());
        supabase.deleteContacts(ids);
    }

    public void permanentlyDeleteMultiple(List<Contact> contacts) throws Exception {
        for (Contact c : contacts) {
            supabase.permanentlyDeleteContact(c.getId());
        }
    }

    public Map<String, Object> getStatistics() throws Exception {
        List<Contact> all = getAllContacts();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("incomplete", all.stream().filter(c -> c.getFilledFieldCount() < 3).count());
        stats.put("noPhone", all.stream().filter(c -> isBlank(c.getPhone())).count());
        double avgCompletion = all.stream().mapToInt(Contact::getCompletionPercent).average().orElse(0);
        stats.put("avgCompletion", (int) avgCompletion);
        return stats;
    }

    // ==================== BIRTHDAYS ====================

    /**
     * Lấy danh sách những người có sinh nhật trong vòng 'days' ngày tới.
     */
    public List<Contact> getUpcomingBirthdays(int days) throws Exception {
        List<Contact> all = getAllContacts();
        java.time.LocalDate today = java.time.LocalDate.now();

        return all.stream()
                .filter(c -> c.getBirthday() != null && !c.getBirthday().isBlank())
                .filter(c -> {
                    try {
                        java.time.LocalDate bday = java.time.LocalDate.parse(c.getBirthday());
                        java.time.LocalDate nextBday = bday.withYear(today.getYear());
                        
                        // Nếu sinh nhật năm nay đã qua, xét sinh nhật năm sau
                        if (nextBday.isBefore(today)) {
                            nextBday = nextBday.plusYears(1);
                        }
                        
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, nextBday);
                        return daysBetween >= 0 && daysBetween <= days;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted((c1, c2) -> {
                    try {
                        java.time.LocalDate bd1 = java.time.LocalDate.parse(c1.getBirthday()).withYear(today.getYear());
                        if (bd1.isBefore(today)) bd1 = bd1.plusYears(1);
                        java.time.LocalDate bd2 = java.time.LocalDate.parse(c2.getBirthday()).withYear(today.getYear());
                        if (bd2.isBefore(today)) bd2 = bd2.plusYears(1);
                        return bd1.compareTo(bd2);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

    // ==================== HELPERS ====================

    private int groupToId(ContactGroup group) {
        switch (group) {
            case FAVORITES: return 1;
            case FAMILY: return 2;
            case WORK: return 3;
            case FRIENDS: return 4;
            default: return 5;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
