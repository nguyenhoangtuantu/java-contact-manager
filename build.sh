#!/bin/bash
# Build script cho ứng dụng Quản lý Danh bạ
# Sử dụng: ./build.sh

echo "🔨 Đang biên dịch ứng dụng Quản lý Danh bạ..."

# Tạo thư mục output
mkdir -p out

# Classpath
CP="lib/flatlaf-3.4.1.jar:lib/gson-2.10.1.jar:lib/core-3.5.3.jar:lib/javase-3.5.3.jar"

# Compile tất cả file Java
javac -cp "$CP" -d out -sourcepath src \
    src/model/ContactGroup.java \
    src/model/Contact.java \
    src/model/Company.java \
    src/model/GroupInfo.java \
    src/config/SupabaseConfig.java \
    src/service/SupabaseService.java \
    src/service/ContactService.java \
    src/ui/UIConstants.java \
    src/ui/ContactPanel.java \
    src/ui/ContactDialog.java \
    src/ui/DuplicateDialog.java \
    src/ui/CleanupDialog.java \
    src/ui/MainFrame.java \
    src/Main.java

if [ $? -eq 0 ]; then
    echo "✅ Biên dịch thành công! Chạy: ./run.sh"
else
    echo "❌ Lỗi biên dịch!"
    exit 1
fi
