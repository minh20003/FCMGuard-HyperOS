# Kế hoạch: Thêm tiếng Việt và dùng mặc định

Trạng thái: Đã triển khai theo yêu cầu dùng skill `incremental-implementation`. Công việc 1–5 đã thực hiện; công việc 6 còn các kiểm tra chưa hoàn tất được ghi trong `todo.md`.

## Tổng quan

Thực hiện [đặc tả tiếng Việt](../SPEC-vietnamese.md): dịch đủ 75 chuỗi, thêm lựa chọn Tiếng Việt trên mọi phiên bản Android app hỗ trợ và chọn tiếng Việt một lần khi chưa có lựa chọn riêng. Giữ các ngôn ngữ cũ, lựa chọn người dùng và cơ chế bảo vệ FCM.

Không có kế hoạch cũ hoặc tracker được chỉ định trong repository. Checklist duy nhất nằm ở [todo.md](todo.md); tài liệu này chỉ ghi quyết định, thứ tự và rủi ro.

## Quyết định kỹ thuật

1. Thêm `values-vi/strings.xml` và `values-vi/feature_strings.xml`. Giữ tài nguyên tiếng Anh trong `values/` làm fallback; không cần sao chép sang `values-en/` hay sửa 10 bản dịch cũ. Mặc định tiếng Việt được thiết lập bằng locale của app, không chỉ bằng thứ tự trong danh sách.
2. Giữ khai báo locale thủ công hiện có; thêm `vi` vào `locales_config.xml`. Android hỗ trợ khai báo này qua `android:localeConfig`, còn việc đổi ngôn ngữ dùng `LocaleManager` từ Android 13. Không bật thêm cơ chế sinh locale config của Gradle. [Tài liệu Android](https://developer.android.com/guide/topics/resources/app-languages#use-localeconfig).
3. Đặt mọi quyết định chọn mặc định và chuyển lựa chọn cũ trong `LocaleHelper`. Thêm một cờ khởi tạo mặc định riêng, độc lập với `native_locale_migrated`; cờ migration cũ không thể cho biết bản này đã áp dụng mặc định Việt hay chưa.
4. Thứ tự Android 13+: ưu tiên lựa chọn native đã có → chuyển lựa chọn legacy cụ thể nếu native đang trống → chọn `vi` nếu chưa khởi tạo mặc định và chưa có lựa chọn cụ thể. Android cũ: giữ lựa chọn cụ thể trong preferences; nếu chưa khởi tạo và giá trị đang trống hoặc `system`, lưu `vi`. Khi người dùng chọn `system` sau đó, không đặt lại mặc định.
5. Chỉ hoàn tất cờ/loại bỏ dữ liệu migration sau khi đã giữ hoặc chuyển lựa chọn thành công. Nếu `LocaleManager` không lấy được, giữ dữ liệu và cho phép thử lại. Giữ đường nâng cấp từ Android cũ lên Android 13+ dù cờ mặc định Việt đã tồn tại.
6. Khởi tạo trước khi lấy tài nguyên giao diện hoặc tạo thông báo đầu tiên. Không gọi `setApplicationLocales` mỗi lần mở app, không tự `recreate()` vô điều kiện. Android áp dụng locale mới qua configuration change. [API LocaleManager](https://developer.android.com/reference/android/app/LocaleManager#setApplicationLocales(android.os.LocaleList)).
7. `GuardService` dùng locale hiện tại khi lấy chuỗi tạo/cập nhật thông báo và thông báo lỗi. Không giữ một context đã dịch từ lần chạy trước rồi dùng mãi sau khi đổi ngôn ngữ. Giữ channel ID, quyền, timer và reconnect hiện có. Dự kiến không cần sửa `BootReceiver` vì service tự xử lý cả đường khởi động sau boot.
8. Dùng Python standard library cho kiểm tra tài nguyên. Với logic locale, thêm một assert-based self-check trong APK kiểm thử, dùng trực tiếp Android `Instrumentation` để gọi mã thật; không thêm JUnit, Robolectric hoặc AndroidX. Runner chỉ nằm trong `src/androidTest/`, được khai báo bằng `testInstrumentationRunner` trong `app/build.gradle`. Self-check trả kết quả đạt/lỗi rõ qua `finish`, không phụ thuộc Java `assert` vốn có thể bị tắt. [API Instrumentation](https://developer.android.com/reference/android/app/Instrumentation).

## Thứ tự và phụ thuộc

```text
1. Bản Việt chọn được qua cài đặt Android 13+
   → 2. Bản Việt chọn được qua hộp chọn trong app
      → checkpoint A: tài nguyên và danh sách chọn
         → 3. Mặc định Việt một lần, bảo toàn lựa chọn/migration
            → 4. Thông báo nền dùng locale của app
               → checkpoint B: hành vi mặc định và service
                  → 5. Tài liệu 11 ngôn ngữ
                     → 6. Xác minh cuối và ghi bằng chứng
```

Đặt phần migration ngay sau khi có đủ tài nguyên và hai đường chọn ngôn ngữ để kiểm tra rủi ro trước phần tài liệu. Mỗi công việc dự kiến chạm tối đa năm tệp; tiêu chí và lệnh xác minh cụ thể nằm trong `todo.md`.

## Kiểm tra và bằng chứng

- Kiểm tra tĩnh: đủ khóa, không trùng khóa, placeholder đúng chỉ số/kiểu, chuỗi kỹ thuật giữ nguyên, locale đã đăng ký và nhãn khớp mã. Không dùng kiểm tra nội dung Java thay cho kiểm tra hành vi.
- Self-check Android: cài mới, khởi tạo lặp lại, giữ ngôn ngữ riêng, chuyển legacy, ưu tiên native, chọn Theo hệ thống, chuẩn hóa `vi-VN` và không đánh dấu hoàn tất khi manager chưa có. Chạy trên một emulator API 21–32 và một emulator API 33+; nhánh manager không có được kiểm bằng context kiểm thử trả về null.
- Self-check chỉ lưu/khôi phục các khóa ngôn ngữ và locale mà nó thay đổi; không xóa toàn bộ `guard_state`, không thay trạng thái bảo vệ. Chạy trên thiết bị/emulator thử nghiệm. Không cần bật bảo vệ FCM cho self-check của helper.
- Thủ công: đối chiếu đủ tiêu chí đặc tả trên hệ thống tiếng Anh/Trung/Việt; kiểm tra chữ có dấu ở 320dp và 393dp, thay ngôn ngữ, mở lại, reboot, thông báo và màn hình hệ thống. Đổi locale hệ thống trong danh sách này là thao tác kiểm thử, không phải hành vi app sẽ tự thực hiện.
- Phân biệt ba mức: script tĩnh đạt; mã Android biên dịch được; hành vi Android/HyperOS đã được quan sát. Build hoặc kiểm tra hình học không thay thế kiểm tra chữ bị cắt hay hành vi thiết bị.
- Các lệnh và ma trận hoàn thành ở công việc 6. Chỉ đánh dấu đạt khi có kết quả thực tế; thiếu môi trường thì ghi chặn và giữ mục tương ứng chưa hoàn thành.

## Rủi ro và cách xử lý

| Rủi ro | Tác động | Cách xử lý |
| --- | --- | --- |
| Cờ khởi tạo ghi quá sớm hoặc tái áp dụng `vi` liên tục | Mất lựa chọn cũ hoặc không dùng được Theo hệ thống | Cờ riêng; ưu tiên locale hiện có; kiểm tra gọi lặp lại và lỗi manager |
| Android 13+ đổi configuration trong lần mở đầu | Giao diện nháy ngôn ngữ cũ hoặc vòng lặp recreate | Khởi tạo trước inflate; kiểm tra vòng đời thực tế; không recreate vô điều kiện |
| Service chạy sau boot hoặc đang chạy khi đổi ngôn ngữ | Thông báo vẫn dùng tiếng Anh/ngôn ngữ cũ | Áp dụng locale trước lấy chuỗi; kiểm tra cả cập nhật thông báo và đường boot |
| Chuỗi Việt dài, sai placeholder hoặc mất dấu | Chữ bị cắt hoặc lỗi định dạng | Kiểm tra XML/placeholder, Android resource compiler, kiểm tra màn hình hẹp |
| Chưa có Gradle/ADB trên PATH; SDK/khóa ký chưa được xác nhận | Chưa build hoặc kiểm thử runtime được | Tìm toolchain có sẵn trước; bổ sung công cụ cục bộ khi cần trong bước triển khai; không đổi SDK/signing/CI của dự án để né lỗi |
| APK thử nghiệm khác chữ ký APK đang cài | Không thể cập nhật tại chỗ để kiểm tra migration | Dùng emulator với bản cũ và bản mới ký cùng khóa thử nghiệm; không gỡ app hay xóa dữ liệu trên máy người dùng |

## Phạm vi và điều kiện duyệt

Không thêm dependency hoặc quyền, không đổi SDK/signing/CI, không refactor FCM, không commit/push/phát hành trong kế hoạch này. Thay đổi `app/build.gradle` chỉ nhằm chỉ định runner của APK kiểm thử; không thay cấu hình APK chính. Không chạy agent song song: các phần này dùng chung `LocaleHelper` và `MainActivity`, làm tuần tự giảm xung đột.

Kế hoạch và toàn bộ checklist được trình duyệt cùng nhau. Các checkpoint là điểm kiểm tra kỹ thuật và cập nhật tiến độ; chỉ xin quyết định mới nếu phải vượt phạm vi đã duyệt hoặc phát hiện yêu cầu mâu thuẫn.

Không còn câu hỏi chức năng mở. Đã xác nhận toolchain cục bộ và emulator API 26/35; APK thử nghiệm dùng khóa ký cục bộ. Những điểm còn thiếu là máy HyperOS thật, thử nâng cấp giữa APK cũ/mới cùng chữ ký và nâng cấp OS thực tế; Lint nền vẫn có 9 lỗi. Bằng chứng cụ thể nằm trong `todo.md`.
