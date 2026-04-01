-- =====================================================
-- SUPABASE SCHEMA: Quản lý Danh bạ (3NF)
-- Chạy script này trong Supabase SQL Editor
-- 3 bảng: contact_groups, companies, contacts
-- =====================================================

-- =====================================================
-- BẢNG 1: contact_groups (Nhóm ưu tiên)
-- Lưu trữ các loại nhóm phân loại ưu tiên
-- =====================================================
CREATE TABLE IF NOT EXISTS contact_groups (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,         -- Tên nhóm: FAVORITES, FAMILY, WORK, FRIENDS, OTHER
    display_name TEXT NOT NULL,        -- Tên hiển thị: Yêu thích, Gia đình, ...
    icon TEXT DEFAULT '📋',            -- Icon emoji
    color_hex TEXT DEFAULT '#9696A0',  -- Mã màu hex
    description TEXT                   -- Mô tả nhóm
);

-- Dữ liệu mặc định cho các nhóm
INSERT INTO contact_groups (name, display_name, icon, color_hex, description) VALUES
    ('FAVORITES', 'Yêu thích', '⭐', '#FF4B4B', 'Liên hệ yêu thích, quan trọng nhất'),
    ('FAMILY', 'Gia đình', '🏠', '#4BC86E', 'Người thân trong gia đình'),
    ('WORK', 'Công việc', '💼', '#4A9EFF', 'Đồng nghiệp, đối tác công việc'),
    ('FRIENDS', 'Bạn bè', '👥', '#FFA532', 'Bạn bè, người quen'),
    ('OTHER', 'Khác', '📋', '#9696A0', 'Liên hệ chưa phân loại')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- BẢNG 2: companies (Công ty / Tổ chức)
-- Lưu trữ thông tin công ty, tránh trùng lặp dữ liệu
-- =====================================================
CREATE TABLE IF NOT EXISTS companies (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,                -- Tên công ty
    address TEXT,                      -- Địa chỉ công ty
    phone TEXT,                        -- SĐT công ty
    website TEXT,                      -- Website
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Index tìm kiếm theo tên công ty
CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);

-- Dữ liệu mẫu
INSERT INTO companies (name, address, phone, website) VALUES
    ('FPT Software', 'Khu CNC Hòa Lạc, Hà Nội', '024-7300-7300', 'https://fpt-software.com'),
    ('Vietcombank', '198 Trần Quang Khải, Q1, TP.HCM', '1900-545-413', 'https://vietcombank.com.vn'),
    ('Google', 'Mountain View, CA', NULL, 'https://google.com'),
    ('Shopee', 'Tòa nhà Flemington, Q11, TP.HCM', '1900-1221', 'https://shopee.vn');

-- =====================================================
-- BẢNG 3: contacts (Liên hệ)
-- Bảng chính, tham chiếu FK đến contact_groups và companies
-- =====================================================
CREATE TABLE IF NOT EXISTS contacts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,                         -- Họ và tên
    phone TEXT,                                 -- Số điện thoại
    email TEXT,                                 -- Email
    address TEXT,                               -- Địa chỉ cá nhân
    birthday DATE,                              -- Ngày sinh
    avatar TEXT,                                -- Ảnh đại diện (Dạng chuỗi Base64)
    notes TEXT,                                 -- Ghi chú
    group_id INT REFERENCES contact_groups(id)  -- FK → contact_groups
        DEFAULT 5,                              -- Mặc định: OTHER (id=5)
    company_id UUID REFERENCES companies(id)    -- FK → companies
        ON DELETE SET NULL,                     -- Nếu xóa công ty → set NULL
    created_at TIMESTAMPTZ DEFAULT now(),
    last_modified TIMESTAMPTZ DEFAULT now(),
    is_deleted BOOLEAN DEFAULT FALSE            -- Soft delete (Thùng rác)
);

-- Indexes cho tìm kiếm và phát hiện trùng lặp
CREATE INDEX IF NOT EXISTS idx_contacts_name ON contacts(name);
CREATE INDEX IF NOT EXISTS idx_contacts_phone ON contacts(phone);
CREATE INDEX IF NOT EXISTS idx_contacts_email ON contacts(email);
CREATE INDEX IF NOT EXISTS idx_contacts_group ON contacts(group_id);
CREATE INDEX IF NOT EXISTS idx_contacts_company ON contacts(company_id);
CREATE INDEX IF NOT EXISTS idx_contacts_is_deleted ON contacts(is_deleted);

-- Trigger tự động cập nhật last_modified khi UPDATE
CREATE OR REPLACE FUNCTION update_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS contacts_updated ON contacts;
CREATE TRIGGER contacts_updated
    BEFORE UPDATE ON contacts
    FOR EACH ROW EXECUTE FUNCTION update_last_modified();

-- =====================================================
-- DỮ LIỆU MẪU
-- =====================================================
-- Lấy company_id để insert
DO $$
DECLARE
    fpt_id UUID;
    vcb_id UUID;
    google_id UUID;
    shopee_id UUID;
BEGIN
    SELECT id INTO fpt_id FROM companies WHERE name = 'FPT Software' LIMIT 1;
    SELECT id INTO vcb_id FROM companies WHERE name = 'Vietcombank' LIMIT 1;
    SELECT id INTO google_id FROM companies WHERE name = 'Google' LIMIT 1;
    SELECT id INTO shopee_id FROM companies WHERE name = 'Shopee' LIMIT 1;

    INSERT INTO contacts (name, phone, email, address, birthday, notes, group_id, company_id) VALUES
        ('Nguyễn Văn An', '0901234567', 'an.nguyen@gmail.com', '123 Lê Lợi, Q1, TP.HCM', '1995-03-15', 'Bạn đại học', 4, fpt_id),
        ('Trần Thị Bình', '0912345678', 'binh.tran@outlook.com', '456 Nguyễn Huệ, Q1, TP.HCM', '1990-07-22', 'Đồng nghiệp cũ', 3, vcb_id),
        ('Lê Hoàng Cường', '0923456789', 'cuong.le@company.com', NULL, NULL, NULL, 3, google_id),
        ('Phạm Minh Đức', '0934567890', NULL, '789 Hai Bà Trưng, Q3', '1998-11-01', NULL, 2, NULL),
        ('Nguyễn Thị Em', '0945678901', 'em.nguyen@yahoo.com', NULL, '2000-01-30', 'Gặp ở hội thảo', 4, shopee_id),
        ('Võ Văn Phúc', '0901234567', 'phuc.vo@gmail.com', '321 CMT8, Q10', NULL, NULL, 3, fpt_id),
        ('Nguyễn Văn An', '0967890123', NULL, NULL, NULL, 'Trùng tên?', 5, NULL),
        ('Mẹ', '0978901234', NULL, 'Nhà, Đà Nẵng', '1965-05-10', NULL, 1, NULL),
        ('Ba', '0989012345', NULL, 'Nhà, Đà Nẵng', '1962-08-20', NULL, 1, NULL);
END $$;

-- =====================================================
-- QUAN HỆ GIỮA CÁC BẢNG (3NF):
--
--   contact_groups (1) ──── (N) contacts
--        (1 nhóm có nhiều liên hệ)
--
--   companies (1) ──── (N) contacts
--        (1 công ty có nhiều liên hệ)
--
-- Đảm bảo:
--   1NF: Mỗi cột chỉ chứa giá trị nguyên tử
--   2NF: Không có phụ thuộc bộ phận (PK là đơn)
--   3NF: Không có phụ thuộc bắc cầu
--        - Thông tin nhóm → tách ra contact_groups
--        - Thông tin công ty → tách ra companies
-- =====================================================
