Quản lý phòng khám nha khoa — Nhóm 8
Ứng dụng desktop phục vụ đồ án Phát triển ứng dụng, xây dựng bằng Java Swing, JDBC và Microsoft SQL Server.

Trạng thái: Dự án đang phát triển và tích hợp theo từng module. Các màn hình đã có mã nguồn không đồng nghĩa đã được nghiệm thu end-to-end trên mọi máy.

1. Giới thiệu
   Hệ thống hỗ trợ quản lý các nghiệp vụ của phòng khám: nhân sự và tài khoản; bệnh nhân; lịch hẹn và tiếp nhận; hồ sơ và hàng chờ khám; danh mục dịch vụ; hóa đơn, thanh toán, công nợ; danh mục kho; và nhật ký thao tác.

Công nghệ chính
Thành phần	Công nghệ
Ngôn ngữ	Java (mức biên dịch Java 21)
Giao diện	Java Swing
Truy cập dữ liệu	JDBC + Microsoft SQL Server
Quản lý build	Maven Wrapper (mvnw.cmd)
Mật khẩu ứng dụng	BCrypt
Kiến trúc	Java desktop; phân chia theo module và tầng View / Controller / Service / DAO


Dự án hiện tại không sử dụng Spring Boot, Hibernate/JPA hoặc Flyway trong runtime desktop.
2. Các chức năng
   Các module sau đã được đăng ký trong FeatureBootstrap trên nhánh main:
   Nhóm	Chức năng đã có màn hình tích hợp
   Hệ thống	Tổng quan; tài khoản; nhân viên; vai trò; nhật ký thao tác
   Bệnh nhân	Danh sách bệnh nhân; hồ sơ bệnh án
   Lịch hẹn & tiếp nhận	Lịch hẹn; xác nhận lịch; tiếp nhận và check-in
   Khám chữa bệnh	Hàng chờ / phiếu khám
   Dịch vụ & tài chính	Danh mục dịch vụ; hóa đơn; thanh toán; công nợ; duyệt hủy hóa đơn
   Kho	Thuốc; vật tư; nhà cung cấp; lô hàng (cùng trong InventoryPanel)


Đang hoàn thiện hoặc chưa có màn hình nghiệp vụ tích hợp: lịch làm việc của tôi, yêu cầu cấp phát, kiểm tra hộp cấp cứu; một số thao tác khác vẫn hiển thị “Sắp triển khai”. Tính khả dụng của từng màn hình phụ thuộc vai trò đăng nhập, quyền SQL và dữ liệu phát triển.
3. Chạy nhanh trên máy đã thiết lập
   Yêu cầu: Windows, JDK 21 trở lên (khuyến nghị JDK 21), PowerShell, SQL Server đang chạy và file cấu hình cục bộ .n08-dev.local.properties đã được tạo trên chính máy này.

Mở PowerShell tại thư mục chứa pom.xml:
# Kiểm tra kết nối JDBC tới database phát triển của máy
.\scripts\check-team-dev.ps1

# Chạy giao diện
.\scripts\run.ps1
Hoặc mở project bằng IntelliJ IDEA và chạy class com.quanlynhakhoa.App. Working directory phải là thư mục gốc project, nơi có pom.xml và .n08-dev.local.properties.

Tài khoản đăng nhập ứng dụng trên database được tạo bằng setup-local.ps1:
- Tên đăng nhập: dev.admin
- Mật khẩu: mật khẩu ứng dụng do thành viên tự đặt lúc chạy setup (12–72 byte UTF-8).
  Chưa có chế độ admin/admin hoặc run-demo.ps1 trong bản main được kiểm tra. Không dùng admin/admin để đăng nhập vào database thật; nếu triển khai chế độ Demo sau này, chế độ đó cần tách biệt với JDBC và dữ liệu thật.
4. Cài đặt lần đầu cho thành viên mới
   Bước 1 — Chuẩn bị
1. Cài JDK 21, Git và IntelliJ IDEA (hoặc dùng PowerShell).
2. Cài SQL Server Developer/Express trên máy, hoặc chạy SQL Server trong Docker; bật kết nối TCP/IP và xác định cổng thực tế (ví dụ 1433, 14333, hoặc cổng do SQL Server cấu hình).
3. Chuẩn bị tài khoản quản trị SQL Server cục bộ đủ quyền tạo database và SQL login, thường là sa. Không gửi mật khẩu này cho các thành viên khác.
   Bước 2 — Tải source và build
   git clone https://github.com/HuyNighty/ptud_nhom8.git
   cd ptud_nhom8
   .\mvnw.cmd clean compile
   Bước 3 — Tạo database phát triển riêng (chỉ một lần)
