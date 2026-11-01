# BÀN GIAO UI-M2-A — Dashboard Admin read-only (tầng query/service/DTO + kiểm thử)

**Ngày:** 19/10/2026
**Repo:** `HuyNighty/quan_ly_nha_khoa`, nhánh `main`, HEAD `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704` (sạch trước khi làm)
**Căn cứ:** 01-BA-DECISIONS-V1, 02-ROLE-MATRIX-V1, 03-DASHBOARD-API-CONTRACT-V1, 04-CODEX-PROMPT-UI-M2-A, 05-BA-REVIEW-DISPOSITION-V1.1, 06-CODEX-PROMPT-UI-M2-A-ADDENDUM
**Kết luận ngắn:** Tầng đọc Dashboard đã xong, có 31 kiểm thử chạy PASS bằng bộ chạy tạm trên JDK 25. **REST LIVE vẫn BỊ CHẶN** vì checkout chưa có AUTH-R1. **Chưa chạy được Maven** vì môi trường bị chặn Maven Central (ENV_BLOCKED). Không commit, push hay merge.

---

## 1. Tiền kiểm và cổng (gate)

| Gate | Kết quả | Bằng chứng |
|---|---|---|
| Checkout | OK | `main` @ `4d25dbf`, `git status` sạch trước khi sửa; sau khi sửa **chỉ có file mới** (không sửa file nào có sẵn) |
| **GATE_AUTH (AUTH-R1)** | **BLOCKED** | `pom.xml` không có `spring-boot-starter-security`; không có `SecurityFilterChain`, BCrypt, đăng nhập, bootstrap ADMIN hay test 401/403. Không có controller nào trong `src/main` |
| GATE_UI_M1 | **MISSING** | Không có `admin-desktop-preview/` trong `main` → không thể đối chiếu 21 khóa menu, không kiểm được nhãn "Bệnh nhân chờ khám" trong preview |
| JDK | Máy làm việc chỉ có Java 21; đã tải **Temurin 25+36** vào thư mục tạm của phiên (không đổi cấu hình hệ thống) | `java -version` → `25 2025-09-16 LTS` |
| **GATE_MAVEN** | **ENV_BLOCKED** | `mvn -s <settings tạm> -Dmaven.repo.local=<thư mục tạm> test-compile` → `repo.maven.apache.org: 403 Forbidden` từ proxy egress của môi trường. Không xoá `.m2`, không đổi cấu hình global |
| SQL Server test | NOT RUN | Không có DB test; `NhakhoaApplicationTests.contextLoads` cần SQL Server thật nên không chạy |
| `.env` | **RỦI RO** | `nhakhoa/.env` đang được Git theo dõi (ở HEAD có 0 byte). Không đọc, không in, không sửa. Đề nghị đưa vào `.gitignore` và kiểm tra lịch sử Git ở task riêng |
| `ddl-auto: update` | **RỦI RO** | `application.yaml` cho Hibernate tự sửa schema khi chạy với DB thật. Không đổi trong task này (ngoài phạm vi), cần quyết định riêng trước khi nối DB production |

## 2. Danh sách file (43 file mới, 0 file sửa)

Package gốc `com.manage.nhakhoa.features.dashboard` (không tạo `com.quanlynhakhoa`).

**Main (39 file)**

