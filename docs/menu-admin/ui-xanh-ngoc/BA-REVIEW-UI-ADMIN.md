# N08 — Rà soát nghiệp vụ 21 màn hình Admin (bản thiết kế xanh ngọc)

**Ngày:** 19/10/2026 · **Phạm vi:** ảnh `docs/menu-admin/ui-xanh-ngoc/01…21` · **Chế độ:** chỉ đọc, chưa sửa.
**Căn cứ:** brief "N08 — ADMIN DESKTOP" (Q01–Q16, Role-only `AD/LT/BS/YT/QL/KHO`), `THIET_KE_DU_LIEU.md`, `KIEM_THU.md`.
**Không truy cập được:** repository `HuyNighty/quan_ly_nha_khoa`, `BA-OPEN-QUESTIONS.md`, mã UI-M1 của Codex → các điểm liên quan ghi **NOT VERIFIED**.

Mức độ: **BLOCKER** sai nghiệp vụ nghiêm trọng · **MAJOR** ảnh hưởng đáng kể / trái quy tắc brief · **MINOR** trình bày.

---

## 1. Kết luận nhanh

- Luồng chính **đúng hướng**: Lịch hẹn → Tiếp nhận → Phiếu khám → Kế hoạch → Tái khám; Phiếu khám → Hóa đơn → Duyệt hủy; Xuất kho → Tồn → Kiểm kê → Cấp cứu. Số liệu giữa các màn hình khớp nhau.
- **Không có BLOCKER.**
- Có **10 điểm MAJOR**. Phần lớn là UI đang thể hiện quy tắc mà brief nói **chưa được duyệt** (ngưỡng ngày, cách đếm, quyền). Đây là nguy cơ Codex code cứng quy tắc chưa chốt.
- Nhiều giá trị trong ảnh là **dữ liệu mẫu tự đặt**, không phải yêu cầu.

---

## 2. Phát hiện

### MAJOR

