# 📇 Quản lý Danh bạ (Contact Management System)

👤 **Tác giả:** Nguyễn Hoàng Tuấn Tú (NGUYEN HOANG TUAN TU)

Dự án **Quản lý Danh bạ** là một ứng dụng Desktop được phát triển bằng ngôn ngữ Java, cung cấp một giải pháp quản lý thông tin liên lạc toàn diện, có giao diện được thiết kế hiện đại và tối ưu hóa trải nghiệm người dùng. Ứng dụng đã được nâng cấp kiến trúc để sử dụng **Cơ sở dữ liệu PostgreSQL cục bộ** kết hợp với **PostgREST** thông qua **Docker**, đảm bảo dữ liệu được an toàn, hỗ trợ sử dụng offline mượt mà và linh hoạt.

## 🎥 Video Demo

- **[Xem Video Demo Ứng dụng tại đây](https://drive.google.com/file/d/1sulAQOeW3Niq9LKDZ8TX1UAClMt_Tlov/view?usp=sharing)**

## ✨ Tính năng nổi bật

- **🔐 Phân quyền Hệ thống (Role-based Access Control):** Hệ thống đăng nhập bảo mật với các cấp độ người dùng khác nhau. Phân tách rõ ràng giữa Quản trị viên (Admin - quản lý hệ thống, khôi phục tài khoản) và Người dùng tiêu chuẩn (User - quản lý danh bạ cá nhân).
- **⚙️ Bảng Điều khiển Admin (Admin Dashboard):** Giao diện quản trị hệ thống độc lập cho phép Quản trị viên theo dõi số lượng người dùng, tìm kiếm, quản lý tài khoản và hỗ trợ khôi phục/đặt lại mật khẩu cho người dùng.
- **🎛️ Quản lý Hồ sơ & Tùy chọn (User Profile):** Cung cấp giao diện quản lý hồ sơ cá nhân (thông tin, đổi mật khẩu), bảng thống kê số liệu (danh bạ thêm trong tuần/tháng, tỷ lệ điền đủ thông tin) và thiết lập giao diện Sáng/Tối (**Dark/Light mode**). Mọi cài đặt đều được lưu trữ kiên định (persistent) vào cơ sở dữ liệu để đồng bộ trên mọi phiên làm việc.
- **🪪 Quản lý Liên hệ (Contacts CRUD):** Cho phép người dùng dễ dàng Thêm mới, Chỉnh sửa, Cập nhật và Xóa các thông tin liên lạc một cách nhanh chóng.
- **📂 Quản lý Nhóm Tùy chỉnh (Dynamic Groups):** Phân loại danh bạ theo các nhóm do người dùng tự tạo với các icon sinh động và bảng màu sắc (color palettes) tùy chỉnh.
- **✅ Lựa chọn Đa nhiệm (Multi-Selection):** Cơ chế chọn nhiều liên hệ trực quan thông qua thao tác **nhấn giữ (long-press)**, tự động làm xuất hiện cột checkbox cho các tác vụ hàng loạt (Xóa, Chuyển nhóm), giúp không gian UI luôn gọn gàng ở chế độ xem thường.
- **📤 Import/Export CSV:** Hỗ trợ nhập lượng lớn dữ liệu danh bạ bằng cách nhấn nút "Duyệt file" hoặc đơn giản là **Kéo thả (Drag & Drop)** tệp `.csv` / `.vcf` trực tiếp thẳng vào giao diện bảng. Tích hợp nút xuất dữ liệu ra file CSV.
- **📱 Chia sẻ qua Mã QR (Share via QR):** Tự động tạo và hiển thị mã QR theo chuẩn định dạng vCard của từng liên hệ để thiết bị di động quét và lưu nhanh chóng (sử dụng ZXing).
- **📑 Phân trang Cổ điển (Pagination):** Quản lý và tải danh sách liên hệ theo trang, không gây giật lag kể cả với số lượng dữ liệu cực lớn.
- **🔍 Xác nhận Trùng lặp & Dọn dẹp (Cleanup):** Tích hợp tính năng quét và cảnh báo các liên hệ có sự trùng lặp, thông báo dọn dẹp định kỳ để làm sạch dữ liệu danh bạ.
- **🎨 Giao diện Hiện đại (Modern UI):** Sử dụng thư viện [FlatLaf](https://www.formdev.com/flatlaf/) mang lại giao diện tinh tế, thân thiện.
- **🔔 Hệ thống Thông báo (Notification System):** Các thông báo pop-up trực quan, kịp thời phản hồi cho người dùng sau khi hoàn thành các tác vụ (thêm mới, cập nhật, xóa) hoặc nhắc nhở khi có lỗi xảy ra.
- **🛡️ Bảo mật Dữ liệu (Data Security):** Dữ liệu xác thực như mật khẩu được bảo vệ an toàn để ngăn ngừa các truy cập trái phép.
- **🔄 Đồng bộ hóa Mượt mà (Smooth Sync):** Tương tác với cơ sở dữ liệu và REST API một cách đa luồng/bất đồng bộ để đảm bảo giao diện luôn phản hồi nhanh (Responsive).

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ lập trình:** Java (Java Swing)
- **Giao diện (Look and Feel):** FlatLaf (Version 3.4.1)
- **Xử lý JSON:** Gson (Version 2.10.1)
- **Cơ sở dữ liệu:** PostgreSQL + PostgREST (cung cấp RESTful API) chạy trên nền tảng **Docker**
- **Tạo mã QR:** Thư viện ZXing (Zebra Crossing) gồm `core` và `javase` (Version 3.5.3)

## 🗄️ Chi tiết Cơ sở dữ liệu (Database Details)

Cơ sở dữ liệu PostgreSQL được thiết kế để đảm bảo tính toàn vẹn và tốc độ truy xuất cao:
- **Bảng `users`:** Quản lý thông tin tài khoản, bao gồm tên đăng nhập, mật khẩu mã hóa, vai trò (Role: admin/user) và trạng thái hoạt động.
- **Bảng `contacts`:** Cốt lõi của hệ thống, dùng để lưu chi tiết các liên hệ (Họ Tên, Số điện thoại, Email, Ngày sinh, Địa chỉ...). Được liên kết khóa ngoại (`user_id`) trỏ về bảng `users` nhằm đảm bảo mỗi người dùng quản lý vùng dữ liệu độc lập, riêng biệt.
- **Bảng `groups`:** Lưu trữ các danh mục, nhóm tùy chỉnh giúp phân loại và lọc các liên lạc một cách logic.
- **Ràng buộc và Hiệu năng:** Hệ thống sử dụng mạnh mẽ các ràng buộc tính toàn vẹn (như `ON DELETE CASCADE`), đồng thời hỗ trợ sử dụng API qua RESTful để dễ dàng thực hiện các thao tác CRUD mà không cần viết các câu truy vấn thủ công trong Java.

## 📁 Cấu trúc Dự án

Dự án được phân chia thành các thư mục và package theo chuẩn mô hình MVC và Dịch vụ:

```text
Do-an/java-contact-manager/
├── src/
│   ├── config/      # Cấu hình hệ thống (Khai báo biến môi trường, API)
│   ├── model/       # Các lớp đối tượng (Contact, User, GroupInfo...)
│   ├── service/     # Nơi xử lý nghiệp vụ, giao tiếp với REST API (PostgREST)
│   ├── ui/          # Giao diện ứng dụng (MainFrame, LoginFrame, AdminDashboardFrame...)
│   ├── util/        # Các lớp tiện ích chung (QRCodeUtil, ThemeUtil...)
│   └── Main.java    # Điểm khởi chạy ứng dụng (Entry point)
├── lib/             # Chứa các gói thư viện phụ thuộc (FlatLaf, Gson, ZXing)
├── docker/          # Thư mục chứa kịch bản khởi tạo CSDL PostgreSQL
├── docker-compose.yml # Tệp cấu hình Docker Compose cho Database và PostgREST
├── seed_users.sql   # Script SQL tạo dữ liệu mẫu/quản trị viên
├── build.sh         # Script tự động biên dịch ứng dụng (.sh)
├── run.sh           # Script khởi chạy ứng dụng sau khi đã biên dịch (.sh)
└── config.properties# File thiết lập các hằng số, API key và URL của PostgREST
```

## 🚀 Hướng dẫn Cài đặt & Chạy ứng dụng

### 1. Yêu cầu Hệ thống
- Máy tính đã cài đặt **Java Development Kit (JDK) 8** trở lên (khuyến nghị Java 11+).
- Đã cài đặt **Docker** và **Docker Compose** để khởi chạy môi trường CSDL nội bộ.
- Hệ điều hành Linux/macOS hoặc Windows (sử dụng Git Bash / WSL để chạy shell script, có thể dùng IDE để build trực tiếp).

### 2. Thiết lập Môi trường Database (Local Docker)
Hệ thống sử dụng Docker để tạo nhanh một máy chủ PostgreSQL cùng API PostgREST ngay trên máy của bạn.
1. Mở terminal tại thư mục gốc của dự án.
2. Khởi chạy các container bằng lệnh sau:
   ```bash
   docker-compose up -d
   ```
   *Quá trình này sẽ khởi chạy database PostgreSQL ở cổng `5432` và API Server PostgREST ở cổng `3000`. Cấu trúc bảng sẽ được tạo tự động khi khởi chạy lần đầu thông qua thư mục `docker/`.*
3. (Tùy chọn) Chạy file `seed_users.sql` để nạp dữ liệu người dùng mẫu hoặc tài khoản Admin ban đầu.
4. Mở file `config.properties`, đảm bảo URL đang trỏ về local:
   ```properties
   SUPABASE_URL=http://localhost:3000
   ```
   *(Lưu ý: Biến vẫn mang tên SUPABASE do lịch sử nâng cấp, nhưng hệ thống hoàn toàn chạy offline qua PostgREST).*

### 3. Biên dịch và Khởi chạy
Mở terminal tại thư mục gốc của dự án và thực thi các câu lệnh sau:

**Cấp quyền thực thi cho các file script (nếu sử dụng Linux/Mac hoặc Git Bash):**
```bash
chmod +x build.sh run.sh
```

**Biên dịch dự án:**
```bash
./build.sh
```
*Script sẽ tự động tạo thư mục `out` và biên dịch các tệp Java từ thư mục `src` sang.*

**Chạy ứng dụng:**
```bash
./run.sh
```

---
*Cảm ơn đã quan tâm tới dự án này. Ứng dụng là đồ án được nỗ lực xây dựng để đáp ứng yêu cầu quản lý danh bạ kết hợp quản trị tài khoản thực tế bằng công nghệ Java Swing, FlatLaf, và Docker.*
