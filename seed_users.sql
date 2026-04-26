DO $$
DECLARE
    i integer;
    ho_array text[] := ARRAY['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Huỳnh', 'Phan', 'Vũ', 'Võ', 'Đặng', 'Bùi', 'Đỗ', 'Hồ', 'Ngô', 'Dương', 'Lý'];
    dem_array text[] := ARRAY['Văn', 'Thị', 'Hoàng', 'Tuấn', 'Ngọc', 'Thanh', 'Hữu', 'Đức', 'Quang', 'Minh', 'Hải', 'Xuân', 'Thu', 'Đình', 'Bảo', 'Gia'];
    ten_array text[] := ARRAY['Anh', 'Bình', 'Cường', 'Dũng', 'Tiến', 'Phong', 'Giang', 'Hùng', 'Hương', 'Khoa', 'Linh', 'Mai', 'Nam', 'Oanh', 'Phúc', 'Quỳnh', 'Sơn', 'Trang', 'Uyên', 'Vinh', 'Yến', 'Tú', 'Hà'];
    rand_ho text;
    rand_dem text;
    rand_ten text;
    full_name text;
BEGIN
    FOR i IN 1..100 LOOP
        rand_ho := ho_array[floor(random() * array_length(ho_array, 1)) + 1];
        rand_dem := dem_array[floor(random() * array_length(dem_array, 1)) + 1];
        rand_ten := ten_array[floor(random() * array_length(ten_array, 1)) + 1];
        full_name := rand_ho || ' ' || rand_dem || ' ' || rand_ten;

        INSERT INTO users (email, password, display_name, role)
        VALUES (
            'khachhang' || floor(random() * 90000 + 10000)::text || '@gmail.com',
            '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
            full_name,
            'user'
        );
    END LOOP;
END $$;