| # | Màn hình | Vấn đề | Căn cứ | Hướng xử lý (ĐỀ XUẤT) |
|---|---|---|---|---|
| M1 | 01 Tổng quan | KPI ghi **"Bệnh nhân chờ khám"** nhưng thực chất đếm phiếu `CHO_KHAM`. | Q04 | Đổi nhãn thành **"Phiếu chờ khám"**; dòng phụ "x đang khám · y đã kết luận" giữ nguyên là số phiếu. |
| M2 | 01 Tổng quan, menu | Badge đỏ "9" cạnh "Việc cần xử lý" là **số nhóm việc**, còn mỗi dòng là **số đối tượng** (1, 2, 6…). Badge menu lại đếm theo kiểu thứ ba (chỉ việc chờ duyệt): Thuốc & vật tư = 1 dù có lô hết hạn, hộp cấp cứu. Ba bộ đếm khác nghĩa nhưng hiển thị giống nhau. | Q03 | Định nghĩa riêng từng bộ đếm. Bỏ badge "9" hoặc ghi rõ "9 nhóm". Ghi định nghĩa badge menu vào quy ước. |
| M3 | 01, 03 Lịch hẹn | "Lịch hẹn hôm nay = 10" **gồm cả lịch Đã đổi lịch (LH000006)**, trong khi lịch mới LH000014 nằm ngày 15/10. Lịch hủy cũng được tính. Nếu đếm theo ngày gốc thì chuỗi đổi lịch có thể bị đếm hai lần. | Q05 | Chốt quy tắc: tổng "hôm nay" có tính Đã hủy / Đã đổi lịch không, hay chỉ hiện chúng ở dòng chi tiết. |
| M4 | 18 Cấp cứu, 15 Thuốc, 07 Tái khám | UI ghi cứng **"HSD còn hơn 30 ngày"**, **"chu kỳ 7 ngày"**, **"đến hạn trong 14 ngày"** như quy tắc đã chốt. | Q10, Q11, Q12: không mặc định 30 ngày nếu chưa duyệt | Hiện ngưỡng dạng "theo cấu hình (hiện tại: 30 ngày)" và đưa vào màn hình cấu hình. Không viết cứng vào nhãn. |
| M5 | 08 Hóa đơn, 01 | Công nợ 3.600.000 đ **đang gồm 2 hóa đơn "Chờ duyệt hủy"** (HD000001, HD000004). | Q07 | Chốt: hóa đơn chờ hủy có tính công nợ không. Hiện tại tính. |
| M6 | 08 Hóa đơn, 09 | Trạng thái **"Chờ duyệt hủy"** đặt chung cột với trạng thái thanh toán. Trong thiết kế dữ liệu, yêu cầu hủy là bảng riêng `YeuCauHuyHoaDon`, không phải trạng thái hóa đơn. | THIET_KE §2 | Giữ trạng thái thanh toán ở cột Trạng thái; thêm nhãn phụ "Đang chờ hủy". |
| M7 | 02 Tiếp nhận | Quy tắc **"lịch Chờ xác nhận vẫn tiếp nhận được, chuyển thẳng sang Đã đến"** mới được duyệt trong buổi thiết kế UI, **chưa có trong BA Decision Register**. | Q16, Q14 | Đưa vào Q16 dạng ĐỀ XUẤT chờ PO / BA duyệt trước khi Codex code. |
| M8 | 20 Tài khoản | Tab **"Ma trận quyền"** và nút **"Đổi vai trò…"** gợi ý một hệ thống Permission/ACL có thể chỉnh. | Q13: Role-only, không phát sinh Permission/ACL | Đổi thành **"Bảng vai trò – chức năng" (chỉ xem)**. "Đổi vai trò…" chỉ chọn 1 trong 6 vai trò. |
| M9 | 19 Nhà cung cấp | Nút **"Ngừng hợp tác…"**, các cột Mã số thuế, Địa chỉ và lọc "Đang hợp tác" **có thể chưa có trong schema** `NhaCungCap`. | Brief: không sửa schema chưa duyệt | NOT VERIFIED. Nếu bảng không có cột trạng thái thì bỏ nút và bộ lọc; chỉ hiện cột có thật. |
| M10 | 05, 06, 04 | Admin thao tác lâm sàng (Kết luận…, Ghi kết quả…, Cập nhật tiền sử…) cần **chọn bác sĩ chịu trách nhiệm**. Ảnh chỉ thể hiện điều này ở Phiếu khám, chưa có ở hộp thoại Kế hoạch / Hồ sơ. | KIEM_THU ("Chọn bác sĩ chịu trách nhiệm"), Q13, Q15 | Ghi yêu cầu cho mọi hộp thoại lâm sàng: ô "BS chịu trách nhiệm *" bắt buộc khi người thao tác là AD. |

### MINOR

| # | Màn hình | Vấn đề | Đề xuất |
|---|---|---|---|
| m1 | 11 Nhân viên | NV0000 (Quản trị viên) nằm trong danh sách. Theo quy tắc không làm thủ tục nghỉ việc cho tài khoản quản trị, nhưng ảnh chưa thể hiện. | Khi chọn NV0000: nút "Thủ tục nghỉ việc…" làm mờ, kèm lý do. |
| m2 | 04 Hồ sơ, 02 | SĐT bệnh nhân hiện đầy đủ ở mọi danh sách. | Q15: xác định vai trò nào thấy đầy đủ, vai trò nào bị che. |
| m3 | 14 Đánh giá | Tên 5 tiêu chí là **tự đặt**. | NOT VERIFIED: đối chiếu `ChiTietDanhGia` / Figma QL-07. |
| m4 | 13 Bàn giao | Nội dung bàn giao ghi số tiền tại quầy. | Q15: xác nhận dữ liệu tài chính có được ghi trong biên bản / log không. |
| m5 | 21 Nhật ký | Cột "Nội dung" phải không chứa dữ liệu nhạy cảm (chẩn đoán, dị ứng). | Q15: liệt kê trường cấm ghi log. |
| m6 | 18 Cấp cứu | Adrenaline trong kho có cùng HSD 03/11/2026, nên "Bổ sung từ kho" không giải quyết cảnh báo sắp hết hạn. | Thêm gợi ý "cần nhập lô mới" khi lô kho cũng sắp hết hạn. |
| m7 | 12 Phân công | Không có ca qua đêm; chưa thể hiện nhân viên vắng hoặc nghỉ việc trong ca. | Q08: bổ sung quy tắc rồi mới thêm vào UI. |
| m8 | Tất cả | Thời điểm "12/10/2026, 09:15" không ghi múi giờ. | Q02: chuẩn `Asia/Ho_Chi_Minh`; nhãn có thể giữ nguyên. |
| m9 | 13, 11, 20 | Một số cột bị cắt chữ ("Nguyễn Thị M…", "NV00…"). | Nới cột hoặc thêm tooltip. |

