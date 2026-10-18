# Công việc: Tiếng Việt và ngôn ngữ mặc định

Trạng thái: Đã triển khai công việc 1–5 theo [kế hoạch](plan.md). Công việc 6 còn Lint nền và các kiểm tra nâng cấp/HyperOS thật; không đánh dấu toàn bộ nghiệm thu là đã đạt.

## Công việc 1: Chọn được tiếng Việt trong cài đặt Android

**Mô tả:** Thêm bản dịch Việt và đăng ký locale để người dùng Android 13+ có thể chọn Tiếng Việt trong cài đặt ngôn ngữ ứng dụng. Bổ sung kiểm tra tài nguyên ngay trong cùng thay đổi.

**Tiêu chí chấp nhận:**
- [x] Hai tệp Việt có đủ 75 khóa hiện có, giữ đúng placeholder, tên riêng và chuỗi kỹ thuật.
- [x] `vi` được đăng ký; `LocaleHelper` nhận `vi` và `vi-VN`; 10 ngôn ngữ cũ giữ nguyên.
- [x] Script kiểm tra phát hiện thiếu/trùng khóa, placeholder sai và chuỗi kỹ thuật bị sửa.

**Xác minh:** `python tools/check_locales.py`; `gradle :app:assembleDebug --stacktrace`; trên Android 13+ chọn Tiếng Việt trong App languages và mở app. Kiểm tra hành vi chuẩn hóa được chạy cùng self-check ở công việc 3.

**Phụ thuộc:** Không.

**Tệp dự kiến:**
- `app/src/main/res/values-vi/strings.xml`
- `app/src/main/res/values-vi/feature_strings.xml`
- `app/src/main/res/xml/locales_config.xml`
- `app/src/main/java/com/reed/fcmguard/LocaleHelper.java`
- `tools/check_locales.py`

**Quy mô:** Vừa, 5 tệp. **Đối chiếu đặc tả:** tiêu chí 1, 5, 6.

## Công việc 2: Chọn được tiếng Việt trong app

**Mô tả:** Hoàn thiện hộp chọn tương thích Android cũ và nhãn nút đổi ngôn ngữ mà không thay cơ chế chọn/hoạt ảnh hiện có.

**Tiêu chí chấp nhận:**
- [x] Mục Tiếng Việt có mã `vi` đúng vị trí; chọn lại English hoặc Theo hệ thống vẫn hoạt động.
- [x] Chu kỳ nhãn nút có “Ngôn ngữ”; nhãn khởi tạo trong layout dùng resource thay chuỗi tiếng Anh ghi trực tiếp.
- [x] Kiểm tra tài nguyên xác nhận số lượng và thứ tự nhãn khớp mã, không chỉ xác nhận có chữ `vi`.

**Xác minh:** `python tools/check_locales.py`; `python tools/check_layout_profiles.py`; `gradle :app:assembleDebug --stacktrace`; trên Android dưới 13 chọn Việt → English → Theo hệ thống, xác nhận mục được đánh dấu đúng. Android 13+ vẫn mở cài đặt hệ thống, hoặc dùng hộp chọn khi intent không khả dụng.

**Phụ thuộc:** Công việc 1.

