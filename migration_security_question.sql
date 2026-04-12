-- =====================================================
-- MIGRATION: Thêm Câu hỏi bảo mật cho tính năng Quên mật khẩu
-- Chạy script này trong Supabase SQL Editor
-- =====================================================

-- Thêm cột câu hỏi bảo mật vào bảng users
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_question TEXT;

-- Thêm cột câu trả lời bảo mật (lưu dưới dạng SHA-256 hash, lowercase)
ALTER TABLE users ADD COLUMN IF NOT EXISTS security_answer TEXT;

-- =====================================================
-- KẾT QUẢ sau migration:
--
--   users
--     id, email, password, display_name, created_at
--     + security_question  (TEXT) — Câu hỏi bảo mật (plain text)
--     + security_answer    (TEXT) — Câu trả lời (SHA-256 hash, lowercase)
--
-- Các câu hỏi bảo mật mặc định trong ứng dụng:
--   1. "Tên trường tiểu học của bạn là gì?"
--   2. "Tên thú cưng đầu tiên của bạn là gì?"
--   3. "Món ăn yêu thích của bạn là gì?"
--   4. "Tên người bạn thân nhất thời thơ ấu?"
--   5. "Thành phố nơi bạn sinh ra?"
-- =====================================================
