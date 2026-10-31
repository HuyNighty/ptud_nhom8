[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Apply,
    [string]$SqlAdminUser = 'sa',
    [switch]$WindowsAuth
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Check -eq $Apply) { throw 'Chon dung mot che do: -Check hoac -Apply.' }
if ($SqlAdminUser -notmatch '^[a-zA-Z0-9._-]{1,60}$') { throw 'Ten tai khoan quan tri SQL khong hop le.' }

$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = Join-Path $repo '.n08-dev.local.properties'
if (!(Test-Path -LiteralPath (Join-Path $repo 'pom.xml')) -or !(Test-Path -LiteralPath $configPath)) {
    throw 'Chua co .n08-dev.local.properties. Chay scripts\setup-local.ps1 truoc.'
}

$settings = @{}
foreach ($line in [IO.File]::ReadAllLines($configPath)) {
    foreach ($key in @('dev.database', 'dev.port', 'dev.user')) {
        if ($line.StartsWith($key + '=')) { $settings[$key] = $line.Substring($key.Length + 1).Trim() }
    }
}
$database = $settings['dev.database']
$appUser = $settings['dev.user']
if ($database -notmatch '^N08Dev_[0-9a-f]{8}$' -or
    $appUser -cne ('n08dev_' + $database.Substring(7)) -or
    $settings['dev.port'] -notmatch '^\d{1,5}$') { throw 'Cau hinh database dev khong hop le.' }
$port = [int]$settings['dev.port']

Add-Type -AssemblyName System.Data
$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder['Data Source'] = '127.0.0.1,' + $port
$builder['Initial Catalog'] = $database
$builder['Connect Timeout'] = 8
$builder['Application Name'] = 'N08-Dev-Grant-All'
if ($WindowsAuth) {
    $builder['Integrated Security'] = $true
} else {
    $secure = Read-Host ('Mat khau SQL cua ' + $SqlAdminUser + ' (an)') -AsSecureString
    $builder['User ID'] = $SqlAdminUser
    $builder['Password'] = (New-Object System.Net.NetworkCredential('local', $secure)).Password
}
$admin = New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)
$builder['Password'] = ''
$transaction = $null
$committed = $false

function Invoke-Scalar([string]$Sql) {
    $command = $admin.CreateCommand()
    try {
        $command.CommandText = $Sql
        $command.CommandTimeout = 15
        if ($null -ne $transaction) { $command.Transaction = $transaction }
        return $command.ExecuteScalar()
    } finally { $command.Dispose() }
}

function Get-AppPermission([string]$Table, [string]$Privilege, [string]$Column) {
    $command = $admin.CreateCommand()
    try {
        $command.CommandText = @"
DECLARE @allowed int;
EXECUTE AS USER = '$appUser';
BEGIN TRY
  SET @allowed = CASE WHEN @column IS NULL
    THEN HAS_PERMS_BY_NAME(@object, 'OBJECT', @privilege)
    ELSE HAS_PERMS_BY_NAME(@object, 'OBJECT', @privilege, @column, 'COLUMN') END;
  REVERT;
END TRY
BEGIN CATCH
  REVERT;
  THROW;
END CATCH;
SELECT COALESCE(@allowed, 0);
"@
        $command.CommandTimeout = 15
        if ($null -ne $transaction) { $command.Transaction = $transaction }
        [void]$command.Parameters.Add('@object', [Data.SqlDbType]::NVarChar, 256)
        $command.Parameters['@object'].Value = 'dbo.' + $Table
        [void]$command.Parameters.Add('@privilege', [Data.SqlDbType]::VarChar, 30)
        $command.Parameters['@privilege'].Value = $Privilege
        [void]$command.Parameters.Add('@column', [Data.SqlDbType]::NVarChar, 128)
        if ([string]::IsNullOrEmpty($Column)) {
            $command.Parameters['@column'].Value = [DBNull]::Value
        } else {
            $command.Parameters['@column'].Value = $Column
        }
        return [int]$command.ExecuteScalar()
    } finally { $command.Dispose() }
}

