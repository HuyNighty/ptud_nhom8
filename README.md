> **M1 — New project foundation (Spring Boot 4.1.1 + Java 25).** The guide below is legacy
> documentation for the old Swing/JDBC prototype; its build/database instructions are not
> valid for the new skeleton. See [M1 setup and verification](docs/M1_CONFIGURATION.md).
>
> Business modules and database access have **not** been migrated yet.

---

# N08 — Quản lý phòng khám nha khoa (Giai đoạn 2)

Ứng dụng desktop **Java Swing + JDBC + Microsoft SQL Server**, xây dựng từ prototype Admin v0.3 đã duyệt
và class diagram mới nhất. Không dùng JavaFX, Spring Boot, Hibernate/JPA hay framework giao diện nào khác.
Maven chỉ dùng để build và tải driver JDBC.

| Hạng mục | Nội dung |
|---|---|
| Ngôn ngữ | Java 17 (biên dịch `--release 17`, chạy được trên JDK 17–21) |
| Giao diện | Java Swing (Metal L&F + component tự vẽ theo token màu của prototype), CardLayout |
| Truy cập dữ liệu | JDBC thuần, `PreparedStatement`, transaction tự quản (`Tx`), không ORM |
| CSDL | Microsoft SQL Server 2019 trở lên (`sql/01` → `sql/04`) |
| Thư viện chạy | Duy nhất `mssql-jdbc 12.8.1.jre11` (driver chính thức của Microsoft) |

## 1. Cấu trúc mã nguồn

```
sql/                       01_tao_csdl · 02_tao_bang · 03_du_lieu_mau · 04_kiem_tra (view đối soát, tùy chọn)
config/db.properties.example   mẫu cấu hình kết nối (người dùng tự nhập thông tin máy mình)
src/main/java/vn/n08/nhakhoa/
  App.java                 điểm khởi động (+ tham số --kiem-tra-ket-noi)
  config/                  DbConfig (đọc file cấu hình), Database (mở Connection, ping)
  model/                   35 entity (record) khớp bảng + 20 enum trạng thái/vai trò có nhãn tiếng Việt
  dto/                     dữ liệu hiển thị ghép bảng (…View), dữ liệu nhập (…Form), tổng hợp (DashboardData…)
  dao/                     24 lớp JDBC — mỗi hàm nhận Connection của transaction đang chạy
  service/                 19 lớp Service + Audit (nhật ký) + BacSiPhuTrach: kiểm tra quyền, trạng thái, không tự duyệt, transaction, nhật ký
  controller/              Navigation (menu theo quyền), AppController (vòng đời cửa sổ), Async (SwingWorker), ErrorHandler
  view/component/          thành phần dùng chung: DataTable, FormDialog, FormGrid, Dialogs, Badge, Btn, Toast, Timeline…
  view/screen/             LoginFrame, MainFrame và 21 màn hình (Page)
  view/dialog/             33 hộp thoại nghiệp vụ
  util/                    Jdbc, Tx, Session, Quyen, PasswordHasher, Validator, Fmt, Now, AppLog, …Exception
src/test/java/…            KiemThuDao, KiemThuTichHop, KiemThuGiaoDien (chạy bằng main, không cần JUnit)
src/test/resources/derby/  schema + seed dịch tự động từ sql/02, sql/03 — CHỈ cho kiểm thử
tools/gensql.py            sinh lại sql/03 và dữ liệu Derby từ một nguồn (Python 3)
docs/                      THIET_KE_DU_LIEU.md, KIEM_THU.md, anh-giao-dien/
scripts/                   build/run/test cho Windows (.bat) và Linux/macOS (.sh) khi không dùng Maven
```

Luồng một thao tác: **Màn hình (view)** đọc ô nhập → **Async** chạy nền → **Service** kiểm tra quyền +
quy tắc + mở transaction → **DAO** chạy `PreparedStatement` → commit / rollback → kết quả hoặc thông báo lỗi
tiếng Việt trở lại màn hình. Quyền được kiểm tra ở Service (không chỉ ẩn nút), nên gọi Service trực tiếp
cũng bị chặn.

