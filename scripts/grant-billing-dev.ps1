[CmdletBinding()]
param([switch]$Check, [switch]$Apply)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Check -eq $Apply) { throw 'Choose exactly one mode: -Check or -Apply.' }
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = Join-Path $repo '.n08-dev.local.properties'
if (!(Test-Path -LiteralPath (Join-Path $repo 'pom.xml')) -or !(Test-Path -LiteralPath $configPath)) {
    throw 'Run against the existing configured Maven development worktree.'
}

# Read target identifiers only. Never use or output the application password here.
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
    $settings['dev.port'] -notmatch '^\d{1,5}$') { throw 'Invalid isolated development target.' }
$port = [int]$settings['dev.port']
if ($port -lt 1 -or $port -gt 65535) { throw 'Invalid local SQL port.' }

# Match the configured TCP endpoint to its local instance before using shared memory.
# This avoids SSPI/SPN issues with TCP loopback while preserving the exact DB target.
$instances = Get-ItemProperty -LiteralPath 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL'
$instanceName = $null
foreach ($instance in $instances.PSObject.Properties) {
    if ($instance.Name -like 'PS*') { continue }
    $tcp = Get-ItemProperty -LiteralPath ('HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\' + $instance.Value + '\MSSQLServer\SuperSocketNetLib\Tcp\IPAll') -ErrorAction SilentlyContinue
    if ($null -eq $tcp) { continue }
    if ($tcp.TcpPort -eq [string]$port -or $tcp.TcpDynamicPorts -eq [string]$port) {
        if ($null -ne $instanceName) { throw 'Ambiguous development SQL instance.' }
        $instanceName = $instance.Name
    }
}
if ($null -eq $instanceName -or $instanceName -notmatch '^[a-zA-Z0-9_]+$') { throw 'No matching local SQL instance.' }
$server = if ($instanceName -eq 'MSSQLSERVER') { 'lpc:.' } else { 'lpc:.\' + $instanceName }
Add-Type -AssemblyName System.Data
$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder['Data Source'] = $server
$builder['Initial Catalog'] = $database
$builder['Integrated Security'] = $true
$builder['Connect Timeout'] = 8
$builder['Application Name'] = 'N08-Billing-Dev-Permissions'
$admin = New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)
$transaction = $null
$committed = $false

function Invoke-Scalar([string]$Sql) {
    $command = $admin.CreateCommand()
    try {
        $command.CommandText = $Sql; $command.CommandTimeout = 15
        if ($null -ne $transaction) { $command.Transaction = $transaction }
        return $command.ExecuteScalar()
    } finally { $command.Dispose() }
}
function Get-AppPermission([string]$Table, [string]$Privilege, [string]$Column) {
    $command = $admin.CreateCommand()
    try {
        # appUser has a strict allowlist above; SQL identifiers/values are otherwise bound.
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
        $command.Parameters['@column'].Value = if ([string]::IsNullOrEmpty($Column)) { [DBNull]::Value } else { $Column }
        return [int]$command.ExecuteScalar()
    } finally { $command.Dispose() }
}

# Requirements from the current DAOs. No UPDATE on historical lines/receipts or clinical data.
$required = @()
foreach ($table in @('HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','ChiTietDieuTri','ChiDinhCanLamSang',
    'PhieuKham','HoSoBenhAn','KeHoachDieuTri','DichVu','Thuoc','VatTu','BenhNhan','TaiKhoan','NhanVien')) {
    $required += [pscustomobject]@{ Table=$table; Privilege='SELECT'; Column=$null }
}
foreach ($table in @('HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','NhatKyThaoTac')) {
    $required += [pscustomobject]@{ Table=$table; Privilege='INSERT'; Column=$null }
}
$required += [pscustomobject]@{ Table='HoaDon'; Privilege='UPDATE'; Column='trangThai' }
foreach ($column in @('trangThai','maTaiKhoanDuyet','thoiGianDuyet','lyDoDuyet')) {
    $required += [pscustomobject]@{ Table='YeuCauHuyHoaDon'; Privilege='UPDATE'; Column=$column }
}

try {
    $admin.Open()
    if ((Invoke-Scalar 'SELECT DB_NAME()') -cne $database) { throw 'Connected database mismatch.' }
    if ([int](Invoke-Scalar "SELECT CASE WHEN ORIGINAL_LOGIN() = 'sa' THEN 1 ELSE 0 END") -ne 0) { throw 'Windows authentication required; sa is not permitted.' }
    if ([int](Invoke-Scalar "SELECT HAS_PERMS_BY_NAME(DB_NAME(), 'DATABASE', 'CONTROL')") -ne 1) { throw 'Windows principal cannot administer this development database.' }
    if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo')") -ne 39) { throw 'Unexpected development schema.' }
    if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.database_principals d JOIN sys.server_principals s ON s.sid=d.sid WHERE d.name='$appUser' AND s.name='$appUser' AND d.type='S'") -ne 1) { throw 'Application database user/login mapping mismatch.' }
    if ([int](Invoke-Scalar "SELECT IS_ROLEMEMBER('db_owner', '$appUser')") -ne 0) { throw 'Application user must not be db_owner.' }
    foreach ($permission in $required) {
        if ([int](Invoke-Scalar ("SELECT COUNT(*) FROM sys.tables WHERE object_id=OBJECT_ID('dbo." + $permission.Table + "')")) -ne 1) { throw ('Required table missing: ' + $permission.Table) }
        if ($permission.Column -and [int](Invoke-Scalar ("SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo." + $permission.Table + "') AND name='" + $permission.Column + "'")) -ne 1) { throw ('Required column missing: ' + $permission.Column) }
    }
    $changed = @()
    if ($Apply) { $transaction = $admin.BeginTransaction() }
    foreach ($permission in $required) {
        $label = 'dbo.' + $permission.Table + $(if ($permission.Column) { '.' + $permission.Column }) + ' / ' + $permission.Privilege
        if ((Get-AppPermission $permission.Table $permission.Privilege $permission.Column) -eq 1) { continue }
        Write-Output ('MISSING: ' + $label)
        if ($Apply) {
            $columnClause = if ($permission.Column) { ' ([' + $permission.Column + '])' } else { '' }
            $sql = 'GRANT ' + $permission.Privilege + ' ON OBJECT::[dbo].[' + $permission.Table + ']' + $columnClause + ' TO [' + $appUser + ']'
            [void](Invoke-Scalar $sql)
            if ((Get-AppPermission $permission.Table $permission.Privilege $permission.Column) -ne 1) { throw ('Permission verification failed: ' + $label) }
            $changed += $label
        }
    }
    # Check the boundary before commit; a failure rolls back this whole grant batch.
    foreach ($table in @('HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','ChiTietDieuTri','ChiDinhCanLamSang')) {
        if ((Get-AppPermission $table 'DELETE' '') -ne 0) { throw ('Unexpected DELETE on ' + $table) }
    }
    foreach ($table in @('HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','ChiTietDieuTri','ChiDinhCanLamSang')) {
        if ((Get-AppPermission $table 'UPDATE' '') -ne 0) { throw ('Unexpected table-wide UPDATE on ' + $table) }
    }
    if ([int](Invoke-Scalar "SELECT IS_ROLEMEMBER('db_owner', '$appUser')") -ne 0) { throw 'Unexpected db_owner role.' }
    if ($Apply) {
        $transaction.Commit(); $committed = $true
        $transaction.Dispose(); $transaction = $null
        $report = [pscustomobject]@{ Timestamp=[DateTimeOffset]::Now.ToString('o'); Scope='Existing isolated N08Dev database only'; Authentication='Windows integrated, local shared memory'; Granted=$changed; SchemaChanged=$false; BusinessDataChanged=$false }
        $reportPath = Join-Path $repo 'target/billing-permission-grant.json'
        [void][IO.Directory]::CreateDirectory((Split-Path -Parent $reportPath))
        [IO.File]::WriteAllText($reportPath, ($report | ConvertTo-Json -Depth 4))
        Write-Output ('PASS: committed ' + $changed.Count + ' missing object/column grants; no business rows or schema changed.')
    } else { Write-Output 'CHECK COMPLETE: no permission/data/schema changes.' }
} catch {
    if ($null -ne $transaction -and !$committed) { try { $transaction.Rollback() } catch { Write-Warning 'Rollback not confirmed. Run -Check before considering another apply.' } }
    throw
} finally {
    if ($null -ne $transaction) { $transaction.Dispose() }
    $admin.Dispose()
}