- `config/DashboardClockConfig.java` — bean `Clock` theo `Asia/Ho_Chi_Minh` (`@ConditionalOnMissingBean`, không đè Clock khác nếu đã có).
- `dto/response/` (19 file) — `AdminDashboardResponse` và các record bất biến: `PeriodDto`, `AppointmentSummary`, `ExamSummary`, `CollectionSummary`, `ReceivableSummary`, `StaffingSummary`, `ShiftStaffingDto`, `RoleCount`, `InventorySummary`, `EmergencyKitSummary`, `TreatmentDueSummary`, `WorkQueueDto`, `WorkQueueItem`, `DashboardWarning`; các enum `SectionStatus`, `ShiftState`, `EmergencyCheckState`, `WorkPriority`.
- `repository/` (9 file) — cổng đọc `DashboardReadRepository` (chỉ có phương thức `count/sum/find`), bản JPA `JpaDashboardReadRepository` (`@Repository`, `@Transactional(readOnly = true)`, JPQL trên Entity hiện hành), các record dòng kết quả `InvoiceAmountRow`, `ShiftRow`, `ShiftAssignmentRow`, `StockLotRow`, `StockItemRow`, `StockItemType`, `EmergencyCheckRow`.
- `service/` (10 file) — `DashboardQueryService` (tổng hợp), các bộ tính thuần `ReceivableCalculator`, `InventoryCalculator`, `StaffingCalculator`, `WorkQueueBuilder`, `ThresholdResolver`, `Money`, hằng `DashboardThresholds`, `DashboardTargetScreens`, ngoại lệ `DashboardDataUnavailableException`.

**Test (4 file)**

- `service/DashboardQueryServiceTest.java` — 24 test công thức nghiệp vụ, Clock cố định.
- `service/DashboardDtoPrivacyTest.java` — duyệt toàn bộ cây DTO: không có trường PII/PHI, không tham chiếu Entity.
- `service/InMemoryDashboardReadRepository.java` — bản giả lập mô phỏng đúng ngữ nghĩa các truy vấn, có thể ép lỗi từng truy vấn.
- `repository/JpaDashboardReadRepositoryTest.java` — 6 test cấu trúc JPQL (toán tử biên, tham số enum, aggregate tách riêng, chỉ có SELECT) qua `EntityManager` giả bằng `java.lang.reflect.Proxy`.

**Không thay đổi:** Entity, enum, schema, migration, `pom.xml`, `application.yaml`, `.env`, AUTH/JWT, Swing preview/fixture. **Không có Controller, không có `permitAll`.**

## 3. Đối chiếu từng chỉ số với BA