$required = @()
foreach ($table in @('BenhNhan','HoSoBenhAn','LichHen','PhieuKham','NhanVien','TaiKhoan','PhanCong','CaLamViec',
    'DichVu','HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','KeHoachDieuTri','ChiTietDieuTri',
    'ChiDinhCanLamSang','Thuoc','VatTu')) {
    $required += [pscustomobject]@{ Table=$table; Privilege='SELECT'; Column=$null }
}
foreach ($table in @('BenhNhan','HoSoBenhAn','LichHen','PhieuKham','NhanVien','TaiKhoan','DichVu',
    'HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','NhatKyThaoTac')) {
    $required += [pscustomobject]@{ Table=$table; Privilege='INSERT'; Column=$null }
}
$updateColumns = [ordered]@{
    'BenhNhan'        = @('hoTen','ngaySinh','gioiTinh','soDienThoai','diaChi','nguoiLienHe')
    'HoSoBenhAn'      = @('tienSuBenh','diUng')
    'LichHen'         = @('trangThai','lyDoHuy')
    'DichVu'          = @('tenDichVu','donGia','moTa','laCanLamSang','dangApDung')
    'HoaDon'          = @('trangThai')
    'YeuCauHuyHoaDon' = @('trangThai','maTaiKhoanDuyet','thoiGianDuyet','lyDoDuyet')
    'TaiKhoan'        = @('trangThai','soLanSai','lanSaiDau','khoaLuc','khoaDen','lanDangNhapCuoi','capNhatLuc','lyDoKhoa','nguonKhoa')
}
foreach ($table in $updateColumns.Keys) {
    foreach ($column in $updateColumns[$table]) {
        $required += [pscustomobject]@{ Table=$table; Privilege='UPDATE'; Column=$column }
    }
}

try {
    $admin.Open()
    if ((Invoke-Scalar 'SELECT DB_NAME()') -cne $database) { throw 'Ket noi sai database.' }
    if ([int](Invoke-Scalar "SELECT HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'CONTROL')") -ne 1) { throw 'Tai khoan quan tri khong co quyen tren database dev.' }
    if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo')") -ne 39) { throw 'Database dev khong dung 39 bang cua V1-V5.' }
    if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.database_principals WHERE name='$appUser' AND type='S'") -ne 1) { throw 'Khong tim thay user ung dung trong database.' }
    if ([int](Invoke-Scalar "SELECT IS_ROLEMEMBER('db_owner', '$appUser')") -ne 0) { throw 'User ung dung khong duoc la db_owner.' }

    $missingObjects = @()
    foreach ($permission in $required) {
        if ([int](Invoke-Scalar ("SELECT COUNT(*) FROM sys.tables WHERE object_id=OBJECT_ID('dbo." + $permission.Table + "')")) -ne 1) {
            $missingObjects += ('bang dbo.' + $permission.Table)
            continue
        }
        if ($permission.Column -and [int](Invoke-Scalar ("SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo." + $permission.Table + "') AND name='" + $permission.Column + "'")) -ne 1) {
            $missingObjects += ('cot dbo.' + $permission.Table + '.' + $permission.Column)
        }
    }
    if ($missingObjects.Count -gt 0) {
        throw ('Database dev thieu: ' + ($missingObjects -join ', ') + '. Kiem tra lai V1-V5.')
    }

    $changed = @()
    if ($Apply) { $transaction = $admin.BeginTransaction() }
    foreach ($permission in $required) {
        $label = 'dbo.' + $permission.Table
        if ($permission.Column) { $label = $label + '.' + $permission.Column }
        $label = $label + ' / ' + $permission.Privilege
        if ((Get-AppPermission $permission.Table $permission.Privilege $permission.Column) -eq 1) { continue }
        Write-Output ('THIEU: ' + $label)
        if ($Apply) {
            $columnClause = ''
            if ($permission.Column) { $columnClause = ' ([' + $permission.Column + '])' }
            $sql = 'GRANT ' + $permission.Privilege + ' ON OBJECT::[dbo].[' + $permission.Table + ']' + $columnClause + ' TO [' + $appUser + ']'
            [void](Invoke-Scalar $sql)
            if ((Get-AppPermission $permission.Table $permission.Privilege $permission.Column) -ne 1) { throw ('Cap quyen khong thanh cong: ' + $label) }
            $changed += $label
        }
    }

    foreach ($table in @('BenhNhan','HoSoBenhAn','LichHen','PhieuKham','HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','NhatKyThaoTac','TaiKhoan')) {
        if ((Get-AppPermission $table 'DELETE' '') -ne 0) { throw ('User ung dung dang co quyen DELETE tren ' + $table + ', can kiem tra lai.') }
    }
    if ([int](Invoke-Scalar "SELECT IS_ROLEMEMBER('db_owner', '$appUser')") -ne 0) { throw 'User ung dung khong duoc la db_owner.' }

    if ($Apply) {
        $transaction.Commit()
        $committed = $true
        $transaction.Dispose()
        $transaction = $null
        Write-Output ('XONG: da cap ' + $changed.Count + ' quyen con thieu. Khong doi du lieu, khong doi cau truc bang.')
    } else {
        Write-Output 'KIEM TRA XONG: khong thay doi gi. Neu co dong THIEU thi chay lai voi -Apply.'
    }
} catch {
    if ($null -ne $transaction -and !$committed) {
        try { $transaction.Rollback() } catch { Write-Warning 'Khong xac nhan duoc rollback. Chay -Check truoc khi thu lai.' }
    }
    throw
} finally {
    if ($null -ne $transaction) { $transaction.Dispose() }
    $admin.Dispose()
}
