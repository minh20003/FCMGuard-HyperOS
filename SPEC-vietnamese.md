# Đặc tả: Tiếng Việt và ngôn ngữ mặc định

Trạng thái: Đã triển khai theo kế hoạch sau yêu cầu dùng skill `incremental-implementation`. Build và self-check đạt trên emulator API 26/35. Lint còn 9 lỗi có sẵn trong mã gốc; kiểm tra trên HyperOS thật và nâng cấp APK/OS thực tế chưa hoàn tất. Xem bằng chứng trong `tasks/todo.md`.

## Mục tiêu

Thêm tiếng Việt đầy đủ cho FCM Guard và dùng tiếng Việt mặc định, kể cả khi điện thoại đặt tiếng Anh hoặc tiếng Trung. Giữ khả năng chọn 10 ngôn ngữ hiện có và tùy chọn Theo hệ thống.

Đây là một tính năng bản địa hóa; không cần chia thành các module riêng.

## Giả định và hành vi cần đạt

- Dịch toàn bộ 75 chuỗi hiện có trong `strings.xml` và `feature_strings.xml`, gồm giao diện, trạng thái, hướng dẫn, lỗi và thông báo do app tạo.
- Giữ nguyên tên FCM Guard, FCM/GCM, HyperOS, tên ứng dụng bên thứ ba, mã gói, khóa cài đặt và placeholder định dạng.
- Lần đầu áp dụng bản mới, nếu chưa có lựa chọn ngôn ngữ cụ thể, app chọn tiếng Việt một lần. Chính sách này bao gồm bản cũ đang dùng mặc định Theo hệ thống.
- Nếu đã chọn riêng một ngôn ngữ, giữ lựa chọn đó, bao gồm lựa chọn cũ cần chuyển sang cơ chế Android 13+.
- Sau khi khởi tạo mặc định, mọi lựa chọn của người dùng đều được tôn trọng. Chọn Theo hệ thống, mở lại app hoặc khởi động lại máy không được khiến app ép về tiếng Việt.
- Tiếng Việt xuất hiện dưới tên “Tiếng Việt” trong danh sách ngôn ngữ của app và danh sách ngôn ngữ ứng dụng do Android cung cấp.
- Nút đổi ngôn ngữ có nhãn tiếng Việt trong chu kỳ nhãn hiện có; không thay cơ chế hoạt ảnh.
- Các trang cài đặt và hộp thoại do Android/HyperOS sở hữu dùng ngôn ngữ mà hệ điều hành quyết định; bản dịch chỉ bao phủ nội dung do FCM Guard sở hữu.

## Công nghệ

- Ứng dụng Android native, Java 17 và XML resources, không có thư viện giao diện bổ sung.
- Android Gradle Plugin 8.6.1; Gradle 8.7 theo CI hiện tại.
- `compileSdk 35`, `minSdk 21`, `targetSdk 22`.
- Android 13+ dùng `LocaleManager`; Android cũ dùng `SharedPreferences` và context đã áp dụng locale qua `LocaleHelper`.

## Lệnh kiểm tra

Chạy từ thư mục gốc repository trong PowerShell:

```powershell
# Kiểm tra bố cục hiện có
python tools/check_layout_profiles.py

# Kiểm tra tài nguyên ngôn ngữ; script nhỏ sẽ được bổ sung khi triển khai
python tools/check_locales.py

# Biên dịch tài nguyên và APK
gradle :app:assembleDebug --stacktrace

# Android Lint
gradle :app:lintDebug --stacktrace

# Kiểm tra whitespace của diff
git diff --check
```

Repository chưa có Gradle wrapper. Đã build bằng Gradle 8.7 trong `%TEMP%/fcmguard-tools/`, JDK 17 và Android SDK 35/ADB có sẵn ngoài PATH; khóa debug cục bộ dùng đúng cấu hình hiện tại. Đã chạy emulator API 26/35. Khi tái lập cần đặt PATH hoặc gọi công cụ bằng đường dẫn đầy đủ. Không ghi nhận kiểm tra chưa chạy là đã đạt.

## Cấu trúc và phạm vi ảnh hưởng

| Vị trí | Vai trò trong thay đổi |
| --- | --- |
| `app/src/main/res/values/strings.xml`, `feature_strings.xml` | Nguồn đối chiếu đủ khóa, chuỗi kỹ thuật và placeholder |
| `app/src/main/res/values-vi/` | Bản dịch tiếng Việt đầy đủ |
| `app/src/main/res/values/arrays.xml` | Thêm lựa chọn Tiếng Việt đúng thứ tự mã ngôn ngữ |
| `app/src/main/res/xml/locales_config.xml` | Khai báo locale `vi` cho Android |
| `app/src/main/java/com/reed/fcmguard/LocaleHelper.java` | Chuẩn hóa `vi`/`vi-VN`, mặc định một lần, giữ lựa chọn và migration cũ |
| `app/src/main/java/com/reed/fcmguard/MainActivity.java` | Khởi tạo locale, danh sách chọn và nhãn nút đổi ngôn ngữ |
| `app/src/main/res/layout/activity_main.xml` | Đối chiếu nhãn ngôn ngữ đang ghi trực tiếp trong layout |
| `app/src/main/java/com/reed/fcmguard/GuardService.java` | Bảo đảm nội dung thông báo dùng locale mặc định/lựa chọn của app, kể cả khi chạy sau boot |
| `app/src/main/java/com/reed/fcmguard/BootReceiver.java` | Kiểm tra đường khởi động service; chỉ sửa nếu cần để áp dụng locale |
| `tools/check_locales.py` | Kiểm tra tài nguyên bằng Python standard library theo kiểu script assert hiện có |
| `README.md`, `README.zh-CN.md` | Cập nhật thành 11 ngôn ngữ và mô tả mặc định tiếng Việt |

