-- ============================================================
-- MIGRATION: Fix contact_groups + thêm cascade + indexes
-- Chạy trong Supabase SQL Editor (Dashboard → SQL Editor → New Query)
-- ============================================================

-- 1. Thêm user_id vào contact_groups (cho phép NULL tạm thời cho data cũ)
ALTER TABLE contact_groups
ADD COLUMN IF NOT EXISTS user_id uuid REFERENCES users(id) ON DELETE CASCADE;

-- 2. Gán user_id cho các nhóm cũ chưa có user (lấy user đầu tiên)
UPDATE contact_groups
SET user_id = (SELECT id FROM users LIMIT 1)
WHERE user_id IS NULL;

-- 3. Thêm CASCADE rules cho contacts
ALTER TABLE contacts
DROP CONSTRAINT IF EXISTS contacts_company_id_fkey;

ALTER TABLE contacts
ADD CONSTRAINT contacts_company_id_fkey
FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL;

ALTER TABLE contacts
DROP CONSTRAINT IF EXISTS contacts_user_id_fkey;

ALTER TABLE contacts
ADD CONSTRAINT contacts_user_id_fkey
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE contacts
DROP CONSTRAINT IF EXISTS contacts_group_id_fkey;

ALTER TABLE contacts
ADD CONSTRAINT contacts_group_id_fkey
FOREIGN KEY (group_id) REFERENCES contact_groups(id) ON DELETE SET NULL;

-- 4. Thêm cột deleted_at
ALTER TABLE contacts
ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

-- 5. Tạo trigger tự động cập nhật last_modified
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_last_modified ON contacts;
CREATE TRIGGER trigger_update_last_modified
    BEFORE UPDATE ON contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified();

-- 6. Tạo trigger tự động set deleted_at khi soft delete
CREATE OR REPLACE FUNCTION update_deleted_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_deleted = true AND OLD.is_deleted = false THEN
        NEW.deleted_at = NOW();
    ELSIF NEW.is_deleted = false THEN
        NEW.deleted_at = NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_deleted_at ON contacts;
CREATE TRIGGER trigger_update_deleted_at
    BEFORE UPDATE ON contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_deleted_at();

-- 7. Tạo indexes cho performance
CREATE INDEX IF NOT EXISTS idx_contacts_user_id ON contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_contacts_group_id ON contacts(group_id);
CREATE INDEX IF NOT EXISTS idx_contacts_is_deleted ON contacts(is_deleted);
CREATE INDEX IF NOT EXISTS idx_contacts_name ON contacts(name);
CREATE INDEX IF NOT EXISTS idx_companies_user_id ON companies(user_id);
CREATE INDEX IF NOT EXISTS idx_contact_groups_user_id ON contact_groups(user_id);

-- 8. Enable RLS (Row Level Security)
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_groups ENABLE ROW LEVEL SECURITY;

-- Xong! Kiểm tra:
SELECT 'Migration hoàn tất!' AS status;
