
-- N08 - Flyway V3
-- Harden account lifecycle constraints.
-- Do not modify previously applied V1 or V2.

ALTER TABLE dbo.TaiKhoan WITH CHECK
ADD CONSTRAINT CK_TaiKhoan_KhoaNguon_Strict
CHECK (
    trangThai <> 'BI_KHOA'
    OR (
        nguonKhoa IS NOT NULL
        AND nguonKhoa IN (
            'FAILED_LOGIN',
            'ADMIN_ACTION'
        )
    )
);

ALTER TABLE dbo.TaiKhoan WITH CHECK
ADD CONSTRAINT CK_TaiKhoan_LyDoKhoa_NotBlank
CHECK (
    trangThai <> 'BI_KHOA'
    OR (
        lyDoKhoa IS NOT NULL
        AND LEN(TRIM(lyDoKhoa)) > 0
    )
);

ALTER TABLE dbo.TaiKhoan WITH CHECK
ADD CONSTRAINT CK_TaiKhoan_LyDoVoHieu_NotBlank
CHECK (
    trangThai <> 'VO_HIEU'
    OR (
        lyDoVoHieu IS NOT NULL
        AND LEN(TRIM(lyDoVoHieu)) > 0
    )
);
