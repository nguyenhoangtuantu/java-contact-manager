# 📇 Quản lý Danh bạ (Contact Management System)

👤 **Tác giả:** Nguyễn Hoàng Tuấn Tú (NGUYEN HOANG TUAN TU)

Dự án **Quản lý Danh bạ** là một ứng dụng Desktop được phát triển bằng ngôn ngữ Java, tích hợp với cơ sở dữ liệu **Supabase**. Ứng dụng cung cấp một giải pháp quản lý thông tin liên lạc toàn diện, có giao diện được thiết kế hiện đại và tối ưu hóa trải nghiệm người dùng.

## ✨ Tính năng nổi bật

- **🪪 Quản lý Liên hệ (Contacts CRUD):** Cho phép người dùng dễ dàng Thêm mới, Chỉnh sửa, Cập nhật và Xóa các thông tin liên lạc một cách nhanh chóng.
- **📂 Quản lý Nhóm Tùy chỉnh (Dynamic Groups):** Phân loại danh bạ theo các nhóm tùy chọn (được lưu trữ động trên CSDL) thay vì bị giới hạn bởi các nhóm cố định.
- **☁️ Đồng bộ Supabase Backend:** Toàn bộ dữ liệu được lưu trữ, bảo mật và truy xuất thông qua API của Supabase bằng JSON (được xử lý thông qua Gson).
- **🔍 Xác định Trùng lặp (Duplicate Detection):** Tính năng tự động quét và cảnh báo các liên hệ có sự trùng lặp (số điện thoại, email) để người dùng dọn dẹp danh bạ.
- **🎨 Giao diện Hiện đại (Modern UI):** Sử dụng thư viện [FlatLaf](https://www.formdev.com/flatlaf/) cho các thành phần UI, mang lại giao diện tinh tế, thân thiện và hỗ trợ đầy đủ các chuẩn thiết kế hiện tại.

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ lập trình:** Java (Java Swing)
- **Giao diện (Look and Feel):** FlatLaf (Version 3.4.1)
- **Xử lý JSON:** Gson (Version 2.10.1)
- **Cơ sở dữ liệu:** Supabase (PostgreSQL + REST API)

## 📁 Cấu trúc Dự án

Dự án được phân chia thành các thư mục và package theo chuẩn mô hình MVC và Dịch vụ:

```text
Do-an-1/
├── src/
│   ├── config/      # Cấu hình hệ thống (SupabaseConfig)
│   ├── model/       # Các lớp đối tượng (Contact, Company, GroupInfo, ContactGroup)
│   ├── service/     # Nơi xử lý nghiệp vụ và gọi API (SupabaseService, ContactService)
│   ├── ui/          # Giao diện người dùng (MainFrame, ContactPanel, các Dialogs, UIConstants)
│   └── Main.java    # Khởi chạy ứng dụng
├── lib/             # Chứa các gói thư viện phụ thuộc (FlatLaf, Gson)
├── build.sh         # Script tự động biên dịch ứng dụng
├── run.sh           # Script khởi chạy ứng dụng sau khi đã biên dịch
├── config.properties# File thiết lập các hằng số, API key cho Supabase
└── supabase_schema.sql # Script chứa cấu trúc CSDL để khởi tạo trên Supabase
```

## 🚀 Hướng dẫn Cài đặt & Chạy ứng dụng

### 1. Yêu cầu Hệ thống
- Máy tính đã cài đặt **Java Development Kit (JDK) 8** trở lên (khuyến nghị Java 11+).
- Hệ điều hành Linux/macOS hoặc Windows (sử dụng Git Bash / WSL để chạy shell script).
- Truy cập internet (để kết nối với Supabase).

### 2. Thiết lập Cơ sở dữ liệu Supabase
1. Tạo một dự án trên [Supabase](https://supabase.com/).
2. Chạy nội dung trong file `supabase_schema.sql` vào SQL Editor của dự án Supabase để tạo các bảng cần thiết.
3. Điền các URL và API Key dự án của bạn (Project URL và anon/public key) vào file `config.properties`.

### 3. Biên dịch và Khởi chạy
Mở terminal tại thư mục gốc của dự án và thực thi các câu lệnh sau:

**Cấp quyền thực thi cho các file script (nếu cần):**
```bash
chmod +x build.sh run.sh
```

**Biên dịch dự án:**
```bash
./build.sh
```
*Script sẽ tự động tạo thư mục `out` và biên dịch các tệp Java vào đó.*

**Chạy ứng dụng:**
```bash
./run.sh
```

---
*Cảm ơn đã quan tâm tới dự án này. Ứng dụng là đồ án được nỗ lực xây dựng để đáp ứng yêu cầu quản lý danh bạ thực tế bằng công nghệ Java và Cloud Database hiện đại.*