## 2. Cài đặt cơ sở dữ liệu

Yêu cầu: SQL Server 2019+ (Express/Developer đều được), bật **TCP/IP** (SQL Server Configuration Manager →
Protocols → TCP/IP = Enabled, cổng 1433) và **SQL Server and Windows Authentication mode** nếu dùng tài khoản SQL.

Chạy lần lượt trong SSMS (mở file → Execute) hoặc bằng `sqlcmd`:

```bat
sqlcmd -S localhost -E -i sql\01_tao_csdl.sql
sqlcmd -S localhost -E -i sql\02_tao_bang.sql
sqlcmd -S localhost -E -i sql\03_du_lieu_mau.sql
sqlcmd -S localhost -E -i sql\04_kiem_tra.sql        & rem tùy chọn: view đối soát + truy vấn kiểm tra (mong đợi 0 dòng)
```

(`-E` = Windows Authentication; dùng `-U <user> -P <mật khẩu>` nếu đăng nhập bằng tài khoản SQL.)

- `01` **xóa và tạo lại** database `N08_NhaKhoa` (collation `Vietnamese_CI_AS`). Có đoạn tạo login riêng
  `n08_app` đang để trong chú thích — tự đặt mật khẩu nếu dùng.
- `03` dùng ngày tương đối theo ngày chạy script (`@homNay`) để màn hình "hôm nay" luôn có dữ liệu minh họa.
  Chạy lại `01 → 02 → 03` để làm mới dữ liệu mẫu.

## 3. Cấu hình kết nối (không có mật khẩu nào ghi trong mã nguồn)

1. Sao chép `config/db.properties.example` thành `config/db.properties`.
2. Sửa `db.url`, `db.user`, `db.password` theo máy của người dùng (hoặc dùng Windows Authentication như hướng dẫn
   trong file mẫu). File `config/db.properties` đã được loại khỏi Git bằng `.gitignore`.
3. Kiểm tra kết nối trước khi mở giao diện:

```bat
java -jar target\n08-nhakhoa.jar --kiem-tra-ket-noi
```

Kết quả `KẾT NỐI THÀNH CÔNG: Microsoft SQL Server …` hoặc thông báo lỗi cụ thể (thiếu driver, sai đăng nhập,
không tới được máy chủ). Có thể trỏ file cấu hình khác: `java -Ddb.config=D:\cauhinh\db.properties -jar …`.

## 4. Build và chạy

**Cách 1 — Maven (khuyên dùng):**

```bat
mvn package
java -jar target\n08-nhakhoa.jar
```

**Cách 2 — chỉ JDK** (đặt `mssql-jdbc-12.8.1.jre11.jar` vào `lib/`, xem `lib/README.md`):

```bat
scripts\build.bat
scripts\run.bat
```

Trong IntelliJ IDEA / NetBeans: mở thư mục như một dự án Maven, chạy lớp `vn.n08.nhakhoa.App`
(thư mục làm việc = thư mục gốc dự án để tìm thấy `config/db.properties`).

## 5. Tài khoản mẫu

| Tên đăng nhập | Mật khẩu | Vai trò | Ghi chú |
|---|---|---|---|
| `admin` | `Admin@123` | Quản trị (mọi chức năng của 5 tác nhân) | thao tác lâm sàng phải chọn bác sĩ chịu trách nhiệm |
| `lethian`, `nguyenmai` | `Nhakhoa@123` | Lễ tân | |
| `tranminh`, `phamanh`, `nguyenha`, `lethu` | `Nhakhoa@123` | Bác sĩ | |
| `dohanh`, `vophuc`, `lytram` | `Nhakhoa@123` | Y tá | |
| `minhtam` | `Nhakhoa@123` | QL phòng khám | người duyệt khác admin |
| `quockhanh` | `Nhakhoa@123` | QL kho | |
| `tranvanan` | `Nhakhoa@123` | Lễ tân | **Bị khóa** (sai mật khẩu 5 lần) — thử mở khóa |
| `dangluc` | `Nhakhoa@123` | Y tá | **Vô hiệu** (đã nghỉ việc) — không đăng nhập được |

