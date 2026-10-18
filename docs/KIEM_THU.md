# Báo cáo kiểm thử — Giai đoạn 2

> **Phạm vi trung thực của kết quả.** Môi trường phát triển bản giao này **không có Microsoft SQL Server** và không
> tải được thư viện từ Maven Central. Vì vậy các kiểm thử chạy được thực hiện trên **Apache Derby 10.14 nhúng trong
> bộ nhớ — CSDL THAY THẾ**, với schema/seed dịch tự động từ `sql/02` và `sql/03`. Kết quả dưới đây là bằng chứng
> cho **logic Java, câu SQL của DAO, transaction và giao diện Swing**, **không phải** bằng chứng chạy trên SQL Server.
> Không dùng kết quả kiểm thử prototype HTML làm bằng chứng.

## 1. Tổng hợp

| # | Loại kiểm thử | Công cụ / môi trường | Kết quả | Ngày chạy |
|---|---|---|---|---|
| 1 | Biên dịch toàn bộ mã (main + test) | `javac --release 17` (OpenJDK 21) | **0 lỗi** | 12/10/2026 |
| 2 | Truy vấn đọc của mọi DAO | KiemThuDao trên Derby | **32/32 đạt** (nằm trong #3) | 12/10/2026 |
| 3 | Tích hợp Service + DAO + JDBC + transaction | KiemThuTichHop trên Derby | **248 đạt / 0 lỗi** | 12/10/2026 |
| 4 | Khói giao diện Swing (6 vai trò, 21 màn hình, 19 hộp thoại) | KiemThuGiaoDien dưới Xvfb, Derby | **Không ngoại lệ trên luồng giao diện**; 46 ảnh | 12/10/2026 |
| 5 | Cú pháp T-SQL của 4 script | sqlglot 30.22 (dialect `tsql`), phân tích tĩnh | **511 câu, 0 lỗi** (2 câu `ALTER DATABASE` được nhận là lệnh quản trị) | 12/10/2026 |
| 6 | Cú pháp T-SQL của câu SQL DAO đã thực thi | bật `-Dn08.sqltrace`, phân tích 303 câu khác nhau bằng sqlglot `tsql` | **303/303 phân tích được** | 12/10/2026 |
| 7 | Script build/test không Maven | `scripts/build.sh`, `scripts/test.sh` | **Chạy được** | 12/10/2026 |
| 8 | Chạy script trên SQL Server thật | — | **CHƯA CHẠY** | — |
| 9 | Kết nối `mssql-jdbc` tới SQL Server | — | **CHƯA CHẠY** (đã kiểm tra thông báo khi thiếu driver / thiếu file cấu hình) | — |
| 10 | `mvn package` | — | **CHƯA CHẠY** (không truy cập được Maven Central) | — |

Những điểm chỉ kiểm được trên SQL Server: chỉ mục lọc `UX_*` (Derby không hỗ trợ — Service đã kiểm tra cùng điều kiện),
`NVARCHAR` + collation `Vietnamese_CI_AS`, `DATETIME2(0)`, `READ_COMMITTED_SNAPSHOT`, Windows Authentication,
`DATEADD/@homNay` trong `03_du_lieu_mau.sql`. Checklist cho người dùng: `README.md` mục 7.

## 2. Cách chạy lại

```bash
# Linux/macOS (cần lib/test/derby-10.14.2.0.jar)
scripts/test.sh                 # tích hợp
xvfb-run -a scripts/test.sh --giao-dien   # + giao diện (máy có màn hình thì bỏ xvfb-run)
# Windows
scripts\test.bat
# Maven (tự tải Derby, scope test)
mvn test-compile exec:java -Dexec.mainClass=vn.n08.nhakhoa.KiemThuTichHop -Dexec.classpathScope=test
```

Bộ kiểm thử tạo CSDL trong bộ nhớ mới mỗi lần chạy, ngày mẫu tính theo ngày chạy, không đụng tới SQL Server.

## 3. Nội dung kiểm thử tích hợp (nhật ký chạy thật, 12/10/2026)

Các dòng `→ "…"` là thông điệp tiếng Việt mà Service trả về khi chặn thao tác — chính là thông điệp người dùng thấy
trên giao diện. Nhóm "không tự duyệt" kiểm ở 3 lớp: Service, câu `UPDATE` của DAO (0 dòng) và `CHECK` của CSDL.

```text
== DAO — truy vấn đọc trên Derby (CSDL thay thế)
  [ĐẠT] khởi tạo DAO
  [ĐẠT] ThamSoDao.getInt SO_LAN_SAI_TOI_DA = 5
  [ĐẠT] NhanVienDao.search: 14 nhân viên mẫu
  [ĐẠT] NhanVienDao.active(BS): 4 bác sĩ
  [ĐẠT] TaiKhoanDao.search 'an'
  [ĐẠT] TaiKhoanDao.findByUsername admin
  [ĐẠT] CaLamViecDao.all: 3 ca
  [ĐẠT] PhanCongDao.range tuần
  [ĐẠT] BanGiaoDao.search
  [ĐẠT] DanhGiaDao.byKy
  [ĐẠT] BenhNhanDao.search theo SĐT
  [ĐẠT] BenhNhanDao.search theo tên
  [ĐẠT] BenhNhanDao.findView (công nợ, lần khám cuối)
  [ĐẠT] LichHenDao.search
  [ĐẠT] PhieuKhamDao.search
  [ĐẠT] PhieuKhamDao.chiDinhOf
  [ĐẠT] KeHoachDao.search (tiến độ, chi phí)
  [ĐẠT] KeHoachDao.items
  [ĐẠT] DichVuDao.all
  [ĐẠT] HoaDonDao.search (tổng, đã trả)
  [ĐẠT] HoaDonDao.lines
  [ĐẠT] YeuCauHuyDao.search
  [ĐẠT] HangHoaDao.tonKho: 5 thuốc + 4 vật tư
  [ĐẠT] HangHoaDao.tonKho có từ khóa
  [ĐẠT] LoHangDao.inStock
  [ĐẠT] NhaCungCapDao.search
  [ĐẠT] PhieuKhoDao.list
  [ĐẠT] DieuChinhDao.search
  [ĐẠT] CapCuuDao.listWithLink: 6 mục mẫu
  [ĐẠT] CapCuuDao.history
  [ĐẠT] NhatKyDao.search
  [ĐẠT] toàn bộ truy vấn đọc chạy không lỗi SQL

== UC38 Đăng nhập
  [ĐẠT] admin đăng nhập đúng mật khẩu
  [ĐẠT] phiên = admin / NV0000
  [ĐẠT] tài khoản BI_KHOA không đăng nhập được → "Tài khoản đang bị khóa (Tự động khóa do nhập sai mật khẩu 5 lần liên tiếp). Liên hệ quản lý phòng khám để mở khóa."
  [ĐẠT] tài khoản VO_HIEU không đăng nhập được → "Tài khoản đã bị vô hiệu hóa và không thể đăng nhập."
  [ĐẠT] sai mật khẩu lần 1 → "Tên đăng nhập hoặc mật khẩu không đúng. Còn 4 lần thử trước khi tài khoản bị khóa."
  [ĐẠT] sai mật khẩu lần 2 → "Tên đăng nhập hoặc mật khẩu không đúng. Còn 3 lần thử trước khi tài khoản bị khóa."
  [ĐẠT] sai mật khẩu lần 3 → "Tên đăng nhập hoặc mật khẩu không đúng. Còn 2 lần thử trước khi tài khoản bị khóa."
  [ĐẠT] sai mật khẩu lần 4 → "Tên đăng nhập hoặc mật khẩu không đúng. Còn 1 lần thử trước khi tài khoản bị khóa."
  [ĐẠT] sai lần 5 → tự khóa → "Sai mật khẩu 5 lần liên tiếp. Tài khoản đã bị khóa, liên hệ quản lý để mở khóa."
  [ĐẠT] nguyenha chuyển BI_KHOA
  [ĐẠT] đúng mật khẩu nhưng đã bị khóa vẫn bị chặn → "Tài khoản đang bị khóa (Tự động khóa sau 5 lần nhập sai mật khẩu). Liên hệ quản lý phòng khám để mở khóa."
  [ĐẠT] QL mở khóa nguyenha
  [ĐẠT] nguyenha đăng nhập lại được

== Phân quyền ở tầng Service (không chỉ ẩn nút)
  [ĐẠT] Lễ tân gọi TaiKhoanService.danhSach → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] Lễ tân gọi KhamService.batDauKham → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] Lễ tân duyệt yêu cầu hủy → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] Lễ tân duyệt điều chỉnh kho → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] QL kho thu tiền → "Tài khoản quockhanh (QL kho) không có quyền thực hiện thao tác này."
  [ĐẠT] QL kho (người lập) duyệt phiếu điều chỉnh → "Tài khoản quockhanh (QL kho) không có quyền thực hiện thao tác này."

== UC01/08/09/23 Hồ sơ bệnh nhân
  [ĐẠT] tìm SĐT 11 số bị báo sai định dạng → "Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0."
  [ĐẠT] tìm theo SĐT ra đúng 1 bệnh nhân
  [ĐẠT] cảnh báo nghi trùng: [BN000001 · Nguyễn Thị Lan · 12/05/1998 · 0909123456 (trùng họ tên + ngày sinh)]
  [ĐẠT] thiếu trường bắt buộc → "Nhập họ tên."
  [ĐẠT] tạo hồ sơ mới (SĐT có khoảng trắng được chuẩn hóa)
  [ĐẠT] SĐT lưu dạng chuẩn
  [ĐẠT] tạo kèm hồ sơ bệnh án
  [ĐẠT] lễ tân không cập nhật tiền sử/dị ứng → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] admin cập nhật tiền sử mà không chọn bác sĩ → "Chọn bác sĩ chịu trách nhiệm chuyên môn."
  [ĐẠT] admin cập nhật tiền sử, chọn BS NV0001
  [ĐẠT] nhật ký: tài khoản thật TK0001, người chịu trách nhiệm NV0001
  [ĐẠT] UC09 xem hồ sơ: có lượt khám

== UC03/06/07 Lịch hẹn
  [ĐẠT] tạo lịch hẹn ngày mai 13:30
  [ĐẠT] trùng khung giờ của bác sĩ → "Khung giờ đã được đặt: BS. Trần Minh đã có lịch lúc 13/10/2026 13:30."
  [ĐẠT] giờ không thuộc khung giờ → "Giờ hẹn phải là một khung giờ của phòng khám."
  [ĐẠT] giờ trong quá khứ → "Thời gian hẹn phải sau thời điểm hiện tại."
  [ĐẠT] khung 13:30 không còn trống
  [ĐẠT] hủy thiếu lý do → "Nhập lý do hủy."
  [ĐẠT] xác nhận lịch
  [ĐẠT] đổi lịch sang BS NV0002 14:00
  [ĐẠT] lịch cũ DA_DOI_LICH, lịch mới trỏ lịch gốc
  [ĐẠT] khung cũ được giải phóng
  [ĐẠT] chuỗi lịch sử đổi lịch có 2 bản ghi
  [ĐẠT] hủy lịch có lý do
  [ĐẠT] đánh dấu vắng khi chưa tới giờ → "Chưa đến giờ hẹn, chưa thể đánh dấu vắng mặt."

== UC02 Tiếp nhận · UC10/11/16 Khám, CLS, kê đơn
  [ĐẠT] check-in LH000004 (hôm nay) → tạo phiếu khám
  [ĐẠT] lịch hẹn → DA_DEN
  [ĐẠT] bệnh nhân đang có phiếu mở không tiếp nhận thêm → "Phạm Quốc Bảo đang có phiếu khám chưa kết thúc (chờ khám / đang khám)."
  [ĐẠT] check-in lịch của ngày mai → "Chỉ check-in lịch hẹn của hôm nay."
  [ĐẠT] BS NV0001 không khám phiếu của BS NV0003 → "Hồ sơ này do bác sĩ khác phụ trách; tài khoản bác sĩ chỉ thao tác trên hồ sơ của mình."
  [ĐẠT] admin bắt đầu khám mà không chọn bác sĩ → "Chọn bác sĩ chịu trách nhiệm chuyên môn."
  [ĐẠT] chọn người không phải bác sĩ → "Lê Thị An không phải bác sĩ."
  [ĐẠT] admin bắt đầu khám, BS chịu trách nhiệm NV0003
  [ĐẠT] lập chỉ định CLS (DV02)
  [ĐẠT] ghi kết quả khám
  [ĐẠT] kết luận khi CLS chưa có kết quả → "Còn 1 chỉ định cận lâm sàng chưa có kết quả."
  [ĐẠT] nhập kết quả CLS
  [ĐẠT] cảnh báo thuốc kiểm soát đặc biệt
  [ĐẠT] lưu đơn sau khi xác nhận cảnh báo
  [ĐẠT] kết luận lượt khám
  [ĐẠT] lịch hẹn → HOAN_THANH
  [ĐẠT] nhật ký kết luận: TK0001 thao tác, NV0003 chịu trách nhiệm
  [ĐẠT] tiếp nhận trực tiếp bệnh nhân mới
  [ĐẠT] BS NV0001 tự bắt đầu khám phiếu của mình
  [ĐẠT] thuốc không liên quan dị ứng: lưu không cảnh báo
  [ĐẠT] cảnh báo dị ứng Amoxicillin
  [ĐẠT] BS lưu kết quả
  [ĐẠT] BS kết luận

== UC12/13/14/15 Kế hoạch điều trị & tái khám
  [ĐẠT] kế hoạch không có hạng mục → "Kế hoạch cần ít nhất một hạng mục."
  [ĐẠT] lập kế hoạch 2 hạng mục
  [ĐẠT] chi phí dự kiến tính từ hạng mục: 1000000
  [ĐẠT] UC14: hạng mục đến hạn xuất hiện trong danh sách tái khám
  [ĐẠT] ghi kết quả hạng mục 1
  [ĐẠT] hoàn thành khi còn hạng mục chưa làm → "Còn 1 hạng mục chưa thực hiện. Ghi kết quả hoặc hủy hạng mục trước."
  [ĐẠT] đổi ngày dự kiến hạng mục 2
  [ĐẠT] hủy hạng mục 2
  [ĐẠT] hoàn thành kế hoạch
  [ĐẠT] BS khác không sửa kế hoạch của BS khác → "Hồ sơ này do bác sĩ khác phụ trách; tài khoản bác sĩ chỉ thao tác trên hồ sơ của mình."
  [ĐẠT] lễ tân tạo lịch tái khám từ danh sách đến hạn (KH000002)

== UC04/05 Thanh toán, công nợ, yêu cầu hủy — không tự duyệt
  [ĐẠT] lượt khám vừa kết luận chờ lập hóa đơn
  [ĐẠT] lập hóa đơn từ lượt khám
  [ĐẠT] lượt khám đã có hóa đơn → "Lượt khám PK000008 đã có hóa đơn còn hiệu lực."
  [ĐẠT] CK thiếu mã giao dịch → "Nhập mã giao dịch."
  [ĐẠT] thu vượt số còn nợ → "Không thu vượt số còn nợ (500.000 ₫)."
  [ĐẠT] tiền khách đưa nhỏ hơn số thu → "Tiền khách đưa nhỏ hơn số tiền thu."
  [ĐẠT] lần 1: tiền mặt 100.000, khách đưa 200.000
  [ĐẠT] tiền thối = 100.000 (tính, không lưu)
  [ĐẠT] hóa đơn → thanh toán một phần
  [ĐẠT] lần 2: chuyển khoản phần còn lại
  [ĐẠT] hóa đơn → đã thanh toán, 2 phương thức khác nhau
  [ĐẠT] trùng mã giao dịch CK → "Mã giao dịch VCB123 đã được ghi nhận cho một lần thu khác."
  [ĐẠT] lễ tân không hủy giao dịch → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] QL hủy lần thu tiền mặt
  [ĐẠT] lần thu hủy vẫn giữ trong lịch sử; trạng thái tính lại
  [ĐẠT] UC05 công nợ theo bệnh nhân có dữ liệu
  [ĐẠT] admin duyệt yêu cầu do chính admin tạo → "Yêu cầu YC000004 do chính tài khoản admin tạo nên không được duyệt hay từ chối. Cần tài khoản khác có quyền duyệt."
  [ĐẠT] admin từ chối yêu cầu do chính admin tạo → "Yêu cầu YC000004 do chính tài khoản admin tạo nên không được duyệt hay từ chối. Cần tài khoản khác có quyền duyệt."
  [ĐẠT] gọi thẳng DAO.decide với cùng tài khoản: 0 dòng được cập nhật
  [ĐẠT] UPDATE thẳng vào CSDL bị CHECK CK_YCHuy_KhongTuDuyet chặn
  [ĐẠT] admin từ chối yêu cầu do lethian tạo
  [ĐẠT] QL (tài khoản khác) duyệt yêu cầu của admin
  [ĐẠT] hóa đơn HD000006 → DA_HUY
  [ĐẠT] lễ tân gửi yêu cầu hủy hóa đơn đã thu một phần
  [ĐẠT] trùng yêu cầu đang chờ → "Hóa đơn đã có yêu cầu hủy đang chờ duyệt."
  [ĐẠT] không thu khi đang chờ duyệt hủy → "Hóa đơn đang có yêu cầu hủy chờ duyệt; không thu thêm."
  [ĐẠT] không duyệt hủy khi còn tiền đã thu → "Hóa đơn còn 400.000 ₫ đã thu. Hủy (hoàn) các lần thu trước khi duyệt hủy hóa đơn."
  [ĐẠT] QL từ chối có lý do

== UC27/29–33/36/37 Kho
  [ĐẠT] UC27 tìm kiếm thuốc/vật tư
  [ĐẠT] UC29 thêm thuốc
  [ĐẠT] trùng tên thuốc → "Đã có thuốc tên "ibuprofen"."
  [ĐẠT] nhập lô đã hết hạn → "Ibuprofen lô IB01 đã hết hạn, không nhập kho."
  [ĐẠT] UC30 nhập 2 lô
  [ĐẠT] UC31 FEFO: lấy hết lô hạn gần (30) rồi lô sau (10)
  [ĐẠT] FEFO thiếu tồn → "Tồn khả dụng (lô còn hạn) không đủ: thiếu 920."
  [ĐẠT] xuất lô đã hết hạn → "Composite A2 (tuýp 4g) lô CP0311 đã hết hạn — không xuất; lập phiếu hủy."
  [ĐẠT] xuất vượt tồn → "Khẩu trang y tế (hộp 50) lô KT0901 chỉ còn 8, không đủ xuất 9999."
  [ĐẠT] xuất theo FEFO
  [ĐẠT] UC33 xuất thuốc KSĐB thiếu người nhận → "Xuất thuốc kiểm soát đặc biệt: bắt buộc ghi người nhận."
  [ĐẠT] UC33 xuất thuốc KSĐB thiếu bác sĩ chịu trách nhiệm → "Xuất thuốc kiểm soát đặc biệt: chọn bác sĩ chịu trách nhiệm."
  [ĐẠT] xuất thuốc KSĐB đủ người nhận + BS chịu trách nhiệm
  [ĐẠT] sổ KSĐB ghi dòng xuất kèm BS chịu trách nhiệm
  [ĐẠT] UC32 hoàn trả chưa xác nhận nguyên seal → "Chỉ nhận hoàn trả hàng còn nguyên seal/niêm phong. Xác nhận đã kiểm tra."
  [ĐẠT] hoàn trả vượt số đã xuất → "Lô LO000013 chỉ còn hoàn trả tối đa 10."
  [ĐẠT] hoàn trả 5 nguyên seal
  [ĐẠT] hoàn trả lần 2 vượt phần còn lại (5) → "Lô LO000013 chỉ còn hoàn trả tối đa 5."
  [ĐẠT] UC37 cảnh báo có lô đã hết hạn LO000009
  [ĐẠT] UC36 đánh dấu đã đặt hàng

== UC28/35 Kiểm kê, hủy/hao hụt — người duyệt khác người lập
  [ĐẠT] phiếu hư hỏng không được tăng tồn → "Phiếu hủy / hao hụt chỉ được giảm tồn (lô CP0702)."
  [ĐẠT] loại hết hạn cho lô chưa hết hạn → "Lô CP0702 chưa hết hạn; chọn loại Hư hỏng hoặc Kiểm kê."
  [ĐẠT] lập phiếu hủy lô hết hạn
  [ĐẠT] lô đang nằm trong phiếu chờ duyệt khác → "Lô CP0311 đang nằm trong phiếu điều chỉnh khác chờ duyệt."
  [ĐẠT] admin lập phiếu kiểm kê
  [ĐẠT] admin duyệt phiếu do chính admin lập → "Phiếu DC000005 do chính tài khoản admin lập nên không được duyệt hay từ chối. Cần tài khoản khác có quyền duyệt."
  [ĐẠT] gọi thẳng DAO.decide cùng tài khoản: 0 dòng
  [ĐẠT] UPDATE thẳng bị CHECK CK_PDC_KhongTuDuyet chặn
  [ĐẠT] admin duyệt phiếu của quockhanh
  [ĐẠT] tồn LO000009 → 0 sau duyệt
  [ĐẠT] từ chối không có lý do → "Nhập lý do từ chối."
  [ĐẠT] QL duyệt phiếu của admin

== UC34 Hộp cấp cứu (danh mục mẫu, cấu hình được)
  [ĐẠT] 6 mục cài sẵn được đánh dấu dữ liệu mẫu
  [ĐẠT] kiểm tra thiếu mục → "Chưa nhập kết quả cho mục Adrenaline 1mg/ml."
  [ĐẠT] kiểm tra đủ mọi mục
  [ĐẠT] kết quả DAT khi đủ số lượng và hạn > ngưỡng
  [ĐẠT] thêm mục cấu hình
  [ĐẠT] bổ sung từ kho (xuất kho bộ phận Hộp cấp cứu)
  [ĐẠT] số hiện có của mục = 4
  [ĐẠT] đổi ngưỡng hạn dùng 45 ngày

== UC17/18/20/24 Phân công · UC19 Bàn giao
  [ĐẠT] phân ca 1
  [ĐẠT] trùng ca → "Đỗ Thị Hạnh đã có phân công Ca sáng ngày 20/10/2026."
  [ĐẠT] phân ca 2 cùng ngày
  [ĐẠT] vượt tối đa 2 ca/ngày → "Đỗ Thị Hạnh đã đủ 2 ca trong ngày 20/10/2026."
  [ĐẠT] phân công ngày đã qua → "Không phân công cho ngày đã qua."
  [ĐẠT] thay bằng người khác chức danh → "Người thay phải cùng chức danh (Y tá)."
  [ĐẠT] thay người (hủy bản cũ + tạo bản mới)
  [ĐẠT] bản mới trỏ bản gốc, bản cũ DA_HUY có lý do
  [ĐẠT] bắt đầu nhiệm vụ không đúng ngày → "Chỉ bắt đầu nhiệm vụ trong đúng ngày làm (20/10/2026)."
  [ĐẠT] sao chép tuần trước: tạo 87, bỏ qua 2
  [ĐẠT] y tá tự bắt đầu nhiệm vụ hôm nay
  [ĐẠT] y tá không sửa nhiệm vụ (chỉ QL) → "Tài khoản vophuc (Y tá) không có quyền thực hiện thao tác này."
  [ĐẠT] admin lập biên bản có chênh lệch
  [ĐẠT] admin tự xác nhận biên bản mình lập → "Biên bản do chính tài khoản admin lập nên không được tự xác nhận. Cần người nhận ca hoặc quản lý khác xác nhận."
  [ĐẠT] UPDATE thẳng bị CHECK CK_BanGiao_KhongTuXacNhan chặn
  [ĐẠT] có chênh lệch mà không ghi chú → "Biên bản có chênh lệch: nhập ghi chú xác nhận (đã kiểm tra, cách xử lý)."
  [ĐẠT] người nhận xác nhận
  [ĐẠT] y tá lập biên bản thay người khác → "Nhân viên chỉ lập biên bản cho ca mình giao."

== Thủ tục nghỉ việc có điều kiện (không tự hủy lịch sử)
  [ĐẠT] BS Lê Thu còn 14 phân công, 4 lịch hẹn, 0 phiếu khám, 0 bàn giao
  [ĐẠT] hoàn tất khi chưa xử lý xong → "Chưa đủ điều kiện: còn 14 phân công, 4 lịch hẹn, 0 phiếu khám, 0 biên bản bàn giao chưa xử lý."
  [ĐẠT] xử lý từng phân công (thay người/hủy) và lịch hẹn (chuyển BS/hủy) có lý do
  [ĐẠT] đủ điều kiện sau khi xử lý
  [ĐẠT] hoàn tất nghỉ việc
  [ĐẠT] tài khoản → VO_HIEU
  [ĐẠT] không xóa phân công lịch sử
  [ĐẠT] không xóa nhân viên
  [ĐẠT] nhân viên đã nghỉ không đăng nhập được → "Tài khoản đã bị vô hiệu hóa và không thể đăng nhập."
  [ĐẠT] không tự làm thủ tục nghỉ việc cho chính mình → "Không thể tự làm thủ tục nghỉ việc cho chính mình."

== UC25 Đánh giá nhân sự
  [ĐẠT] điểm ngoài 1–5 → "Điểm tiêu chí "Chuyên cần, đúng giờ" phải từ 1 đến 5."
  [ĐẠT] tự đánh giá → "Không tự đánh giá chính mình."
  [ĐẠT] lưu nháp
  [ĐẠT] gửi đánh giá
  [ĐẠT] điểm trung bình = 4.80
  [ĐẠT] sửa đánh giá đã gửi → "Đánh giá kỳ 2026-Q4 của Đỗ Thị Hạnh đã gửi, không sửa được."

== Class diagram: DichVu.capNhatDonGia · CaLamViec.capNhatCa
  [ĐẠT] lễ tân xem bảng giá
  [ĐẠT] lễ tân đổi giá → "Tài khoản lethian (Lễ tân) không có quyền thực hiện thao tác này."
  [ĐẠT] QL cập nhật đơn giá DV01
  [ĐẠT] hóa đơn đã lập giữ đơn giá cũ (donGiaApDung)
  [ĐẠT] trùng tên dịch vụ → "Đã có dịch vụ "khám tổng quát"."
  [ĐẠT] giờ kết thúc trước giờ bắt đầu → "Giờ kết thúc phải sau giờ bắt đầu."
  [ĐẠT] QL đổi định mức bác sĩ ca sáng = 3
  [ĐẠT] định mức đã lưu

== UC21/22/26 Tài khoản & nhân viên
  [ĐẠT] SĐT trùng nhân viên khác → "Số điện thoại đã thuộc nhân viên khác."
  [ĐẠT] thêm nhân viên
  [ĐẠT] tên đăng nhập trùng → "Tên đăng nhập "admin" đã tồn tại."
  [ĐẠT] mật khẩu yếu → "Mật khẩu tối thiểu 8 ký tự."
  [ĐẠT] QL cấp vai trò quản trị → "Chỉ quản trị hệ thống được cấp vai trò quản trị."
  [ĐẠT] cấp tài khoản
  [ĐẠT] khóa (BI_KHOA)
  [ĐẠT] mở khóa
  [ĐẠT] đổi vai trò
  [ĐẠT] tự khóa tài khoản đang đăng nhập → "Không thể tự khóa tài khoản đang đăng nhập."
  [ĐẠT] QL vô hiệu tài khoản quản trị → "Tài khoản quản trị hệ thống chỉ do quản trị viên khác thay đổi."
  [ĐẠT] vô hiệu (VO_HIEU)
  [ĐẠT] không mở lại tài khoản VO_HIEU → "Chỉ mở khóa được tài khoản đang bị khóa."
  [ĐẠT] đổi mật khẩu của chính mình
  [ĐẠT] đăng nhập bằng mật khẩu mới

== Tổng quan theo vai trò
      admin: lịch hẹn=true, doanh thu={Tiền mặt=700000, Chuyển khoản ngân hàng=400000, Thẻ (POS)=0}, kho dưới định mức=3, ca thiếu=[Ca sáng: Bác sĩ thiếu 1, Ca chiều: Bác sĩ thiếu 1, Y tá thiếu 1, Ca tối: Y tá thiếu 1]
  [ĐẠT] tải Tổng quan với admin
      lethian: lịch hẹn=true, doanh thu={Tiền mặt=700000, Chuyển khoản ngân hàng=400000, Thẻ (POS)=0}, kho dưới định mức=null, ca thiếu=null
  [ĐẠT] tải Tổng quan với lethian
      tranminh: lịch hẹn=true, doanh thu=null, kho dưới định mức=null, ca thiếu=null
  [ĐẠT] tải Tổng quan với tranminh
      vophuc: lịch hẹn=false, doanh thu=null, kho dưới định mức=null, ca thiếu=null
  [ĐẠT] tải Tổng quan với vophuc
      minhtam: lịch hẹn=false, doanh thu={Tiền mặt=700000, Chuyển khoản ngân hàng=400000, Thẻ (POS)=0}, kho dưới định mức=3, ca thiếu=[Ca sáng: Bác sĩ thiếu 1, Ca chiều: Bác sĩ thiếu 1, Y tá thiếu 1, Ca tối: Y tá thiếu 1]
  [ĐẠT] tải Tổng quan với minhtam
      quockhanh: lịch hẹn=false, doanh thu=null, kho dưới định mức=3, ca thiếu=null
  [ĐẠT] tải Tổng quan với quockhanh

== Đối soát dữ liệu sau toàn bộ kiểm thử (truy vấn của 04_kiem_tra.sql)
  [ĐẠT] tồn lô khớp lịch sử phiếu (lệch: 0)
  [ĐẠT] trạng thái hóa đơn khớp số đã thu []
  [ĐẠT] không có yêu cầu hủy tự duyệt
  [ĐẠT] không có phiếu điều chỉnh tự duyệt
  [ĐẠT] nhân viên nghỉ việc không còn tài khoản hoạt động
  [ĐẠT] nhân viên nghỉ việc không còn phân công mở
  [ĐẠT] nhật ký đã ghi 201 dòng
(lịch hẹn tạo trong kiểm thử: LH000016)

==================== KẾT QUẢ: 248 đạt, 0 lỗi
```

## 4. Kiểm thử giao diện (nhật ký chạy thật)

```text
admin: mở 21 màn hình
Đã mở 19 hộp thoại
lethian: mở 9 màn hình
tranminh: mở 7 màn hình
minhtam: mở 13 màn hình
quockhanh: mở 7 màn hình
vophuc: mở 3 màn hình
Ảnh đã lưu tại /tmp/claude-0/shots
KHÔNG CÓ NGOẠI LỆ TRÊN LUỒNG GIAO DIỆN
```

Ảnh chụp tiêu biểu: `docs/anh-giao-dien/` (màn hình của tài khoản `admin` và một số vai trò khác, các hộp thoại chính).
Kiểm thử giao diện chỉ mở màn hình / hộp thoại, tải dữ liệu qua Service và chụp ảnh; thao tác nghiệp vụ được kiểm ở mục 3.

## 5. Kiểm tra đối soát dữ liệu

Cuối bộ tích hợp chạy lại các truy vấn của `sql/04_kiem_tra.sql` (dạng chạy được trên Derby):
tồn lô khớp lịch sử phiếu, trạng thái hóa đơn khớp số đã thu, không có yêu cầu / phiếu tự duyệt,
nhân viên nghỉ việc không còn tài khoản hoạt động hay phân công mở — đều đạt (xem cuối nhật ký mục 3).
