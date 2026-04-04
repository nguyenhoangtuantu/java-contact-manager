-- ============================================================
-- FIX: Thêm các nhóm mặc định nếu chưa có
-- Chạy trong Supabase SQL Editor
-- ============================================================

-- Thêm nhóm mặc định (bỏ qua nếu đã tồn tại)
INSERT INTO contact_groups (id, name, display_name, icon, color_hex)
VALUES
  (1, 'important', 'Quan trọng', '⭐', '#F59E0B'),
  (2, 'family', 'Gia đình', '🏠', '#10B981'),
  (3, 'work', 'Công việc', '💼', '#3B82F6'),
  (4, 'friends', 'Bạn bè', '👥', '#8B5CF6'),
  (5, 'other', 'Khác', '📋', '#6B7280')
ON CONFLICT (id) DO NOTHING;

-- Kiểm tra
SELECT * FROM contact_groups ORDER BY id;