# Thay 1433 bằng cổng SQL Server thật của máy
.\scripts\setup-local.ps1 -Port 1433 -SqlAdminUser sa
Script yêu cầu xác nhận CREATE, mật khẩu quản trị SQL và mật khẩu ứng dụng dev.admin do em tự chọn. Khi thành công, script sẽ:
- Tạo database riêng có tên N08Dev_XXXXXXXX và SQL login n08dev_XXXXXXXX.
- Khởi tạo schema V1–V5 và tài khoản ứng dụng dev.admin trong database phát triển.
- Tự sinh mật khẩu JDBC riêng, kiểm tra đăng nhập thật và lưu cấu hình vào file .n08-dev.local.properties (đã được Git bỏ qua).
  Không tự ý chạy lại setup-local.ps1 nếu đã có cấu hình. Script không được thiết kế để ghi đè database đang tồn tại. Không chạy setup trên database N08Dental hoặc dữ liệu bệnh nhân thật.
  Bước 4 — Kiểm tra và mở ứng dụng
  .\scripts\check-team-dev.ps1
  .\scripts\run.ps1
  Khi kết nối thành công, sử dụng dev.admin cùng mật khẩu Swing đã chọn ở Bước 3.

Hướng dẫn chi tiết: [TEAM_LOCAL_ONBOARDING.md](docs/TEAM_LOCAL_ONBOARDING.md).
5. Ba loại tài khoản không được nhầm
   Tài khoản	Vai trò
   sa (hoặc SQL administrator khác)	Chỉ dùng để thiết lập/quản trị SQL Server cục bộ
   n08dev_XXXXXXXX	JDBC kết nối database N08Dev_XXXXXXXX; mật khẩu nằm trong dev.password do setup sinh
   dev.admin	Đăng nhập giao diện Swing bằng mật khẩu ứng dụng do người dùng tự đặt


dev.password không phải mật khẩu sa và cũng không phải mật khẩu Swing dev.admin.

Mỗi máy có thể có database và file cấu hình khác nhau. Không copy file .n08-dev.local.properties giữa các máy, không commit hoặc đăng công khai.
6. Kiểm thử và xử lý lỗi
   Build và smoke không cần database
   .\mvnw.cmd clean test-compile
   .\scripts\run.ps1 -Smoke
   test-compile chỉ biên dịch test, không có nghĩa mọi SelfTest đã chạy. Có thể chạy từng test theo nhu cầu, ví dụ:
   java -cp "target\classes;target\test-classes" com.quanlynhakhoa.features.FeatureRegistrySelfTest
   java -cp "target\classes;target\test-classes" com.quanlynhakhoa.audit.AuditSelfTest
   java -cp "target\classes;target\test-classes" com.quanlynhakhoa.examination.ExamQueueSelfTest
   Các lỗi thường gặp
   Biểu hiện	Cách kiểm tra
   Không kết nối được SQL Server	Xác nhận SQL Server đang chạy, đúng TCP port, thử Test-NetConnection 127.0.0.1 -Port <PORT>
   SQL Error 18456	Kiểm tra SQL login n08dev_*, đúng mật khẩu JDBC dev.password, và SQL Server Error Log; không đoán là sai mật khẩu dev.admin
   SID_MISMATCH hoặc không mở được database	Kiểm tra ánh xạ SQL login/database user và quyền truy cập bằng SSMS
   Thiếu .n08-dev.local.properties	Kiểm tra đúng worktree/working directory; nếu là máy mới thì làm setup một lần
   Mở màn hình nhưng lỗi quyền SQL	Kiểm tra grant của module và hướng dẫn trong docs/TEAM_LOCAL_ONBOARDING.md; chỉ chạy script -Apply khi đã hiểu phạm vi quyền
   Mật khẩu Swing không đúng	Dùng mật khẩu tự đặt khi setup, không phải mật khẩu sa hoặc n08dev_*
   Mục menu hiện nhưng chưa có chức năng	Module có thể mới có menu/placeholder; kiểm tra FeatureBootstrap và tình trạng tích hợp


Quan trọng: Một số script cấp quyền cũ chưa bao phủ mọi SQL statement của các module mới, đặc biệt khi tích hợp Inventory. Cần rà soát quyền theo module; không tự cấp db_owner, DELETE hay quyền rộng chỉ để vượt qua lỗi.
7. Cấu trúc repository
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
8. Quy trình làm việc nhóm
- Mỗi thành viên tạo branch hoặc worktree riêng từ origin/main; không sửa trực tiếp main khi đang phát triển tính năng.
- Commit theo phạm vi tính năng, tạo Pull Request để nhóm review trước khi merge.
- Giữ đúng tác giả của code; không nhận thay công của thành viên khác.
- Trước merge: kiểm tra xung đột, chạy build + test liên quan, kiểm tra quyền SQL và luồng tương tác thật nếu chức năng có ghi dữ liệu.
- Không commit mật khẩu, file cấu hình riêng, dữ liệu bệnh nhân thật hoặc tài liệu nội bộ nhạy cảm.
9. Phạm vi hiện tại và kế hoạch
   Đây là đồ án đang phát triển, chưa phải sản phẩm dành cho môi trường y tế vận hành thật. Việc module đã có giao diện không đảm bảo tất cả thao tác đã được kiểm thử end-to-end.