**Tệp dự kiến:**
- `app/src/main/res/values/arrays.xml`
- `app/src/main/java/com/reed/fcmguard/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `tools/check_locales.py`

**Quy mô:** Vừa, 4 tệp. **Đối chiếu đặc tả:** tiêu chí 4, 5.

## Checkpoint A: Sau công việc 1–2

- [x] Script tài nguyên và bố cục đạt; APK biên dịch được.
- [ ] Hai đường chọn ngôn ngữ có mục Tiếng Việt hoạt động, không lệch nhãn/mã.
- [x] Ghi kết quả hoặc lý do chưa chạy; rà diff trước khi đổi chính sách mặc định.

Hộp chọn trong app đã kiểm tra bằng UI trên API 26. Trên API 35 ATD, không có Activity cài đặt App languages của hệ điều hành (`resolve-activity` trả No activity found); locale native được kiểm bằng API thật/self-check và `cmd locale`. Màn hình App languages trên HyperOS thật vẫn cần kiểm tra riêng.

## Công việc 3: Khởi tạo mặc định tiếng Việt một lần

**Mô tả:** Bổ sung khởi tạo locale trong helper, nối vào vòng đời Activity trước khi lấy chuỗi giao diện và kiểm tra mã thật bằng một self-check Android nhỏ, không thêm thư viện kiểm thử.

**Tiêu chí chấp nhận:**
- [x] Chưa có lựa chọn riêng thì dùng Việt; lựa chọn native/legacy cụ thể được giữ đúng thứ tự ưu tiên; migration chỉ hoàn tất khi xử lý thành công.
- [x] Cờ mặc định độc lập cờ migration; chọn Theo hệ thống sau khởi tạo không bị ghi đè khi mở lại hoặc nâng cấp Android; manager không có thì không làm mất dữ liệu.
- [x] Self-check thực thi helper trên Android, bao phủ chuẩn hóa, khởi tạo lặp lại, lựa chọn cụ thể, Theo hệ thống và migration; không chỉ tìm chuỗi trong Java.

**Xác minh:** `gradle :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace`; chạy self-check theo lệnh ở công việc 6 trên API 21–32 và API 33+; kiểm tra lần mở đầu trên hệ thống tiếng Anh và Trung không tạo vòng lặp recreate. Trường hợp manager null dùng context kiểm thử có cùng preferences và trả null cho locale service.

**Phụ thuộc:** Công việc 1–2.

**Tệp dự kiến:**
- `app/src/main/java/com/reed/fcmguard/LocaleHelper.java`
- `app/src/main/java/com/reed/fcmguard/MainActivity.java`
- `app/src/androidTest/java/com/reed/fcmguard/LocaleHelperSelfCheck.java`
- `app/build.gradle` — chỉ cấu hình `testInstrumentationRunner` của APK kiểm thử.

**Quy mô:** Vừa, 4 tệp. **Đối chiếu đặc tả:** tiêu chí 2, 3, 4, 5, 7.

## Công việc 4: Thông báo dùng ngôn ngữ của app

**Mô tả:** Áp dụng cùng chính sách locale khi service tạo/cập nhật thông báo, bao gồm chạy sau boot và sau khi người dùng đổi ngôn ngữ lúc service còn sống.

**Tiêu chí chấp nhận:**
- [x] Service khởi động trước Activity vẫn dùng mặc định Việt nếu chưa có lựa chọn riêng; lần cập nhật sau dùng lựa chọn hiện tại.
- [x] Chuỗi lỗi, tên/mô tả channel và nội dung thông báo do app tạo dùng context đúng ngôn ngữ; giữ channel ID và cơ chế bảo vệ FCM.
- [x] Khởi động sau boot và bật/tắt bảo vệ vẫn hoạt động; không sửa BootReceiver nếu service tự xử lý được.

**Xác minh:** chạy lại self-check sau khi mở rộng kiểm tra lấy chuỗi bằng context service; `gradle :app:assembleDebug --stacktrace`; trên máy thử bật bảo vệ, đổi Việt/English, quan sát thông báo được cập nhật, reboot rồi quan sát thông báo đầu tiên. Chỉ kiểm tra FCM thực tế trên máy HyperOS phù hợp; emulator kiểm tra phần Android/locale.

**Phụ thuộc:** Công việc 3.

**Tệp dự kiến:**
- `app/src/main/java/com/reed/fcmguard/GuardService.java`
- `app/src/main/java/com/reed/fcmguard/LocaleHelper.java` — nếu cần điều chỉnh thời điểm/context.
- `app/src/androidTest/java/com/reed/fcmguard/LocaleHelperSelfCheck.java`

**Quy mô:** Vừa, tối đa 3 tệp. **Đối chiếu đặc tả:** tiêu chí 4, 6, 7.

## Checkpoint B: Sau công việc 3–4

- [x] Build app và APK self-check đạt; self-check đạt trên hai nhánh phiên bản Android.
- [x] Chưa chọn → Việt; đã chọn riêng → giữ nguyên; chọn Theo hệ thống sau khởi tạo → không bị ép lại Việt.
- [x] Service trước Activity, đổi ngôn ngữ khi đang chạy và đường boot đã được kiểm tra; ghi rõ mục nào chỉ kiểm được trên emulator.

## Công việc 5: Cập nhật tài liệu ngôn ngữ

**Mô tả:** Cập nhật hai README hiện có để mô tả 11 ngôn ngữ và quy tắc mặc định một lần.

**Tiêu chí chấp nhận:**
- [x] Cả hai README ghi có tiếng Việt, tổng 11 ngôn ngữ và mặc định Việt khi chưa chọn riêng.
- [x] Mô tả đúng việc giữ lựa chọn cũ và tiếp tục hỗ trợ Theo hệ thống; không thêm cam kết bảo đảm thông báo FCM.

**Xác minh:** `rg -n '10-language|10 native app languages|10 种|11|Vietnamese|越南语' README.md README.zh-CN.md`; đọc lại các đoạn ngôn ngữ trong cả hai tệp; `git diff --check`.

**Phụ thuộc:** Công việc 4.

**Tệp dự kiến:** `README.md`, `README.zh-CN.md`.

**Quy mô:** Nhỏ, 2 tệp. **Đối chiếu đặc tả:** tiêu chí 8.

## Công việc 6: Xác minh cuối và ghi bằng chứng

**Mô tả:** Chạy các kiểm tra phù hợp một lượt sau thay đổi cuối cùng, hoàn thành ma trận hành vi và ghi kết quả thật. Không mở rộng thành refactor hay đổi CI để vượt kiểm tra.

**Tiêu chí chấp nhận:**
- [ ] Script tài nguyên/bố cục, build và Lint đạt; self-check đạt trên hai nhánh Android hoặc mục chưa chạy được ghi rõ là còn thiếu.
- [x] Ma trận dưới đây có kết quả và môi trường; chỉ đánh dấu hoàn thành toàn tính năng khi đủ bằng chứng cho các tiêu chí đặc tả.
- [x] Diff chỉ thuộc phạm vi tiếng Việt; không thêm dependency/quyền, không đổi SDK/signing/CI, không commit/push/phát hành.

**Xác minh:** chạy từ repository root; dùng thiết bị/emulator thử nghiệm đang được chọn bởi ADB. Tìm toolchain có sẵn và cấu hình môi trường cục bộ trước, không đổi cấu hình SDK/signing dự án. APK cũ/mới trong bài kiểm tra nâng cấp phải ký cùng khóa.

```powershell
python tools/check_locales.py
python tools/check_layout_profiles.py
gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --stacktrace
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.reed.fcmguard.test/com.reed.fcmguard.LocaleHelperSelfCheck
git diff --check
```

Runner phải trả thông báo self-check thành công và `INSTRUMENTATION_CODE: -1`; lỗi assertion trả lỗi rõ, không coi riêng exit code của lệnh ADB là bằng chứng đạt. Với targetSdk 22 hiện có, nếu máy thử chặn cài APK target cũ, xác minh cách cài phù hợp cho phiên bản Android đó và ghi thao tác thực tế; không tăng targetSdk để né chặn.

**Phụ thuộc:** Công việc 1–5.

**Tệp dự kiến:** cập nhật kết quả trong `tasks/todo.md`; cập nhật trạng thái `tasks/plan.md` và `SPEC-vietnamese.md` khi đủ điều kiện. Không sửa mã sản phẩm trong công việc xác minh; nếu phát hiện lỗi, quay lại công việc liên quan.

**Quy mô:** Nhỏ, tối đa 3 tệp tài liệu. **Đối chiếu đặc tả:** toàn bộ tiêu chí 1–8.

### Ma trận kết quả runtime

Ghi model/API, ngôn ngữ hệ thống, thao tác và kết quả vào cột cuối. “Chưa chạy” không đồng nghĩa đạt.

| Trường hợp | Kỳ vọng | Kết quả |
| --- | --- | --- |
| Cài mới, hệ thống Anh/Trung, API dưới 33 và API 33+ | App mặc định Việt | UI cài mới đạt trên API 26/35 khi hệ thống Anh; self-check khởi tạo sạch đạt thêm trên API 35 hệ thống Trung. UI Việt quan sát được khi hệ thống Trung. Chưa chạy API 26 hệ thống Trung. |
| Bản cũ đang Theo hệ thống, cập nhật cùng chữ ký | Chuyển Việt một lần | Self-check đạt với preferences cũ; chưa chạy nâng cấp giữa hai APK phiên bản cũ/mới. |
| Bản cũ đã chọn English/Trung, cập nhật cùng chữ ký | Giữ lựa chọn | Self-check giữ legacy English và ưu tiên native English đạt; chưa chạy nâng cấp giữa hai APK phiên bản cũ/mới. |
| Legacy có ngôn ngữ riêng, native đang trống | Chuyển đúng lựa chọn legacy | Đạt trong self-check API 35: English và French. |
| Native và legacy có lựa chọn khác nhau | Giữ lựa chọn native | Đạt trong self-check API 35: native English ưu tiên hơn legacy Trung. |
| Manager null trong self-check rồi thử lại | Không mất lựa chọn/cờ migration | Đạt trên API 35 với context kiểm thử; thử lại chuyển đúng French. |
| Chọn Việt/English, đóng và mở lại | Giữ lựa chọn; nhãn/mã khớp | Đạt: hộp chọn API 26, native locale API 35; đối chiếu XML UI và preferences/locale thực tế. |
| Chọn Theo hệ thống sau khởi tạo, mở lại/reboot | Không tự ép về Việt | Self-check cả hai API đạt; force-stop/mở lại giữ System, UI Anh trên API 26 và UI Trung trên API 35. Reboot riêng cho System chưa chạy. |
| Thiết bị nâng từ Android cũ lên Android 13+ | Giữ lựa chọn đã có | Self-check dữ liệu tương đương trước/sau nâng cấp đạt; chưa nâng cấp hệ điều hành thực tế. |
| Service chạy trước Activity sau cập nhật/reboot | Thông báo dùng mặc định/lựa chọn đúng | API 35: service tự chạy sau boot trước khi mở Activity, native locale Việt được giữ và thông báo là “Đang bảo vệ · theo dõi thay đổi” dù hệ thống Trung. Tắt bảo vệ qua UI dừng service và lưu `enabled=false`. Chỉ kiểm trên emulator, không có máy HyperOS thật. |
| Đổi ngôn ngữ khi service đang chạy | Lần tạo/cập nhật thông báo tiếp dùng locale mới | Đạt trên API 35: `dumpsys notification` có “Protection active · event-driven” khi English, “Đang bảo vệ · theo dõi thay đổi” khi Việt; channel đổi tên tương ứng. |
| Màn hình 320dp và 393dp | Chữ có dấu đúng, không cắt nội dung thao tác | Đã xem screenshot trên API 26 ở 320dp/393dp và API 35 khoảng 393dp. Các nút và chữ Việt quan sát được đều vừa; kiểm tra hình học 320–480dp đạt. |

### Bằng chứng và môi trường thực hiện

- Windows, JDK 17.0.15, Gradle 8.7 tải vào `%TEMP%/fcmguard-tools/`, Android SDK 35 có sẵn tại `C:/Users/Admin/AppData/Local/Android/Sdk`. Không đổi SDK, signing config hoặc CI.
- Emulator riêng của công việc: `emulator-5560` (API 26 Google Play x86, hệ thống Anh), `emulator-5562` (API 35 AOSP ATD x86_64, hệ thống Anh rồi Trung). Không dùng điện thoại hoặc tài khoản cá nhân.
- `python tools/check_locales.py`: đạt, 75 chuỗi. Lần chạy đầu trước khi thêm bản dịch thất bại vì thiếu `values-vi`, sau khi thêm bản dịch thì đạt.
- `python tools/check_layout_profiles.py`: đạt cả 6 profile 320–480dp. `git diff --check`: đạt.
- `:app:assembleDebug` và `:app:assembleDebugAndroidTest`: đạt. APK tại `app/build/outputs/apk/debug/app-debug.apk` và `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Self-check ban đầu thất bại đúng ở “fresh default: expected vi, got system”; sau thay đổi helper thì đạt trên API 26 và API 35. Thêm kiểm tra channel tái hiện “expected Bảo vệ FCM Guard, got FCM Guard protection”; sau sửa service thì đạt trên cả hai API.
- Các lần self-check cuối trả `LocaleHelper self-check passed on API 26/35` và `INSTRUMENTATION_CODE: -1`. API 35 hệ thống Trung cũng đạt. Đã chạy bản APK kiểm thử thật, không mô phỏng Android API bằng stub.
- Android 15 yêu cầu `adb -s emulator-5562 install -r --bypass-low-target-sdk-block ...` vì targetSdk 22; giữ nguyên target của dự án. ATD mặc định tắt vẽ UI, nên chỉ bật `debug.hwui.drawing_enabled=true` trong emulator để chụp màn hình.
- `:app:lintDebug` thất bại: **9 lỗi, 31 cảnh báo**. Đã xuất `git archive HEAD` vào thư mục baseline tạm và chạy Lint mã gốc: **9 lỗi, 32 cảnh báo**. Đối chiếu nội dung lỗi không phát hiện lỗi mới; các lỗi có sẵn gồm API tối thiểu của `canWrite`/thuộc tính theme, `WRITE_SETTINGS` và target SDK cũ. Không thêm suppression, baseline Lint hay hạ mức kiểm tra.
- Khóa debug được tạo cục bộ theo cấu hình alias hiện có, lưu trong tệp keystore đã được `.gitignore` loại trừ. APK thử nghiệm có thể không cài đè bản GitHub do khác chữ ký; không xóa dữ liệu hay gỡ bản của người dùng để vượt lỗi này.
- Log/screenshot chi tiết nằm tại `%TEMP%/fcmguard-tools/`; báo cáo Lint hiện tại ở `app/build/reports/lint-results-debug.html`. Không commit, push hay phát hành.
- APK chính: 185.136 byte; SHA-256 `92855DE8D14A784D0DC2D602941FB74CE7DD039868B774BB08A2D39D90B3B2C1`. `aapt2 dump badging` xác nhận locale `vi` đã được đóng gói, cùng 10 ngôn ngữ cũ (tiếng Anh qua resource fallback).

## Checkpoint hoàn tất

- [ ] Mọi tiêu chí đặc tả có bằng chứng đạt; ghi riêng giới hạn quan sát trên HyperOS nếu chưa có máy thật.
- [x] Hai README thống nhất với hành vi cuối cùng; kiểm tra diff và phạm vi thay đổi.
- [x] Bàn giao tóm tắt thay đổi, các lệnh đã chạy và phần chưa xác minh; APK chỉ được báo đã build nếu có artifact thực tế.
