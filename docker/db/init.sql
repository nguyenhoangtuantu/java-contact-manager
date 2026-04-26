-- Tạo role cho PostgREST
CREATE ROLE web_anon NOLOGIN;

GRANT USAGE ON SCHEMA public TO web_anon;

-- =====================================================
-- BẢNG 1: users (Quản lý tài khoản)
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    email        TEXT        NOT NULL UNIQUE,
    password     TEXT        NOT NULL,   -- SHA-256 hex hash
    display_name TEXT,                   -- Tên hiển thị (tùy chọn)
    created_at   TIMESTAMPTZ DEFAULT now(),
    avatar       TEXT,
    security_question TEXT,
    security_answer   TEXT,
    is_dark_mode      BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- =====================================================
-- BẢNG 2: contact_groups (Nhóm ưu tiên)
-- =====================================================
CREATE TABLE IF NOT EXISTS contact_groups (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,         
    display_name TEXT NOT NULL,        
    icon TEXT DEFAULT '📋',            
    color_hex TEXT DEFAULT '#9696A0',  
    description TEXT                   
);

INSERT INTO contact_groups (id, name, display_name, icon, color_hex, description) VALUES
    (1, 'important', 'Quan trọng', '⭐', '#F59E0B', 'Liên hệ yêu thích, quan trọng nhất'),
    (2, 'family', 'Gia đình', '🏠', '#10B981', 'Người thân trong gia đình'),
    (3, 'work', 'Công việc', '💼', '#3B82F6', 'Đồng nghiệp, đối tác công việc'),
    (4, 'friends', 'Bạn bè', '👥', '#8B5CF6', 'Bạn bè, người quen'),
    (5, 'other', 'Khác', '📋', '#6B7280', 'Liên hệ chưa phân loại')
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- BẢNG 3: companies (Công ty / Tổ chức)
-- =====================================================
CREATE TABLE IF NOT EXISTS companies (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,
    address TEXT,
    phone TEXT,
    website TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);
CREATE INDEX IF NOT EXISTS idx_companies_user ON companies(user_id);

-- =====================================================
-- BẢNG 4: contacts (Liên hệ)
-- =====================================================
CREATE TABLE IF NOT EXISTS contacts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,                         
    phone TEXT,                                 
    email TEXT,                                 
    address TEXT,                               
    birthday DATE,                              
    avatar TEXT,                                
    notes TEXT,                                 
    group_id INT REFERENCES contact_groups(id) ON DELETE SET NULL DEFAULT 5,
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    last_modified TIMESTAMPTZ DEFAULT now(),
    is_deleted BOOLEAN DEFAULT FALSE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_contacts_name ON contacts(name);
CREATE INDEX IF NOT EXISTS idx_contacts_phone ON contacts(phone);
CREATE INDEX IF NOT EXISTS idx_contacts_email ON contacts(email);
CREATE INDEX IF NOT EXISTS idx_contacts_group ON contacts(group_id);
CREATE INDEX IF NOT EXISTS idx_contacts_company ON contacts(company_id);
CREATE INDEX IF NOT EXISTS idx_contacts_is_deleted ON contacts(is_deleted);
CREATE INDEX IF NOT EXISTS idx_contacts_user ON contacts(user_id);

-- =====================================================
-- TRIGGER TỰ ĐỘNG
-- =====================================================
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_last_modified
    BEFORE UPDATE ON contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified();

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

CREATE TRIGGER trigger_update_deleted_at
    BEFORE UPDATE ON contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_deleted_at();

-- =====================================================
-- CẤP QUYỀN CHO PostgREST ANON ROLE
-- =====================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON users TO web_anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON contact_groups TO web_anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON companies TO web_anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON contacts TO web_anon;

-- Cấp quyền sequence
GRANT USAGE, SELECT ON SEQUENCE contact_groups_id_seq TO web_anon;
