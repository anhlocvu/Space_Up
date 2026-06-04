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

---

## 🔑 Hướng Dẫn Ký Chữ Ký Số (Signing APK)

Để cài đặt được bản **Release APK** lên các thiết bị Android thông thường, tệp tin APK bắt buộc phải được ký bằng một khóa bảo mật (Keystore).

### 1. Tạo Khóa Bảo Mật Mới (Keystore)
Nếu bạn chưa có tệp tin Keystore, bạn có thể tạo một tệp mới bằng cách sử dụng công cụ `keytool` (đi kèm sẵn trong bộ cài đặt Java JDK của máy tính):

Chạy lệnh sau trên terminal:
```powershell
keytool -genkey -v -keystore spaceup-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias spaceup-alias
```
*Hệ thống sẽ yêu cầu bạn nhập mật khẩu cho khóa, mật khẩu cho bí danh (alias) và các thông tin cơ bản của tổ chức.*
*Kết quả sẽ tạo ra một file khóa tên là `spaceup-release-key.jks`.*

---

### 2. Cách Ký APK Thủ Công Bằng Dòng Lệnh
Nếu bạn muốn ký thủ công file APK đã build xong (`app-release-unsigned.apk`):

Chạy công cụ `apksigner` đi kèm trong thư mục cài đặt Android SDK Build Tools (Ví dụ: `C:\Users\<Tên_User>\AppData\Local\Android\Sdk\build-tools\<phiên_bản>\apksigner.bat`):

```powershell
apksigner sign --ks spaceup-release-key.jks --out Space_Up_signed.apk app-release-unsigned.apk
```
*Nhập mật khẩu keystore bạn đã tạo ở bước 1 khi được hỏi. Tệp tin `Space_Up_signed.apk` được tạo ra là file cuối cùng sẵn sàng để chia sẻ lên GitHub Releases.*

---

### 3. Cách Tự Động Ký Khi Build Bằng Gradle (Khuyên Dùng)
Để quy trình tự động hóa hoàn toàn (mỗi lần chạy lệnh build Gradle là tự động xuất ra file APK đã ký sẵn), bạn làm theo các bước sau:

#### Bước A: Sao chép tệp khóa vào thư mục dự án
Chép file khóa `spaceup-release-key.jks` bạn vừa tạo vào thư mục `app/` của dự án.

#### Bước B: Cấu hình mật khẩu bảo mật trong `local.properties`
Mở file `local.properties` ở thư mục gốc của dự án (file này đã được đưa vào `.gitignore` nên mật khẩu sẽ không bao giờ bị lộ lên GitHub) và thêm vào cuối các dòng sau:
```properties
RELEASE_STORE_PASSWORD=mật_khẩu_keystore_của_bạn
RELEASE_KEY_PASSWORD=mật_khẩu_alias_của_bạn
RELEASE_KEY_ALIAS=spaceup-alias
```

#### Bước C: Cấu hình Gradle tự động ký
Cập nhật file `app/build.gradle.kts` ở phần cấu hình `android` để tự động đọc thông tin từ `local.properties` và tiến hành ký APK khi build bản Release.
*(Sau khi cấu hình, chỉ cần chạy lệnh `.\gradlew.bat assembleRelease` là Gradle tự động ký và tạo file APK hoàn thiện tại `app/build/outputs/apk/release/app-release.apk`)*.
