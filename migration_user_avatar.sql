-- =====================================================
-- MIGRATION: Thêm cột avatar vào bảng users
-- Chạy script này trong Supabase SQL Editor
-- =====================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar TEXT;

-- =====================================================
-- KẾT QUẢ sau migration:
-- Bảng users giờ đây có các cột:
--   id, email, password, display_name, created_at, avatar
-- =====================================================
