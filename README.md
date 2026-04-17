# 📇 Quản lý Danh bạ (Contact Management System)

👤 **Tác giả:** Nguyễn Hoàng Tuấn Tú (NGUYEN HOANG TUAN TU)

Dự án **Quản lý Danh bạ** là một ứng dụng Desktop được phát triển bằng ngôn ngữ Java, tích hợp với cơ sở dữ liệu **Supabase**. Ứng dụng cung cấp một giải pháp quản lý thông tin liên lạc toàn diện, có giao diện được thiết kế hiện đại và tối ưu hóa trải nghiệm người dùng.

## ✨ Tính năng nổi bật

- **🪪 Quản lý Liên hệ (Contacts CRUD):** Cho phép người dùng dễ dàng Thêm mới, Chỉnh sửa, Cập nhật và Xóa các thông tin liên lạc một cách nhanh chóng.
- **📂 Quản lý Nhóm Tùy chỉnh (Dynamic Groups):** Phân loại danh bạ theo các nhóm tùy chọn thay vì bị giới hạn bởi các nhóm cố định.
- **📤 Import/Export CSV:** Hỗ trợ nhập lượng lớn dữ liệu danh bạ bằng cách nhấn nút "Duyệt file" hoặc đơn giản là **Kéo thả (Drag & Drop)** tệp `.csv` / `.vcf` trực tiếp thẳng vào giao diện bảng. Tích hợp nút xuất dữ liệu toàn bộ danh sách ra file CSV.
- **📱 Chia sẻ qua Mã QR (Share via QR):** Tự động tạo và hiển thị mã QR có chứa chuẩn định dạng vCard của từng liên hệ. Người xung quanh có thể dùng điện thoại quét mã và lưu thẳng vào danh bạ thiết bị (công nghệ bởi ZXing).
- **📑 Phân trang Cổ điển (Pagination):** Xử lý mượt mà hàng trăm ngàn danh bạ mà không gây giật lag với hệ thống sang trang hiện đại, dễ thao tác.
- **🖱️ Menu Ngữ Cảnh (Context Menu) linh hoạt:** Tích hợp menu chuộc phải cho thao tác nhanh (Chuyển nhóm, Sửa, Chia sẻ, Xóa dữ liệu).
- **☁️ Đồng bộ CSDL Đám mây (Supabase Backend):** Toàn bộ dữ liệu được lưu trữ, bảo mật và truy xuất thông qua API của Supabase bằng JSON.
- **🔍 Xác nhận Trùng lặp (Duplicate Detection):** Tính năng tự động quét và cảnh báo các liên hệ có sự trùng lặp để dọn dẹp.
- **🎨 Giao diện Hiện đại (Modern UI):** Sử dụng thư viện [FlatLaf](https://www.formdev.com/flatlaf/) mang lại giao diện tinh tế, thân thiện.

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ lập trình:** Java (Java Swing)
- **Giao diện (Look and Feel):** FlatLaf (Version 3.4.1)
- **Xử lý JSON:** Gson (Version 2.10.1)
- **Cơ sở dữ liệu:** Supabase (PostgreSQL + REST API)
- **Tạo mã QR:** Thư viện ZXing (Zebra Crossing) gồm `core` và `javase` (Version 3.5.3)

## 📁 Cấu trúc Dự án

Dự án được phân chia thành các thư mục và package theo chuẩn mô hình MVC và Dịch vụ:

```text
Do-an-1/
├── src/
│   ├── config/      # Cấu hình hệ thống (SupabaseConfig)
│   ├── model/       # Các lớp đối tượng (Contact, Company, GroupInfo, ContactGroup)
│   ├── service/     # Nơi xử lý nghiệp vụ và gọi API (SupabaseService, ContactService)
│   ├── ui/          # Giao diện người dùng (MainFrame, ContactPanel, Dialogs, UIConstants)
│   ├── util/        # Các lớp tiện ích chung (VD: QRCodeUtil tạo chuỗi vCard và khởi tạo QR)
│   └── Main.java    # Khởi chạy ứng dụng
├── lib/             # Chứa các gói thư viện phụ thuộc (FlatLaf, Gson, ZXing Core & JavaSE)
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