Không đổi API công khai, thuật toán sửa whitelist, cấu hình bảo vệ, quyền hệ thống hay dữ liệu người dùng ngoài trạng thái ngôn ngữ cần thiết.

## Quy ước mã và bản dịch

Giữ tên resource hiện có, định dạng XML bốn dấu cách và chuỗi UTF-8 có dấu. Không đổi ID chuỗi để thêm bản dịch. Ví dụ:

```xml
<string name="repair_now">Sửa ngay</string>
<string name="write_failed">Không thể ghi: %1$s</string>
```

Giữ nguyên chỉ số và kiểu của `%1$s`, `%1$d`, `%2$d`... và escape đúng cú pháp Android XML. Tên ngôn ngữ trong danh sách giữ cách viết bản ngữ. Không thêm dependency hoặc tạo framework bản địa hóa mới.

## Chiến lược kiểm thử

- Tài nguyên: kiểm tra XML hợp lệ, không trùng khóa, bản Việt đủ toàn bộ khóa nguồn, placeholder tương ứng và các chuỗi kỹ thuật không đổi. Kiểm tra locale `vi` và sự khớp giữa danh sách hiển thị với mã ngôn ngữ.
- Hành vi: kiểm tra mặc định một lần và lưu lựa chọn trên cả Android dưới 13 và Android 13+. Nếu thêm nhánh logic mới, có kiểm tra nhỏ thực thi logic đó; kiểm tra văn bản mã nguồn không thay thế kiểm tra hành vi.
- Ma trận thủ công: cài mới trên máy tiếng Anh và tiếng Trung; nâng cấp khi có ngôn ngữ riêng; nâng cấp từ mặc định hệ thống; chọn `vi`/English/Theo hệ thống; mở lại và khởi động lại. Kiểm tra migration cũ, giao diện chính, hộp chọn, thông báo nền và nội dung có placeholder.
- Bố cục: chạy kiểm tra hình học hiện có cho 320–480dp; trên thiết bị/emulator kiểm tra chữ Việt có dấu và nút chứa nhãn dài. Kiểm tra hình học không chứng minh chữ không bị cắt.
- Build và Lint phải chạy được khi toolchain đầy đủ. Báo riêng những phần bị chặn bởi môi trường. Không đặt thêm ngưỡng coverage toàn dự án cho tính năng này.

## Ranh giới

- Luôn: tái sử dụng `LocaleHelper`, giữ 10 bản dịch cũ, bảo toàn lựa chọn cụ thể và cài đặt bảo vệ, kiểm tra placeholder và diff trước khi bàn giao.
- Hỏi trước: thêm dependency; đổi SDK, signing hoặc CI; ghi đè lựa chọn ngôn ngữ cụ thể đã lưu; commit, push hoặc phát hành APK lên remote.
- Không làm: xóa dữ liệu app để đổi ngôn ngữ, dịch mã gói/khóa kỹ thuật, bỏ kiểm tra đang lỗi, thay cơ chế FCM hoặc thêm quyền mới.

## Tiêu chí hoàn thành

1. Có bản dịch Việt đầy đủ, không thiếu khóa hay sai placeholder so với nguồn.
2. Cài mới hiển thị tiếng Việt dù ngôn ngữ hệ thống là Anh hoặc Trung.
3. Bản cũ chưa chọn ngôn ngữ cụ thể chuyển sang tiếng Việt một lần; bản cũ đã chọn riêng ngôn ngữ vẫn giữ lựa chọn, kể cả qua migration.
4. Chọn ngôn ngữ khác hoặc Theo hệ thống vẫn hoạt động sau khi mở lại và khởi động lại; không bị reset về Việt.
5. Android nhận diện `vi`, `vi-VN` dùng bản dịch Việt, và danh sách chọn không lệch mã với nhãn.
6. Nội dung do app tạo, gồm thông báo bảo vệ mặc định, có tiếng Việt; không làm hỏng tên kỹ thuật hoặc nội dung định dạng.
7. Kiểm tra tài nguyên, kiểm tra logic mới, kiểm tra bố cục, build và Lint đạt; phần chưa thể chạy được ghi rõ cùng lý do.
8. README mô tả đúng 11 ngôn ngữ và chính sách mặc định.

## Trạng thái quy trình

Người dùng đã yêu cầu chuyển sang lập kế hoạch, sau đó triển khai bằng skill `incremental-implementation`. Công việc 1–5 đã triển khai và kiểm tra trong phạm vi môi trường có sẵn. Công việc 6 ghi riêng các kiểm tra đạt, lỗi Lint nền và phần chưa xác minh; không coi những phần còn thiếu là đã đạt. Không commit, push hoặc phát hành lên remote theo phạm vi kế hoạch.
