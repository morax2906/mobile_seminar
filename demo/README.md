# Ứng Dụng Mobile Demo: Biometric Authentication (Xác thực Sinh Trắc Học)

Ứng dụng mẫu minh họa cơ chế **Xác thực Sinh trắc học (Biometric Authentication)** phục vụ bài thuyết trình Seminar. Ứng dụng được xây dựng bằng **Kotlin** và giao diện hiện đại **Jetpack Compose**.

---

## 1. Tính Năng & Kiến Trúc Dự Án

* **Đăng nhập truyền thống (Fallback):** Nhập Tên đăng nhập & Mật khẩu.
* **Xác thực Vân tay (Fingerprint):**
  * Sử dụng thư viện chuẩn của Android: `androidx.biometric:biometric` (`BiometricPrompt` & `BiometricManager`).
  * Minh họa quy trình **Verification 1:1** (So khớp trực tiếp mẫu sinh trắc học với Template lưu an toàn trong phần cứng TEE/SE).
  * Xử lý đầy đủ các kịch bản: Thành công, Thất bại (sai vân tay), Người dùng bấm Hủy, Chưa đăng ký vân tay trên máy.
* **Kiến trúc mở (Dành cho Đồng đội phát triển Nhận diện Khuôn mặt):**
  * Áp dụng **Strategy Pattern** thông qua interface `BiometricAuthenticator`.
  * Class `FaceAuthenticator` đã được dựng sẵn khung kết nối với ViewModel và giao diện. Đồng đội chỉ cần cài đặt logic khuôn mặt tại file này mà không cần sửa đổi giao diện chính.

---

## 2. Cách Mở và Chạy Dự Án Trên Android Studio

### Bước 1: Mở dự án trong Android Studio
1. Khởi động **Android Studio** (Hedgehog, Iguana, Jellyfish, Koala hoặc mới hơn).
2. Chọn **File $\rightarrow$ Open...**
3. Trỏ đường dẫn đến thư mục **`demo`** trong repository này (`.../mobile_seminar/demo`) và nhấn **OK**.
4. Chờ Android Studio đồng bộ Gradle (**Sync Project with Gradle Files**).
   * Dự án sử dụng **Gradle 8.7** và **JDK 17** (được tích hợp sẵn trong Android Studio).

### Bước 2: Thiết lập vân tay trên Máy ảo (Android Emulator)
Để thử nghiệm tính năng xác thực vân tay trên máy ảo:
1. Mở máy ảo Android (Pixel với API 28 trở lên).
2. Vào **Settings $\rightarrow$ Security $\rightarrow$ Fingerprint** (hoặc Pixel Imprint).
3. Thiết lập mã PIN dự phòng, sau đó tiến hành đăng ký vân tay.
4. Khi màn hình máy ảo yêu cầu chạm ngón tay vào cảm biến:
   * **Cách 1:** Nhấp vào nút **Ba chấm (...)** ở thanh công cụ máy ảo $\rightarrow$ Mục **Fingerprint** $\rightarrow$ Bấm nút **Touch Sensor**.
   * **Cách 2:** Chạy lệnh Terminal qua adb:
     ```bash
     adb emu finger touch 1
     ```

### Bước 3: Chạy ứng dụng và Demo các kịch bản
1. Nhấn nút **Run 'app'** (tam giác xanh) trên Android Studio.
2. Kiểm thử các tình huống theo checklist seminar:
   * **Thành công:** Nhấn *Xác thực bằng Vân tay* $\rightarrow$ Gửi tín hiệu vân tay hợp lệ (`Touch Sensor` với ID 1) $\rightarrow$ Ứng dụng chuyển sang màn hình **HomeScreen** hiển thị chi tiết phiên.
   * **Không khớp:** Gửi tín hiệu vân tay không hợp lệ (ví dụ ID 2 chưa đăng ký) $\rightarrow$ Thông báo *"Mẫu sinh trắc học không khớp"*.
   * **Hủy xác thực:** Nhấn *Hủy bỏ* trên dialog $\rightarrow$ Trạng thái chuyển về an toàn, không bị crash ứng dụng.

---

## 3. Hướng Dẫn Dành Cho Thành Viên Nhận Diện Khuôn Mặt (Face Recognition)

Đồng đội sẽ làm việc trên nhánh **`feature/biometric-face`**.

File cần làm việc:
👉 `app/src/main/java/com/seminar/biometric/biometric/FaceAuthenticator.kt`

Bạn có 2 lựa chọn triển khai:
* **Lựa chọn 1 (Dùng BiometricPrompt hệ điều hành):** Cấu hình `BiometricPrompt` tương tự `FingerprintAuthenticator` trên các thiết bị Android hỗ trợ Face Unlock phần cứng.
* **Lựa chọn 2 (Mở Camera quét mặt thời gian thực):**
  * Tích hợp CameraX (`androidx.camera`) và Google ML Kit Face Detection (`com.google.mlkit:face-detection`).
  * Khởi chạy màn hình quét mặt, kiểm tra khuôn mặt người dùng và trả về `BiometricAuthResult.Success` hoặc `BiometricAuthResult.Failed`.

---

## 4. Cấu Trúc Thư Mục Source Code

```text
demo/
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/seminar/biometric/
│       ├── MainActivity.kt               # Activity chính tích hợp Compose & BiometricPrompt
│       ├── BiometricApp.kt               # Application class
│       ├── biometric/                    # Tầng kiến trúc sinh trắc học
│       │   ├── BiometricAuthenticator.kt # Interface cốt lõi
│       │   ├── BiometricModels.kt        # Enums, Sealed classes kết quả & trạng thái
│       │   ├── FingerprintAuthenticator.kt # Triển khai Vân tay qua BiometricPrompt
│       │   └── FaceAuthenticator.kt      # Vị trí tích hợp Khuôn mặt cho đồng đội
│       ├── data/                         # Tầng dữ liệu & phiên đăng nhập
│       │   ├── AuthRepository.kt
│       │   └── UserSession.kt
│       └── ui/                           # Giao diện Jetpack Compose
│           ├── theme/                    # Material 3 Theme, Typography, Colors
│           ├── login/                    # LoginScreen & LoginViewModel
│           └── home/                     # HomeScreen hiển thị kết quả xác thực
└── gradle/
    └── libs.versions.toml                # Quản lý phiên bản thư viện hiện đại
```