| Section | Nguồn và công thức đã cài | Trạng thái khi lỗi/thiếu |
|---|---|---|
| Ngày nghiệp vụ (Q02) | `D = ZonedDateTime.now(clock.withZone(Asia/Ho_Chi_Minh))`; khoảng `[D 00:00, D+1 00:00)`; `generatedAt` và `period` có offset `+07:00` | — |
| Lịch hẹn (Q05, V1.1 #03) | `LichHen.thoiGianHen` trong khoảng; đủ 7 trạng thái kể cả 0; `totalRecords = Σ byStatus` (gồm `DA_HUY`, `DA_DOI_LICH`); `activeCount = CHO_XAC_NHAN + DA_XAC_NHAN + DA_DEN` là trường riêng | Lỗi → `DashboardDataUnavailableException` (sẽ là 503), không trả 0 |
| Phiếu chờ khám (Q04) | Đếm **phiếu** `PhieuKham` theo `thoiGianTiepNhan` trong ngày: `CHO_KHAM`, `DANG_KHAM`, `DA_KET_LUAN`; `DA_HUY` không tính; `unit = PHIEU_KHAM_TIEP_NHAN_TRONG_NGAY`; nhãn work queue "Phiếu chờ khám" | Như trên |
| Thu hợp lệ trong ngày (Q06) | `ThanhToan` `THANH_CONG` theo `thoiGian` trong ngày, đủ 3 phương thức; `amountVnd = Σ byMethodVnd`; chuỗi số nguyên; có thu thành công trên hóa đơn `DA_HUY` → cảnh báo `THU_TREN_HOA_DON_DA_HUY` | Như trên; tiền có phần lẻ → lỗi, **không làm tròn ngầm** |
| Công nợ (Q07, V1.1 #05) | Hai aggregate tách riêng theo hóa đơn khác `DA_HUY` (tổng `donGiaApDung × soLuong` / tổng thanh toán `THANH_CONG`), gộp trong Java: `max(billed − paid, 0)`; hóa đơn đang có yêu cầu hủy `CHO_DUYET` **vẫn tính**; thu vượt → `overpaidInvoiceCount` + cảnh báo `HOA_DON_THU_VUOT`, không thành công nợ âm; BigDecimal scale 0 | Như trên |
| Nhân sự (Q08) | Mọi `CaLamViec`; `COUNT(DISTINCT maNhanVien)` theo (ca, chức danh BS/YT/LT) với `PhanCong.ngayLam = D`, trạng thái `CHO_HIEU_LUC/DANG_THUC_HIEN/HOAN_THANH`, nhân viên `DANG_LAM_VIEC`; `missing = max(định mức − đã phân, 0)`; ca qua đêm hợp lệ (`overnight=true`); giờ bắt đầu = kết thúc hoặc định mức âm/null → `LOI_CAU_HINH` | Lỗi → `UNAVAILABLE`, `shifts=[]`, count `null`; chưa có ca nào → `UNCONFIGURED` |
| Kho (Q09/Q10) | Lô `soLuongTon ≠ 0`; hết hạn `HSD ≤ D`; cận hạn `D < HSD ≤ D+N`; HSD null → unknown, không tính khả dụng; tồn khả dụng theo mã mặt hàng chỉ từ lô còn hạn; dưới định mức đếm **mã duy nhất** với `dinhMucToiThieu > 0`; mặt hàng không có lô = tồn 0; lô tồn âm hoặc trỏ cả hai/không trỏ Thuoc/VatTu → `anomalyLotCount` + cảnh báo, loại khỏi phép tính. `N` đọc từ `SO_NGAY_CANH_BAO_HSD`, fallback 30 + cảnh báo | Lỗi → `UNAVAILABLE`, mọi count `null` |
| Hộp cấp cứu (Q11) | Không có mục `dangApDung=true, laDuLieuMau=false` → `UNCONFIGURED` "Chưa có danh mục cấp cứu chính thức". Có danh mục: lấy `KiemTraCapCuu` gần nhất `≤ now`; không có → `CHUA_CO_KIEM_TRA`; `CAN_BO_SUNG` hoặc có mục `dat=false`/HSD `≤ D+30` → `CAN_XU_LY`; quá chu kỳ (`> 7` ngày) → `QUA_CHU_KY`; còn lại `TRONG_CHU_KY` kèm câu "không phải chứng nhận an toàn lâm sàng". **Không có trạng thái "ĐẠT"** | Lỗi → `UNAVAILABLE` |
| Kế hoạch điều trị đến hạn (Q12) | `COUNT(DISTINCT maKeHoach)`: kế hoạch `DANG_THUC_HIEN` có hạng mục `CHUA_THUC_HIEN` với `ngayDuKien ≤ D+14`; `overduePlanCount` khi `ngayDuKien < D` | Lỗi → `UNAVAILABLE`, count `null` |
| Work queue (Q03) | Chỉ xuất nhóm có nguồn truy vấn thật và `objectCount > 0`; sắp xếp KHẨN → CHỜ DUYỆT → THEO DÕI rồi theo code; `workGroupCount` = số nhóm, `relatedObjectCount` = tổng object (có thể trùng). 16 code: `HOA_DON_THU_VUOT`, `CA_THIEU_NHAN_SU`, `CA_LOI_CAU_HINH`, `KHO_HET_HAN`, `KHO_DUOI_DINH_MUC`, `KHO_CAN_HAN`, `KHO_CHUA_RO_HSD`, `CAP_CUU_CHUA_CAU_HINH`, `CAP_CUU_CAN_KIEM_TRA`, `CAP_CUU_CAN_XU_LY`, `KE_HOACH_QUA_HAN`, `KE_HOACH_DEN_HAN`, `LICH_HEN_CHO_XAC_NHAN`, `PHIEU_CHO_KHAM`, `HOA_DON_CHO_DUYET_HUY`, `KHO_CHO_DUYET_DIEU_CHINH` | Section lỗi → không sinh dòng việc giả |

Cảnh báo (`warnings[]`) chỉ có `code`, `section`, câu tiếng Việt cố định; **không chứa SQL, tham số hay dữ liệu cá nhân** (có test kiểm tra). Service không ghi log.

## 4. Kiểm thử

**Lệnh đã chạy** (JDK 25, bộ chạy tạm vì Maven bị chặn):

```
javac --release 25 -Xlint:all -encoding UTF-8 -d out @srcs.txt   # stub API + enum + dashboard main/test
java -cp out MiniRunner <3 lớp test>
```

**Kết quả:** biên dịch 0 lỗi, 0 cảnh báo; **31/31 PASS** (24 service, 1 privacy, 6 JPQL structure).

**Kiểm tra độ nhạy (mutation):** cố tình làm sai 5 công thức (HSD hôm nay coi còn hạn; cộng công nợ âm; `activeCount` thiếu `DA_DEN`; không chặn `missing` âm; chu kỳ cấp cứu lệch 1 ngày) → **cả 5 lần đều có test FAIL**, sau đó khôi phục mã đúng.

Các ca đã phủ (theo doc 03 §5 và doc 06 Phần D): DB trống trả 0 hợp lệ; ranh giới 23:59:59/00:00 và ngày theo giờ VN chứ không theo UTC; đủ 7 trạng thái lịch, đổi lịch cùng ngày/khác ngày; phiếu chờ/đang/đã kết luận, loại `DA_HUY`; thu theo phương thức, loại thanh toán hủy, loại hôm qua/nửa đêm sau; hóa đơn 2 dòng + 2 lần thu + 1 lần thu hủy không nhân đôi; hóa đơn chờ duyệt hủy vẫn tính nợ; hóa đơn `DA_HUY` loại; thu vượt là anomaly; số tiền 18 chữ số cộng ra 19 chữ số chính xác; tiền lẻ không bị làm tròn ngầm; nhân sự DISTINCT, lọc nghỉ việc/hủy/ngày khác/chức danh AD, ca qua đêm, ca lỗi cấu hình, thừa người không ra số âm; kho HSD hôm nay/biên 30 ngày/null/lô rỗng/tồn âm/trỏ cả hai, dưới định mức đếm mã duy nhất; tham số cấu hình hợp lệ/sai/0/lỗi đọc; cấp cứu chỉ có dữ liệu mẫu, chưa kiểm tra, biên 7/8 ngày, cần bổ sung, HSD sát hạn; kế hoạch DISTINCT, biên D+14/D+15, quá hạn; lỗi DB section lõi ném ngoại lệ không lộ SQL; lỗi section phụ thành `UNAVAILABLE` với `null`; work queue động, đúng thứ tự; collection bất biến; service chỉ gọi truy vấn đọc; DTO không PII/PHI.

**Trạng thái trung thực:**

| Hạng mục | Trạng thái |
|---|---|
| Logic nghiệp vụ (service + calculator) | **PASS** — bộ chạy tạm, JDK 25 |
| Biên dịch với Spring/Hibernate/JUnit thật | **NOT VERIFIED** — biên dịch dựa trên stub API tối giản (`@Service`, `@Repository`, `@Transactional`, `EntityManager`, `TypedQuery`, JUnit `Assertions` cùng tập overload) |
| `mvn -B -f nhakhoa/pom.xml test` | **ENV_BLOCKED** (Maven Central 403) |
| JPQL chạy trên Hibernate 7 + SQL Server | **NOT VERIFIED** — chỉ kiểm cấu trúc chuỗi truy vấn và ánh xạ kết quả |
| REST 401/403/200 | **NOT RUN** — endpoint không được tạo |

## 5. Hạn chế và sai lệch so với contract

1. **Không có endpoint `GET /manage/api/v1/admin/dashboard`.** Lý do: AUTH-R1 không có trong checkout; tạo endpoint lúc này đồng nghĩa mở số liệu tài chính/lâm sàng không có bảo vệ AD.
2. `source = "LIVE"` chỉ được gán khi đọc DB thành công; vì chưa có HTTP nên không có UI nào hiện nhãn LIVE.
3. **Bổ sung so với ví dụ contract 03** (không bỏ trường nào): `appointments.activeCount`, `receivables.overpaidInvoiceCount`, `staffing.understaffedShiftCount`, `inventory.anomalyLotCount`, `emergencyKit.checkState` + `checkCycleDays`; `warnings[]` là object `{code, section, message}`. Cần BA/UI-M2-B xác nhận.
4. Tên khóa `SO_NGAY_KE_HOACH_DEN_HAN` là **đề xuất** (BA V1 Q12 chỉ chốt 14 ngày, chưa chốt tên).
5. `targetScreen` (`LICH_HEN`, `PHIEU_KHAM`, `HOA_DON`, `PHAN_CONG_CA`, `TON_KHO`, `DIEU_CHINH_KHO`, `CAP_CUU`, `KE_HOACH_DIEU_TRI`) **chưa đối chiếu** với 21 khóa menu UI-M1 vì không có `admin-desktop-preview/`.
6. Q12 "Đã có lịch" (lịch còn hiệu lực có `LichHen.keHoach`) chưa xuất vì contract chưa có trường; có thể thêm ở UI-M2-B.
7. Chưa phản ánh chiết khấu/điều chỉnh/hoàn tiền vì Entity không có (đúng BA Q07).
8. `BanGiaoCa` chờ xác nhận chưa đưa vào work queue vì BA chưa chốt định nghĩa.
9. Kho đọc toàn bộ lô tồn khác 0 và toàn bộ mặt hàng vào bộ nhớ để tính trong Java (dễ kiểm thử). Phù hợp quy mô phòng khám; nếu dữ liệu rất lớn cần chuyển sang aggregate SQL.

## 6. Việc tiếp theo

1. **Chạy trên máy nhóm (có Maven Central):** `mvnw -B -f nhakhoa/pom.xml -Dtest="Dashboard*Test,JpaDashboard*Test" test` — 3 lớp test mới không cần SQL Server. `contextLoads` có sẵn vẫn cần DB.
2. **Integration test JPQL** trên SQL Server test (Testcontainers hoặc DB test riêng, không dùng production) cho 18 truy vấn trong `JpaDashboardReadRepository`.
3. **AUTH-R1 (task riêng):** đưa Spring Security, BCrypt, bootstrap ADMIN, cơ chế định danh vào đúng checkout kèm test. Sau đó mới thêm `AdminDashboardController` GET read-only với `hasRole('AD')`, test 401/403/200, `Cache-Control: no-store`, map `DashboardDataUnavailableException` → 503.
4. **UI-M2-B:** đối chiếu `targetScreen` với 21 khóa menu, thêm `HttpDashboardDataProvider` cho `--live` khi đã có auth; preview `--fixture`/`--runtime` giữ dữ liệu mô phỏng.
5. BA xác nhận: các trường bổ sung ở mục 5.3, tên khóa `SO_NGAY_KE_HOACH_DEN_HAN`, cách hiển thị "Đã có lịch".
6. Quản lý repo: gỡ `nhakhoa/.env` khỏi Git theo dõi; quyết định về `ddl-auto: update`.

## 7. Áp dụng bản vá

File `UI-M2-A-dashboard-readonly.patch` (43 file mới) tạo từ HEAD `4d25dbf`:

```
git checkout -b feature/ui-m2-a-dashboard-readonly
git apply --check UI-M2-A-dashboard-readonly.patch
git apply UI-M2-A-dashboard-readonly.patch
```

Việc commit, push và merge do nhóm tự thực hiện sau khi review.
