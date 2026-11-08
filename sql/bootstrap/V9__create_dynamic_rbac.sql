-- C2B schema-only foundation, source 0940bc2. NOT executed/validated on SQL Server.
-- Standalone transaction owner. Do not append to DevSetup's ambient transaction.
-- No account alteration, runtime wiring, SQL grants, triggers or mutation procedures.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF @@TRANCOUNT <> 0 THROW 51900, N'V9 yêu cầu connection không có transaction đang mở.', 1;
IF DATALENGTH(DB_NAME()) <> 30
   OR LEFT(DB_NAME(),7) COLLATE Latin1_General_100_BIN2 <> N'N08Dev_'
   OR SUBSTRING(DB_NAME(),8,8) COLLATE Latin1_General_100_BIN2 LIKE N'%[^0-9a-f]%'
    THROW 51901, N'Chỉ cho phép database N08Dev_<8 hex thường>.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lockResult int;
    EXEC @lockResult = sys.sp_getapplock @Resource=N'N08_SCHEMA_V9_DYNAMIC_RBAC',
        @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=5000;
    IF @lockResult IS NULL OR @lockResult < 0 THROW 51902, N'Không lấy được khóa migration V9.', 1;

    -- Named manifest: V1-V8 prerequisites, not a total table-count gate.
    DECLARE @RequiredTables TABLE(tableName sysname NOT NULL PRIMARY KEY);
    -- BEGIN REQUIRED TABLES
    INSERT @RequiredTables VALUES
        (N'BanGiaoCa'),
        (N'BenhNhan'),
        (N'BoDemMa'),
        (N'CaLamViec'),
        (N'ChiDinhCanLamSang'),
        (N'ChiTietDanhGia'),
        (N'ChiTietDieuChinhKho'),
        (N'ChiTietDieuTri'),
        (N'ChiTietDonThuoc'),
        (N'ChiTietHoaDon'),
        (N'ChiTietKiemTraCapCuu'),
        (N'ChiTietPhieuNhap'),
        (N'ChiTietPhieuXuat'),
        (N'ChiTietYeuCauCapPhat'),
        (N'DanhGiaNhanSu'),
        (N'DanhMucCapCuu'),
        (N'DichVu'),
        (N'DieuChinhThanhToan'),
        (N'DonThuoc'),
        (N'HoaDon'),
        (N'HoSoBenhAn'),
        (N'KeHoachDieuTri'),
        (N'KiemTraCapCuu'),
        (N'LanThuHoaDon'),
        (N'LichHen'),
        (N'LoHang'),
        (N'NhaCungCap'),
        (N'NhanVien'),
        (N'NhatKyThaoTac'),
        (N'PhanCong'),
        (N'PhienDangNhap'),
        (N'PhieuDieuChinhKho'),
        (N'PhieuKham'),
        (N'PhieuNhapKho'),
        (N'PhieuXuatKho'),
        (N'RefreshToken'),
        (N'TaiKhoan'),
        (N'ThamSoHeThong'),
        (N'ThanhToan'),
        (N'Thuoc'),
        (N'VatTu'),
        (N'YeuCauCapPhat'),
        (N'YeuCauHuyHoaDon');
    -- END REQUIRED TABLES
    IF EXISTS(SELECT 1 FROM @RequiredTables r WHERE OBJECT_ID(N'dbo.'+r.tableName,N'U') IS NULL)
        THROW 51903, N'Thiếu bảng bắt buộc V1-V8; không tự sửa schema.', 1;
    IF OBJECT_ID(N'dbo.DongTienHoaDon',N'V') IS NULL
        THROW 51904, N'Thiếu view dòng tiền V8.', 1;
    IF OBJECT_ID(N'dbo.VaiTro') IS NOT NULL OR OBJECT_ID(N'dbo.Quyen') IS NOT NULL
       OR OBJECT_ID(N'dbo.VaiTroQuyen') IS NOT NULL OR OBJECT_ID(N'dbo.RbacEpoch') IS NOT NULL
        THROW 51905, N'V9 đã tồn tại hoặc có schema một phần; dừng thay vì ghi đè.', 1;

    DECLARE @RequiredColumns TABLE(tableName sysname, columnName sysname, typeName sysname,
        maxLength smallint, precisionValue tinyint, scaleValue tinyint, nullable bit);
    INSERT @RequiredColumns VALUES
        (N'TaiKhoan',N'vaiTro',N'varchar',32,0,0,0),
        (N'Thuoc',N'donGiaBan',N'decimal',9,18,0,1),
        (N'HoaDon',N'tenBenhNhanChot',N'nvarchar',300,0,0,1),
        (N'HoaDon',N'soDienThoaiChot',N'varchar',20,0,0,1),
        (N'ChiTietHoaDon',N'tenHangChot',N'nvarchar',400,0,0,1);
    IF EXISTS(SELECT 1 FROM @RequiredColumns r WHERE NOT EXISTS(
        SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
        WHERE c.object_id=OBJECT_ID(N'dbo.'+r.tableName,N'U') AND c.name=r.columnName
          AND t.name=r.typeName AND c.max_length=r.maxLength AND c.is_nullable=r.nullable
          AND (r.precisionValue=0 OR (c.precision=r.precisionValue AND c.scale=r.scaleValue))))
        THROW 51906, N'Column contract V1/V7/V8 không khớp.', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.TaiKhoan')
        AND name=N'vaiTro' AND collation_name=N'Latin1_General_100_BIN2')
        THROW 51907, N'Collation mã vai trò cũ không khớp.', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.TaiKhoan')
        AND name=N'CK_TaiKhoan_VaiTro' AND is_disabled=0 AND is_not_trusted=0)
       OR NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.Thuoc')
        AND name=N'CK_Thuoc_DonGiaBan' AND is_disabled=0 AND is_not_trusted=0)
       OR NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.ThanhToan')
        AND name=N'CK_ThanhToan_TrangThai' AND is_disabled=0 AND is_not_trusted=0)
        THROW 51908, N'Constraint baseline bị thiếu, disabled hoặc không trusted.', 1;

    -- Require visible constraint metadata; do not parse engine-rewritten IN/OR text.
    -- Its definition and all account data are preserved, never replaced by V9.
    IF NOT EXISTS(SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id=OBJECT_ID(N'dbo.TaiKhoan') AND name=N'CK_TaiKhoan_VaiTro'
          AND definition IS NOT NULL)
        THROW 51909, N'Không đọc được metadata CHECK vai trò; dừng để review.', 1;
    IF EXISTS(SELECT 1 FROM dbo.TaiKhoan WHERE vaiTro NOT IN
        ('RECEPTIONIST','DOCTOR','NURSE','CLINIC_MANAGER','INVENTORY_MANAGER','SYSTEM_ADMIN'))
        THROW 51913, N'Có account ngoài sáu vai trò baseline; dừng để review.', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.LanThuHoaDon')
        AND name=N'UX_LanThu_DangXuLy' AND is_unique=1 AND has_filter=1 AND is_disabled=0)
        THROW 51910, N'Thiếu filtered unique index V8.', 1;
    DECLARE @RequiredTriggers TABLE(triggerName sysname PRIMARY KEY, tableName sysname);
    INSERT @RequiredTriggers VALUES
        (N'TR_HoaDon_ChotTen',N'HoaDon'),
        (N'TR_ChiTietHoaDon_ChotTen',N'ChiTietHoaDon'),
        (N'TR_KeHoach_KhoaSauHoaDon',N'KeHoachDieuTri');
    IF EXISTS(SELECT 1 FROM @RequiredTriggers r WHERE NOT EXISTS(SELECT 1 FROM sys.triggers t
        WHERE t.object_id=OBJECT_ID(N'dbo.'+r.triggerName,N'TR')
          AND t.parent_id=OBJECT_ID(N'dbo.'+r.tableName,N'U') AND t.is_disabled=0))
        THROW 51911, N'Thiếu trigger V8 hoặc trigger đã disabled.', 1;

    CREATE TABLE dbo.VaiTro (
        roleCode varchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL CONSTRAINT PK_VaiTro PRIMARY KEY,
        roleName nvarchar(100) NOT NULL,
        description nvarchar(500) NOT NULL,
        enabled bit NOT NULL,
        isSystem bit NOT NULL,
        isProtected bit NOT NULL,
        revision rowversion NOT NULL,
        CONSTRAINT CK_VaiTro_Code CHECK (
            DATALENGTH(roleCode) BETWEEN 1 AND 32
            AND DATALENGTH(roleCode)=DATALENGTH(TRIM(roleCode))
            AND LEFT(roleCode,1) COLLATE Latin1_General_100_BIN2 LIKE '[A-Z]'
            AND roleCode COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^A-Z0-9_]%'),
        -- Java String.isBlank whitespace set (excluding NBSP, which Java does not consider blank).
        CONSTRAINT CK_VaiTro_Name CHECK (roleName COLLATE Latin1_General_100_BIN2 LIKE
            N'%[^ '+NCHAR(9)+NCHAR(10)+NCHAR(11)+NCHAR(12)+NCHAR(13)
            +NCHAR(28)+NCHAR(29)+NCHAR(30)+NCHAR(31)+NCHAR(5760)
            +NCHAR(8192)+NCHAR(8193)+NCHAR(8194)+NCHAR(8195)+NCHAR(8196)+NCHAR(8197)+NCHAR(8198)
            +NCHAR(8200)+NCHAR(8201)+NCHAR(8202)+NCHAR(8232)+NCHAR(8233)+NCHAR(8287)+NCHAR(12288)+N']%'),
        CONSTRAINT CK_VaiTro_System CHECK (
            (roleCode IN ('RECEPTIONIST','DOCTOR','NURSE','CLINIC_MANAGER','INVENTORY_MANAGER','SYSTEM_ADMIN') AND isSystem=1)
            OR (roleCode NOT IN ('RECEPTIONIST','DOCTOR','NURSE','CLINIC_MANAGER','INVENTORY_MANAGER','SYSTEM_ADMIN') AND isSystem=0)),
        CONSTRAINT CK_VaiTro_Protected CHECK (
            (roleCode='SYSTEM_ADMIN' AND isSystem=1 AND isProtected=1 AND enabled=1)
            OR (roleCode<>'SYSTEM_ADMIN' AND isProtected=0))
    );
    CREATE TABLE dbo.Quyen (
        permissionCode varchar(64) COLLATE Latin1_General_100_BIN2 NOT NULL CONSTRAINT PK_Quyen PRIMARY KEY,
        permissionName nvarchar(120) NOT NULL,
        enabled bit NOT NULL,
        CONSTRAINT CK_Quyen_Code CHECK (DATALENGTH(permissionCode)=DATALENGTH(TRIM(permissionCode))
            AND permissionCode IN ('PATIENT_READ','PATIENT_CREATE','PATIENT_UPDATE','MEDICAL_RECORD_READ','MEDICAL_RECORD_UPDATE','APPOINTMENT_READ','APPOINTMENT_MANAGE','BILLING_READ','BILLING_MANAGE','INVENTORY_READ','INVENTORY_MANAGE','WORKFORCE_READ','WORKFORCE_MANAGE','ACCOUNT_MANAGE','ROLE_MANAGE','AUDIT_READ','PATIENT_CHECK_IN','REEXAM_SCHEDULE','SUPPLY_REQUEST','EMERGENCY_KIT_CHECK','SCHEDULE_SELF_READ','SERVICE_CATALOG_MANAGE','INVOICE_CANCEL_REQUEST','INVOICE_CANCEL_APPROVE','SHIFT_HANDOVER_MANAGE','REVENUE_REPORT_READ')),
        CONSTRAINT CK_Quyen_Name CHECK (LEN(TRIM(permissionName))>0)
    );
    CREATE TABLE dbo.VaiTroQuyen (
        roleCode varchar(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
        permissionCode varchar(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
        CONSTRAINT PK_VaiTroQuyen PRIMARY KEY(roleCode,permissionCode),
        CONSTRAINT FK_VaiTroQuyen_VaiTro FOREIGN KEY(roleCode) REFERENCES dbo.VaiTro(roleCode)
            ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT FK_VaiTroQuyen_Quyen FOREIGN KEY(permissionCode) REFERENCES dbo.Quyen(permissionCode)
            ON DELETE NO ACTION ON UPDATE NO ACTION
    );
    CREATE TABLE dbo.RbacEpoch (
        singletonId tinyint NOT NULL CONSTRAINT PK_RbacEpoch PRIMARY KEY,
        epoch bigint NOT NULL,
        revision rowversion NOT NULL,
        CONSTRAINT CK_RbacEpoch_Singleton CHECK(singletonId=1),
        CONSTRAINT CK_RbacEpoch_Positive CHECK(epoch>=1)
    );

    DECLARE @SeedRoles TABLE(roleCode varchar(32) COLLATE Latin1_General_100_BIN2 PRIMARY KEY,
        roleName nvarchar(100), description nvarchar(500), enabled bit, isSystem bit, isProtected bit);
    -- BEGIN SEED ROLES
    INSERT @SeedRoles VALUES
        ('RECEPTIONIST',N'Lễ tân',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,0),
        ('DOCTOR',N'Bác sĩ',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,0),
        ('NURSE',N'Y tá',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,0),
        ('CLINIC_MANAGER',N'Quản lý phòng khám',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,0),
        ('INVENTORY_MANAGER',N'Quản lý kho',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,0),
        ('SYSTEM_ADMIN',N'Quản trị hệ thống',N'Vai trò hệ thống giữ nguyên quyền tại baseline 0940bc2.',1,1,1);
    -- END SEED ROLES
    DECLARE @SeedPermissions TABLE(permissionCode varchar(64) COLLATE Latin1_General_100_BIN2 PRIMARY KEY,
        permissionName nvarchar(120), enabled bit);
    -- BEGIN SEED PERMISSIONS
    INSERT @SeedPermissions VALUES
        ('PATIENT_READ',N'Xem bệnh nhân',1),
        ('PATIENT_CREATE',N'Tạo bệnh nhân',1),
        ('PATIENT_UPDATE',N'Cập nhật bệnh nhân',1),
        ('MEDICAL_RECORD_READ',N'Xem hồ sơ bệnh án',1),
        ('MEDICAL_RECORD_UPDATE',N'Cập nhật hồ sơ bệnh án',1),
        ('APPOINTMENT_READ',N'Xem lịch hẹn',1),
        ('APPOINTMENT_MANAGE',N'Quản lý lịch hẹn',1),
        ('BILLING_READ',N'Xem hóa đơn',1),
        ('BILLING_MANAGE',N'Quản lý hóa đơn',1),
        ('INVENTORY_READ',N'Xem kho',1),
        ('INVENTORY_MANAGE',N'Quản lý kho',1),
        ('WORKFORCE_READ',N'Xem nhân sự',1),
        ('WORKFORCE_MANAGE',N'Quản lý nhân sự',1),
        ('ACCOUNT_MANAGE',N'Quản lý tài khoản',1),
        ('ROLE_MANAGE',N'Xem vai trò và quyền',1),
        ('AUDIT_READ',N'Xem nhật ký',1),
        ('PATIENT_CHECK_IN',N'Tiếp đón bệnh nhân',1),
        ('REEXAM_SCHEDULE',N'Đặt lịch tái khám',1),
        ('SUPPLY_REQUEST',N'Yêu cầu cấp phát',1),
        ('EMERGENCY_KIT_CHECK',N'Kiểm tra hộp cấp cứu',1),
        ('SCHEDULE_SELF_READ',N'Xem lịch cá nhân',1),
        ('SERVICE_CATALOG_MANAGE',N'Quản lý dịch vụ',1),
        ('INVOICE_CANCEL_REQUEST',N'Yêu cầu hủy hóa đơn',1),
        ('INVOICE_CANCEL_APPROVE',N'Duyệt hủy hóa đơn',1),
        ('SHIFT_HANDOVER_MANAGE',N'Bàn giao ca',1),
        ('REVENUE_REPORT_READ',N'Xem báo cáo doanh thu',1);
    -- END SEED PERMISSIONS
    DECLARE @SeedGrants TABLE(roleCode varchar(32) COLLATE Latin1_General_100_BIN2,
        permissionCode varchar(64) COLLATE Latin1_General_100_BIN2, PRIMARY KEY(roleCode,permissionCode));
    -- BEGIN SEED GRANTS
    INSERT @SeedGrants VALUES
        ('RECEPTIONIST','PATIENT_READ'),
        ('RECEPTIONIST','PATIENT_CREATE'),
        ('RECEPTIONIST','PATIENT_UPDATE'),
        ('RECEPTIONIST','PATIENT_CHECK_IN'),
        ('RECEPTIONIST','APPOINTMENT_READ'),
        ('RECEPTIONIST','APPOINTMENT_MANAGE'),
        ('RECEPTIONIST','BILLING_READ'),
        ('RECEPTIONIST','BILLING_MANAGE'),
        ('RECEPTIONIST','INVOICE_CANCEL_REQUEST'),
        ('RECEPTIONIST','SCHEDULE_SELF_READ'),
        ('RECEPTIONIST','SHIFT_HANDOVER_MANAGE'),
        ('DOCTOR','PATIENT_READ'),
        ('DOCTOR','MEDICAL_RECORD_READ'),
        ('DOCTOR','MEDICAL_RECORD_UPDATE'),
        ('DOCTOR','REEXAM_SCHEDULE'),
        ('DOCTOR','SUPPLY_REQUEST'),
        ('DOCTOR','SCHEDULE_SELF_READ'),
        ('DOCTOR','SHIFT_HANDOVER_MANAGE'),
        ('NURSE','SUPPLY_REQUEST'),
        ('NURSE','EMERGENCY_KIT_CHECK'),
        ('NURSE','SCHEDULE_SELF_READ'),
        ('NURSE','SHIFT_HANDOVER_MANAGE'),
        ('CLINIC_MANAGER','REVENUE_REPORT_READ'),
        ('CLINIC_MANAGER','BILLING_READ'),
        ('CLINIC_MANAGER','WORKFORCE_READ'),
        ('CLINIC_MANAGER','WORKFORCE_MANAGE'),
        ('CLINIC_MANAGER','ACCOUNT_MANAGE'),
        ('CLINIC_MANAGER','ROLE_MANAGE'),
        ('CLINIC_MANAGER','AUDIT_READ'),
        ('CLINIC_MANAGER','SERVICE_CATALOG_MANAGE'),
        ('CLINIC_MANAGER','INVOICE_CANCEL_APPROVE'),
        ('CLINIC_MANAGER','SCHEDULE_SELF_READ'),
        ('CLINIC_MANAGER','SHIFT_HANDOVER_MANAGE'),
        ('INVENTORY_MANAGER','INVENTORY_READ'),
        ('INVENTORY_MANAGER','INVENTORY_MANAGE'),
        ('INVENTORY_MANAGER','SCHEDULE_SELF_READ'),
        ('INVENTORY_MANAGER','SHIFT_HANDOVER_MANAGE'),
        ('SYSTEM_ADMIN','PATIENT_READ'),
        ('SYSTEM_ADMIN','PATIENT_CREATE'),
        ('SYSTEM_ADMIN','PATIENT_UPDATE'),
        ('SYSTEM_ADMIN','MEDICAL_RECORD_READ'),
        ('SYSTEM_ADMIN','MEDICAL_RECORD_UPDATE'),
        ('SYSTEM_ADMIN','APPOINTMENT_READ'),
        ('SYSTEM_ADMIN','APPOINTMENT_MANAGE'),
        ('SYSTEM_ADMIN','BILLING_READ'),
        ('SYSTEM_ADMIN','BILLING_MANAGE'),
        ('SYSTEM_ADMIN','INVENTORY_READ'),
        ('SYSTEM_ADMIN','INVENTORY_MANAGE'),
        ('SYSTEM_ADMIN','WORKFORCE_READ'),
        ('SYSTEM_ADMIN','WORKFORCE_MANAGE'),
        ('SYSTEM_ADMIN','ACCOUNT_MANAGE'),
        ('SYSTEM_ADMIN','ROLE_MANAGE'),
        ('SYSTEM_ADMIN','AUDIT_READ'),
        ('SYSTEM_ADMIN','PATIENT_CHECK_IN'),
        ('SYSTEM_ADMIN','REEXAM_SCHEDULE'),
        ('SYSTEM_ADMIN','SUPPLY_REQUEST'),
        ('SYSTEM_ADMIN','EMERGENCY_KIT_CHECK'),
        ('SYSTEM_ADMIN','SCHEDULE_SELF_READ'),
        ('SYSTEM_ADMIN','SERVICE_CATALOG_MANAGE'),
        ('SYSTEM_ADMIN','INVOICE_CANCEL_REQUEST'),
        ('SYSTEM_ADMIN','INVOICE_CANCEL_APPROVE'),
        ('SYSTEM_ADMIN','SHIFT_HANDOVER_MANAGE'),
        ('SYSTEM_ADMIN','REVENUE_REPORT_READ');
    -- END SEED GRANTS
    INSERT dbo.VaiTro(roleCode,roleName,description,enabled,isSystem,isProtected)
        SELECT roleCode,roleName,description,enabled,isSystem,isProtected FROM @SeedRoles;
    INSERT dbo.Quyen(permissionCode,permissionName,enabled)
        SELECT permissionCode,permissionName,enabled FROM @SeedPermissions;
    INSERT dbo.VaiTroQuyen(roleCode,permissionCode) SELECT roleCode,permissionCode FROM @SeedGrants;
    INSERT dbo.RbacEpoch(singletonId,epoch) VALUES(1,1);
    IF (SELECT COUNT(*) FROM dbo.VaiTro)<>6 OR (SELECT COUNT(*) FROM dbo.Quyen)<>26
       OR (SELECT COUNT(*) FROM dbo.VaiTroQuyen)<>63
       OR EXISTS(SELECT roleCode,roleName,description,enabled,isSystem,isProtected FROM @SeedRoles
                 EXCEPT SELECT roleCode,roleName,description,enabled,isSystem,isProtected FROM dbo.VaiTro)
       OR EXISTS(SELECT roleCode,roleName,description,enabled,isSystem,isProtected FROM dbo.VaiTro
                 EXCEPT SELECT roleCode,roleName,description,enabled,isSystem,isProtected FROM @SeedRoles)
       OR EXISTS(SELECT permissionCode,permissionName,enabled FROM @SeedPermissions
                 EXCEPT SELECT permissionCode,permissionName,enabled FROM dbo.Quyen)
       OR EXISTS(SELECT permissionCode,permissionName,enabled FROM dbo.Quyen
                 EXCEPT SELECT permissionCode,permissionName,enabled FROM @SeedPermissions)
       OR EXISTS(SELECT roleCode,permissionCode FROM @SeedGrants
                 EXCEPT SELECT roleCode,permissionCode FROM dbo.VaiTroQuyen)
       OR EXISTS(SELECT roleCode,permissionCode FROM dbo.VaiTroQuyen
                 EXCEPT SELECT roleCode,permissionCode FROM @SeedGrants)
        THROW 51912, N'Seed không khớp snapshot 6/26/63; rollback toàn migration.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
-- No post-commit work. A second application fails closed, never overwrites existing catalog.
-- C3: CAS parent role revision, replace grants, increment epoch + audit atomically.
-- No SQL execution evidence yet: failure injection/DDL rollback requires approved isolated DB.
