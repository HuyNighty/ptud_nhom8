-- Local development: NULL means not priced; 0 is an explicitly free medicine.
SET XACT_ABORT ON;
IF LEN(DB_NAME()) <> 15 OR DB_NAME() COLLATE Latin1_General_100_BIN2
    NOT LIKE N'N08Dev[_][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'
    THROW 51120, 'Expected isolated local N08Dev database.', 1;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.Thuoc','donGiaBan') IS NULL
    ALTER TABLE dbo.Thuoc ADD donGiaBan DECIMAL(18,0) NULL;
IF OBJECT_ID('dbo.CK_Thuoc_DonGiaBan','C') IS NULL
    EXEC(N'ALTER TABLE dbo.Thuoc WITH CHECK ADD CONSTRAINT CK_Thuoc_DonGiaBan CHECK (donGiaBan IS NULL OR donGiaBan>=0)');
COMMIT TRANSACTION;
