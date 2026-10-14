
-- N08 - Flyway V2
-- Session and Refresh Token Foundation
-- SQL Server 2022
-- All timestamps are UTC, supplied by the application.

CREATE TABLE dbo.PhienDangNhap (
    sessionId UNIQUEIDENTIFIER NOT NULL,
    maTaiKhoan VARCHAR(10) NOT NULL,

    kenh VARCHAR(8)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    taoLuc DATETIME2(3) NOT NULL,
    hoatDongCuoiLuc DATETIME2(3) NOT NULL,
    hetHanLuc DATETIME2(3) NOT NULL,

    thuHoiLuc DATETIME2(3) NULL,
    lyDoThuHoi VARCHAR(32) NULL,

    CONSTRAINT PK_PhienDangNhap
        PRIMARY KEY (sessionId),

    CONSTRAINT FK_PhienDangNhap_TaiKhoan
        FOREIGN KEY (maTaiKhoan)
        REFERENCES dbo.TaiKhoan(maTaiKhoan),

    CONSTRAINT CK_PhienDangNhap_Kenh
        CHECK (kenh IN ('API', 'DESKTOP')),

    CONSTRAINT CK_PhienDangNhap_ThoiGian
        CHECK (
            hetHanLuc > taoLuc
            AND hoatDongCuoiLuc >= taoLuc
            AND hoatDongCuoiLuc <= hetHanLuc
            AND (
                thuHoiLuc IS NULL
                OR thuHoiLuc >= taoLuc
            )
        ),

    CONSTRAINT CK_PhienDangNhap_ThuHoi
        CHECK (
            (
                thuHoiLuc IS NULL
                AND lyDoThuHoi IS NULL
            )
            OR
            (
                thuHoiLuc IS NOT NULL
                AND lyDoThuHoi IS NOT NULL
                AND LEN(TRIM(lyDoThuHoi)) > 0
            )
        )
);

CREATE INDEX IX_PhienDangNhap_TaiKhoan
ON dbo.PhienDangNhap (
    maTaiKhoan,
    thuHoiLuc,
    hetHanLuc
);

CREATE TABLE dbo.RefreshToken (
    tokenId UNIQUEIDENTIFIER NOT NULL,
    sessionId UNIQUEIDENTIFIER NOT NULL,

    tokenHash BINARY(32) NOT NULL,

    phatHanhLuc DATETIME2(3) NOT NULL,
    hetHanLuc DATETIME2(3) NOT NULL,

    daSuDungLuc DATETIME2(3) NULL,
    thuHoiLuc DATETIME2(3) NULL,

    tokenThayTheId UNIQUEIDENTIFIER NULL,

    CONSTRAINT PK_RefreshToken
        PRIMARY KEY (tokenId),

    CONSTRAINT UQ_RefreshToken_Hash
        UNIQUE (tokenHash),

    CONSTRAINT FK_RefreshToken_Phien
        FOREIGN KEY (sessionId)
        REFERENCES dbo.PhienDangNhap(sessionId),

    CONSTRAINT UQ_RefreshToken_SessionToken
        UNIQUE (sessionId, tokenId),

    CONSTRAINT FK_RefreshToken_ThayTheCungPhien
        FOREIGN KEY (sessionId, tokenThayTheId)
        REFERENCES dbo.RefreshToken(sessionId, tokenId),

    CONSTRAINT CK_RefreshToken_ThoiGian
        CHECK (
            hetHanLuc > phatHanhLuc
            AND (
                daSuDungLuc IS NULL
                OR (
                    daSuDungLuc >= phatHanhLuc
                    AND daSuDungLuc <= hetHanLuc
                )
            )
            AND (
                thuHoiLuc IS NULL
                OR thuHoiLuc >= phatHanhLuc
            )
        ),

    CONSTRAINT CK_RefreshToken_ThayThe
        CHECK (
            tokenThayTheId IS NULL
            OR (
                daSuDungLuc IS NOT NULL
                AND tokenThayTheId <> tokenId
            )
        )
);

CREATE UNIQUE INDEX UX_RefreshToken_OneCurrent
ON dbo.RefreshToken (sessionId)
WHERE daSuDungLuc IS NULL
  AND thuHoiLuc IS NULL;

CREATE UNIQUE INDEX UX_RefreshToken_Replacement
ON dbo.RefreshToken (tokenThayTheId)
WHERE tokenThayTheId IS NOT NULL;

CREATE INDEX IX_RefreshToken_Session_Issued
ON dbo.RefreshToken (
    sessionId,
    phatHanhLuc DESC
);
