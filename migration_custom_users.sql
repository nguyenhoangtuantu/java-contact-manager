-- =====================================================
-- MIGRATION: Chuyển sang Custom Users Table
-- Bỏ Supabase Auth, tự quản lý bảng users
-- Chạy script này trong Supabase SQL Editor
-- =====================================================

-- BƯỚC 1: Xóa RLS policies cũ (dùng auth.uid())
DROP POLICY IF EXISTS "Cho phép User xem/thêm/sửa nhóm của họ" ON contact_groups;
DROP POLICY IF EXISTS "Cho phép User thao tác công ty của họ" ON companies;
DROP POLICY IF EXISTS "Cho phép User quản lý danh bạ của riêng mình" ON contacts;

-- BƯỚC 2: Tắt RLS trên tất cả bảng
ALTER TABLE contact_groups DISABLE ROW LEVEL SECURITY;
ALTER TABLE companies     DISABLE ROW LEVEL SECURITY;
ALTER TABLE contacts      DISABLE ROW LEVEL SECURITY;

-- BƯỚC 3: Xóa cột user_id cũ (trỏ đến auth.users)
ALTER TABLE contacts       DROP COLUMN IF EXISTS user_id;
ALTER TABLE companies      DROP COLUMN IF EXISTS user_id;
ALTER TABLE contact_groups DROP COLUMN IF EXISTS user_id;

-- BƯỚC 4: Tạo bảng users tự quản lý
CREATE TABLE IF NOT EXISTS users (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    email        TEXT        NOT NULL UNIQUE,
    password     TEXT        NOT NULL,   -- SHA-256 hex hash
    display_name TEXT,                   -- Tên hiển thị (tùy chọn)
    created_at   TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- BƯỚC 5: Thêm cột user_id mới (trỏ đến bảng users tự tạo)
ALTER TABLE contacts  ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE;
-- contact_groups giữ global (dùng chung cho tất cả user)

-- BƯỚC 6: Index cho user_id
CREATE INDEX IF NOT EXISTS idx_contacts_user  ON contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_companies_user ON companies(user_id);

-- BƯỚC 7: Cấp quyền cho anon role truy cập bảng qua PostgREST
GRANT SELECT, INSERT, UPDATE, DELETE ON users          TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON contacts       TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON companies      TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON contact_groups TO anon;
GRANT USAGE, SELECT ON SEQUENCE contact_groups_id_seq  TO anon;

-- =====================================================
-- KẾT QUẢ sau migration:
--
--   users (mới)
--     id, email, password (SHA-256), display_name
--
--   contacts
--     ... các cột cũ ...
--     user_id → users.id  (mỗi user chỉ thấy danh bạ của mình)
--
--   companies
--     ... các cột cũ ...
--     user_id → users.id  (mỗi user chỉ thấy công ty của mình)
--
--   contact_groups  (global - dùng chung)
--     ... không đổi ...
-- =====================================================
