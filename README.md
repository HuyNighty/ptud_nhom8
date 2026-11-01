# Quản lý phòng khám nha khoa — Nhóm 8

> Ứng dụng desktop phục vụ môn **Phát triển ứng dụng**, xây dựng bằng **Java Swing, JDBC và Microsoft SQL Server**.
>
> **Trạng thái:** Dự án đang phát triển và tích hợp theo từng module. Các màn hình đã có mã nguồn không đồng nghĩa đã được nghiệm thu end-to-end trên mọi máy.

## 1. Giới thiệu

Hệ thống hỗ trợ quản lý các nghiệp vụ của phòng khám: nhân sự và tài khoản; bệnh nhân; lịch hẹn và tiếp nhận; hồ sơ và hàng chờ khám; danh mục dịch vụ; hóa đơn, thanh toán, công nợ; danh mục kho; và nhật ký thao tác.

**Công nghệ chính**

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java (mức biên dịch Java 21) |
| Giao diện | Java Swing |
| Truy cập dữ liệu | JDBC + Microsoft SQL Server |
| Quản lý build | Maven Wrapper (`mvnw.cmd`) |
| Mật khẩu ứng dụng | BCrypt |
| Kiến trúc | Java desktop; phân chia theo module và tầng View / Controller / Service / DAO |

Dự án hiện tại **không sử dụng Spring Boot, Hibernate/JPA hoặc Flyway trong runtime desktop**.

## 2. Các chức năng

Các module sau đã được đăng ký trong `FeatureBootstrap` trên nhánh `main`:

| Nhóm | Chức năng đã có màn hình tích hợp |
|---|---|
| Hệ thống | Tổng quan; tài khoản; nhân viên; vai trò; nhật ký thao tác |
| Bệnh nhân | Danh sách bệnh nhân; hồ sơ bệnh án |
| Lịch hẹn & tiếp nhận | Lịch hẹn; xác nhận lịch; tiếp nhận và check-in |
| Khám chữa bệnh | Hàng chờ / phiếu khám |
| Dịch vụ & tài chính | Danh mục dịch vụ; hóa đơn; thanh toán; công nợ; duyệt hủy hóa đơn |
| Kho | Thuốc; vật tư; nhà cung cấp; lô hàng (cùng trong `InventoryPanel`) |

## 3. Cấu trúc repository

```text
ptud_nhom8/
├── src/main/java/com/quanlynhakhoa/
│   ├── App.java               # Điểm khởi động
│   ├── view/                  # Giao diện chính và đăng nhập
│   ├── features/              # Registry tích hợp module
│   ├── security/              # Phiên đăng nhập và phân quyền
│   ├── auth/                  # Xác thực JDBC
│   ├── dev/                   # Kết nối/setup database phát triển
│   ├── benhnhan/              # Bệnh nhân và hồ sơ
│   ├── lichhen/               # Lịch hẹn và tiếp nhận
│   ├── examination/           # Hàng chờ khám
│   ├── billing/               # Hóa đơn, thanh toán, công nợ
│   ├── inventory/             # Thuốc, vật tư, NCC, lô hàng
│   ├── audit/                 # Nhật ký thao tác
│   └── ...                    # Các module khác
├── src/test/java/             # Synthetic SelfTest và test hỗ trợ
├── sql/bootstrap/             # Các script schema V1–V5
├── scripts/                   # Setup, chạy và kiểm thử
├── docs/                      # Tài liệu nhóm
├── pom.xml
└── README.md
```

**Repository:** https://github.com/HuyNighty/ptud_nhom8  
**Nhóm thực hiện:** Nhóm 8 - Phát triển ứng dụng.