---

## 3. Quyết định đã chốt trong buổi thiết kế UI (cần chuyển vào BA Decision Register)

Các quyết định dưới đây **người dùng đã đồng ý trong buổi thiết kế**, nhưng theo brief vẫn là **ĐỀ XUẤT**, chưa phải ĐÃ DUYỆT ở cấp BA / PO:

1. Lịch "Chờ xác nhận" vẫn được tiếp nhận (→ Q16).
2. Công nợ chỉ phát sinh từ hóa đơn của lượt khám; bệnh nhân còn nợ phải có lượt khám (→ Q07).
3. Dị ứng nằm ở tab Tiền sử (Hồ sơ, Phiếu khám) (→ Q15).
4. Nút hủy và xóa nền đỏ, đặt ở chân khung; nút chưa hợp lệ thì làm mờ (quy ước UI, không cần BA).
5. Menu 6 nhóm nghiệp vụ, 21 màn hình (khớp UI-M1).

---

## 4. Dữ liệu mẫu tự đặt (không được coi là yêu cầu)

Mã BN000001–011; ngày sinh, địa chỉ; dị ứng Amoxicillin (Lan); tiền sử bệnh; giờ lịch hẹn chi tiết; người tạo lịch; phân công tuần; nội dung biên bản; tên tiêu chí đánh giá; mã số thuế / địa chỉ nhà cung cấp; lô PA1125; ghi chú trong nhật ký.

---

## 5. Câu hỏi cần PO / BA trả lời trước khi Codex code các màn hình này

1. Q04: đổi nhãn "Bệnh nhân chờ khám" thành "Phiếu chờ khám"?
2. Q03: badge menu đếm gì: chỉ việc chờ duyệt, hay cả việc khẩn?
3. Q05: "Lịch hẹn hôm nay" có tính lịch Đã hủy / Đã đổi lịch?
4. Q07: hóa đơn đang chờ duyệt hủy có tính công nợ?
5. Q10 / Q11 / Q12: ngưỡng sắp hết hạn, chu kỳ kiểm tra cấp cứu, cửa sổ tái khám là bao nhiêu và ai được cấu hình?
6. Q16: duyệt quy tắc tiếp nhận lịch "Chờ xác nhận"?
7. Q13: bảng Vai trò – Chức năng cho 21 màn hình (xem / tạo / sửa / hủy / duyệt), đặc biệt quyền duyệt của AD ở Kiểm kê, Duyệt hủy, Bàn giao.
8. Schema: `NhaCungCap` có cột trạng thái / mã số thuế / địa chỉ không? Tên 5 tiêu chí đánh giá là gì?

**Chưa thực hiện trong bản này:** nghiệm thu mã UI-M1, Q01 kiến trúc, AUTH-R1, hợp đồng UI-M2, backlog, prompt Codex. Lý do: cần quyền truy cập repository và bằng chứng kiểm thử.
