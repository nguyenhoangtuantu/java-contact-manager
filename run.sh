#!/bin/bash
# Run script cho ứng dụng Quản lý Danh bạ
# Sử dụng: ./run.sh

echo "🚀 Đang khởi chạy ứng dụng Quản lý Danh bạ..."

# Classpath
CP="out:lib/flatlaf-3.4.1.jar:lib/gson-2.10.1.jar"

java -cp "$CP" Main