Đổi mật khẩu các tài khoản mẫu sau khi cài đặt (nút **Đổi mật khẩu** trên thanh trên cùng).

## 6. Kịch bản dùng thử nhanh (với dữ liệu mẫu)

1. `lethian`: **Tiếp nhận** → check-in `LH000004` (Phạm Quốc Bảo, hôm nay) → phiếu khám mới.
2. `admin`: **Phiếu khám** → chọn phiếu → *Bắt đầu khám* (bắt buộc chọn bác sĩ) → chỉ định CLS → nhập kết quả →
   ghi chẩn đoán/kết luận → kê đơn (thuốc KSĐB/dị ứng sẽ hỏi xác nhận) → *Kết luận*. Xem **Nhật ký**: tài khoản
   `admin`, người chịu trách nhiệm là bác sĩ đã chọn.
3. `lethian`: **Hóa đơn** → tab *Chờ lập hóa đơn* → lập → thu tiền mặt một phần (hiện tiền thối) → thu chuyển
   khoản phần còn lại (bắt buộc mã giao dịch) → gửi yêu cầu hủy hóa đơn khác.
4. `admin`: **Duyệt hủy** → yêu cầu `YC000004` do chính admin tạo: nút bị khóa và Service cũng chặn;
   đăng nhập `minhtam` để duyệt.
5. `minhtam`: **Nhân viên** → *Thủ tục nghỉ việc* cho BS Lê Thu: hệ thống liệt kê phân công / lịch hẹn còn mở,
   xử lý từng việc (thay người, đổi bác sĩ, hủy có lý do) rồi mới *Hoàn tất* → tài khoản `lethu` thành Vô hiệu.
6. `quockhanh`: **Nhập / xuất** → xuất theo FEFO, xuất thuốc KSĐB cần người nhận + bác sĩ chịu trách nhiệm;
   **Kiểm kê & hao hụt** → lập phiếu; `minhtam`/`admin` duyệt (người lập không tự duyệt).

## 7. Kiểm thử — đã chạy gì, chưa chạy gì

Môi trường phát triển của bản giao này **không có SQL Server**, nên:

| Loại | Trạng thái | Cách chạy lại |
|---|---|---|
| Biên dịch `javac --release 17` toàn bộ mã | **Đã chạy, 0 lỗi** | `scripts/build.*` |
| Tích hợp Service + DAO + JDBC (248 kiểm tra) | **Đã chạy trên Apache Derby (CSDL thay thế), 248 đạt / 0 lỗi** | `scripts/test.*` hoặc Maven (mục dưới) |
| Khói giao diện Swing: 6 vai trò, 21 màn hình, 19 hộp thoại, chụp ảnh | **Đã chạy dưới Xvfb trên Derby, không ngoại lệ** | `scripts/test.* --giao-dien` |
| Cú pháp T-SQL: 4 script (511 câu) + 303 câu SQL khác nhau mà DAO đã thực thi | **Đã phân tích tĩnh bằng sqlglot (dialect tsql), 0 lỗi** | — |
| Chạy script trên SQL Server thật, kết nối `mssql-jdbc`, `mvn package` | **CHƯA chạy** (không có SQL Server/Maven Central trong môi trường) | theo checklist bên dưới |

Kết quả trên Derby chứng minh logic Java, câu SQL và transaction chạy được qua JDBC, **không thay thế**
kiểm thử trên SQL Server (chỉ mục lọc `UX_*`, `NVARCHAR`, `DATETIME2`, collation chỉ có ở SQL Server).
Chi tiết: `docs/KIEM_THU.md`.

Chạy kiểm thử bằng Maven (tự tải Derby, scope test):

```bat
mvn test-compile exec:java -Dexec.mainClass=vn.n08.nhakhoa.KiemThuTichHop -Dexec.classpathScope=test
```

