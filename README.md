# Space Up

Ứng dụng dọn rác, tối ưu RAM, hạ nhiệt CPU chuyên nghiệp cho thiết bị Android. 

Được phát triển bởi **technology entertainment**.

---

## 🛠️ Hướng Dẫn Build APK Để Phát Hành (Releases)

Dự án sử dụng **Gradle Wrapper** đi kèm sẵn để thực hiện việc biên dịch và đóng gói ứng dụng mà không cần cài đặt Gradle toàn cục trên máy tính.

### 1. Build Bản Cài Đặt Thử Nghiệm (Debug APK)
Bản Debug thường dùng để cài đặt test nhanh trên thiết bị cá nhân hoặc chia sẻ cho các lập trình viên khác thử nghiệm. Bản này không yêu cầu cấu hình chữ ký số phức tạp và quá trình build diễn ra rất nhanh.

*   **Lệnh chạy trên Windows (PowerShell hoặc CMD):**
    ```powershell
    .\gradlew.bat assembleDebug
    ```
*   **Lệnh chạy trên Linux hoặc macOS:**
    ```bash
    ./gradlew assembleDebug
    ```
*   **Đường dẫn file APK sau khi build:**
    `app/build/outputs/apk/debug/app-debug.apk`

---

### 2. Build Bản Phát Hành Chính Thức (Release APK / AAB)
Bản Release là bản đóng gói tối ưu nhất, đã qua quy trình làm gọn mã nguồn (Minify / R8) để giảm thiểu tối đa dung lượng file. Bản này dùng để tải lên GitHub Releases hoặc Google Play Store.

#### A. Build file APK phát hành:
*   **Lệnh chạy trên Windows:**
    ```powershell
    .\gradlew.bat assembleRelease
    ```
*   **Lệnh chạy trên Linux hoặc macOS:**
    ```bash
    ./gradlew assembleRelease
    ```
*   **Đường dẫn file APK sau khi build:**
    `app/build/outputs/apk/release/app-release-unsigned.apk`
    *(Lưu ý: Để cài đặt bản Release lên máy khác, bạn cần ký số (sign) file APK này bằng keystore chính thức của bạn).*

#### B. Build file AAB (Android App Bundle - Dùng cho Google Play Store):
*   **Lệnh chạy trên Windows:**
    ```powershell
    .\gradlew.bat bundleRelease
    ```
*   **Đường dẫn file AAB sau khi build:**
    `app/build/outputs/bundle/release/app-release.aab`

---

### 3. Dọn Dẹp Bộ Nhớ Cache Build Cũ (Clean Build)
Trước khi build một bản phát hành chính thức mới, bạn nên chạy lệnh dọn dẹp các tệp tin cache đã biên dịch cũ để tránh xung đột mã nguồn:

*   **Dọn dẹp và build bản Release mới luôn (Windows):**
    ```powershell
    .\gradlew.bat clean assembleRelease
    ```