**Checklist kiểm thử trên SQL Server thật (người dùng thực hiện):**

1. Chạy `01 → 02 → 03`, không lỗi; chạy `04_kiem_tra.sql` — các truy vấn kiểm tra trả về 0 dòng.
2. `java -jar target\n08-nhakhoa.jar --kiem-tra-ket-noi` → "KẾT NỐI THÀNH CÔNG".
3. Đăng nhập lần lượt các tài khoản mẫu; `tranvanan` báo bị khóa, `dangluc` báo vô hiệu; nhập sai 5 lần một tài khoản → tự khóa.
4. Làm kịch bản mục 6; sau mỗi bước chạy lại `04_kiem_tra.sql` (0 dòng lệch tồn / sai trạng thái hóa đơn / tự duyệt).
5. Thử tạo 2 lịch hẹn cùng bác sĩ cùng giờ, 2 hóa đơn cho cùng lượt khám → bị chặn (Service và chỉ mục lọc `UX_*`).
6. Gõ tiếng Việt có dấu ở mọi ô, kiểm tra hiển thị lại đúng (NVARCHAR + collation `Vietnamese_CI_AS`).

## 8. Quy tắc nghiệp vụ chính đã cài

- **Không tự duyệt / tự xác nhận**: yêu cầu hủy hóa đơn, phiếu kiểm kê/hao hụt, biên bản bàn giao — chặn ở
  Service, ở điều kiện `UPDATE … AND maTaiKhoanTao <> ?` của DAO và ở `CHECK` của CSDL.
- **Admin không mạo danh**: nhật ký luôn ghi tài khoản đăng nhập; thao tác lâm sàng bắt buộc chọn bác sĩ chịu
  trách nhiệm (`maNguoiChiuTrachNhiem`); tài khoản bác sĩ chỉ ghi dưới tên mình.
- **Thanh toán**: mỗi lần thu một phương thức (Tiền mặt / Chuyển khoản / Thẻ POS), mã giao dịch bắt buộc với CK/POS,
  tiền thối tính khi tiền mặt, không thu vượt còn nợ, hủy lần thu giữ lịch sử; không cổng thanh toán trực tuyến.
  Tổng tiền / đã thu / còn nợ **không lưu cột**, tính từ chi tiết và các lần thu.
- **BI_KHOA** (khóa tạm, mở lại được) khác **VO_HIEU** (vĩnh viễn, không xóa để giữ lịch sử).
- **Nghỉ việc có điều kiện**: phải xử lý hết phân công, lịch hẹn, phiếu khám, bàn giao còn mở; không tự hủy/xóa;
  hoàn tất → nhân viên `NGHI_VIEC`, tài khoản `VO_HIEU`.
- **Hồ sơ bệnh nhân**: lễ tân tạo chính thức trực tiếp, cảnh báo nghi trùng (SĐT hoặc họ tên + ngày sinh), không xóa.
- **Kho**: tồn chỉ đổi trong transaction cùng phiếu; xuất FEFO, chặn lô hết hạn/thiếu tồn; hoàn trả chỉ hàng
  nguyên seal; kiểm kê/hao hụt chờ người khác duyệt và kiểm tra tồn chưa đổi từ lúc lập phiếu.
- **Hộp cấp cứu**: danh mục cấu hình được; 6 mục cài sẵn là **dữ liệu mẫu để chạy thử, không phải danh mục chuyên
  môn chính thức**; ngưỡng hạn dùng và chu kỳ kiểm tra là tham số cấu hình (giả định).

Các giả định khác và bảng ánh xạ class diagram ↔ bảng ↔ Service: `docs/THIET_KE_DU_LIEU.md`.

## 9. Nhật ký kỹ thuật

Lỗi ngoài dự kiến được ghi vào `logs/n08-nhakhoa.log` (không ghi mật khẩu). Nhật ký nghiệp vụ nằm trong bảng
`NhatKyThaoTac`, xem ở màn hình **Nhật ký thao tác**.
