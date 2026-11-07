[CmdletBinding()]
param(
    [switch]$Check, [switch]$Apply, [switch]$Plan,
    [string]$SqlAdminUser = 'sa',
    [switch]$WindowsAuth
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (([int]$Check.IsPresent + [int]$Apply.IsPresent + [int]$Plan.IsPresent) -ne 1) {
    throw 'Choose exactly one mode: -Check, -Apply or -Plan.'
}
# Only requirements issued by PhanCongDao. Never grant table-wide UPDATE.
$required = @(
    foreach ($table in @('NhanVien','TaiKhoan','CaLamViec','PhanCong','ThamSoHeThong')) {
        [pscustomobject]@{ Table=$table; Privilege='SELECT'; Column=$null }
    }
    [pscustomobject]@{ Table='CaLamViec'; Privilege='INSERT'; Column=$null }
    [pscustomobject]@{ Table='PhanCong'; Privilege='INSERT'; Column=$null }
    [pscustomobject]@{ Table='PhanCong'; Privilege='UPDATE'; Column='trangThai' }
    [pscustomobject]@{ Table='PhanCong'; Privilege='UPDATE'; Column='lyDoHuy' }
    [pscustomobject]@{ Table='NhatKyThaoTac'; Privilege='INSERT'; Column=$null }
)
if ($Plan) { return $required } # Offline matrix only; no SQL connection, no readiness claim.

$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$config = Join-Path $repo '.n08-dev.local.properties'
if (!(Test-Path -LiteralPath $config -PathType Leaf)) {
    throw 'BLOCKED: no local development configuration. Do not copy credentials from another workspace.'
}
# Read only endpoint identifiers; application passwords are never used or printed.
$settings = @{}
foreach ($line in [IO.File]::ReadAllLines($config)) {
    foreach ($key in @('dev.database','dev.port','dev.user')) {
        if ($line.StartsWith($key + '=')) { $settings[$key] = $line.Substring($key.Length + 1).Trim() }
    }
}
$database = $settings['dev.database']
$appUser = $settings['dev.user']
if ($database -cnotmatch '^N08Dev_[0-9a-f]{8}$') { throw 'Invalid isolated N08Dev database.' }
if ($appUser -cne ('n08dev_' + $database.Substring(7))) { throw 'Application user does not match database.' }
if ($settings['dev.port'] -notmatch '^[0-9]{1,5}$') { throw 'Invalid local SQL port.' }
$port = [int]$settings['dev.port']
if ($port -lt 1 -or $port -gt 65535) { throw 'Invalid local SQL port.' }

# Build an unopened connection. SQL Authentication matches setup-local/grant-dev-all;
# -WindowsAuth is opt-in. SqlCredential keeps the secret out of connection strings.
function New-ScheduleAdminConnection([string]$Database, [int]$Port,
        [string]$SqlAdminUser = 'sa', [switch]$WindowsAuth) {
    if ($Database -cnotmatch '^N08Dev_[0-9a-f]{8}$' -or $Port -lt 1 -or $Port -gt 65535) {
        throw 'Invalid isolated SQL endpoint.'
    }
    if (!$WindowsAuth -and ($SqlAdminUser -cnotmatch '^[a-zA-Z0-9._-]{1,60}$' -or
            $SqlAdminUser -match '^n08dev_[0-9a-f]{8}$')) {
        throw 'Use an administrator SQL login, never an application database user.'
    }
    Add-Type -AssemblyName System.Data
    $builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
    $builder['Data Source'] = 'tcp:127.0.0.1,' + $Port
    $builder['Initial Catalog'] = $Database
    $builder['Integrated Security'] = $WindowsAuth.IsPresent
    $builder['Encrypt'] = $true
    $builder['TrustServerCertificate'] = $true
    $builder['Persist Security Info'] = $false
    $builder['Pooling'] = $false
    $builder['Connect Timeout'] = 8
    $builder['Application Name'] = 'N08-Schedule-Dev-Permissions'
    $secure = $null
    $connection = $null
    try {
        if ($WindowsAuth) {
            $connection = [System.Data.SqlClient.SqlConnection]::new($builder.ConnectionString)
        } else {
            $secure = Read-Host ('Password for local SQL administrator ' + $SqlAdminUser + ' (hidden)') -AsSecureString
            if ($null -eq $secure -or $secure.Length -eq 0) { throw 'Administrator password must not be empty.' }
            $secure.MakeReadOnly()
            $credential = [System.Data.SqlClient.SqlCredential]::new($SqlAdminUser, $secure)
            $connection = [System.Data.SqlClient.SqlConnection]::new($builder.ConnectionString, $credential)
        }
        return $connection
    } finally {
        # Once returned, caller owns both connection and Credential.Password cleanup.
        if ($null -eq $connection -and $null -ne $secure) { $secure.Dispose() }
        $builder.Clear()
    }
}
$admin = New-ScheduleAdminConnection -Database $database -Port $port -SqlAdminUser $SqlAdminUser -WindowsAuth:$WindowsAuth
$transaction = $null
$confirmed = $false
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
function Get-AppPermission([string]$Object, [string]$Class, [string]$Privilege, [string]$Column = '') {
    $command = $admin.CreateCommand()
    try {
        # appUser is strictly n08dev_<8 hex>. All permission values are parameters.
        $command.CommandText = @"
DECLARE @allowed int;
EXECUTE AS USER = '$appUser';
BEGIN TRY
  SET @allowed = CASE WHEN @column IS NULL
    THEN HAS_PERMS_BY_NAME(@object, @class, @privilege)
    ELSE HAS_PERMS_BY_NAME(@object, @class, @privilege, @column, 'COLUMN') END;
  REVERT;
END TRY
BEGIN CATCH
  REVERT;
  THROW;
END CATCH;
SELECT @allowed;
"@
        $command.CommandTimeout = 15
        if ($null -ne $transaction) { $command.Transaction = $transaction }
        foreach ($name in @('object','class','privilege','column')) {
            [void]$command.Parameters.Add('@' + $name, [Data.SqlDbType]::NVarChar, 256)
        }
        $command.Parameters['@object'].Value = $Object
        $command.Parameters['@class'].Value = $Class
        $command.Parameters['@privilege'].Value = $Privilege
        $command.Parameters['@column'].Value = if ($Column) { $Column } else { [DBNull]::Value }
        $value = $command.ExecuteScalar()
        if ($null -eq $value -or $value -is [DBNull]) { throw 'Permission evaluation unknown; refusing grant.' }
        return [int]$value
    } finally { $command.Dispose() }
}
function Assert-Boundary {
    if ((Invoke-Scalar 'SELECT DB_NAME()') -cne $database) { throw 'Connected database mismatch.' }
    if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.database_principals d JOIN sys.server_principals s ON s.sid=d.sid WHERE d.name='$appUser' AND s.name='$appUser' AND d.type='S'") -ne 1) {
        throw 'Application database user/login association mismatch.'
    }
    if ([int](Invoke-Scalar "SELECT COALESCE(IS_SRVROLEMEMBER('sysadmin', '$appUser'), 1)") -ne 0 -or
        [int](Invoke-Scalar "SELECT COALESCE(IS_ROLEMEMBER('db_owner', '$appUser'), 1)") -ne 0) {
        throw 'Application principal must not be sysadmin/db_owner (unknown membership also refused).'
    }
    if ((Get-AppPermission $database 'DATABASE' 'CONTROL') -ne 0 -or
        (Get-AppPermission 'dbo' 'SCHEMA' 'CONTROL') -ne 0) { throw 'Unexpected database/schema CONTROL.' }
    foreach ($table in @('NhanVien','TaiKhoan','CaLamViec','PhanCong','ThamSoHeThong','NhatKyThaoTac')) {
        if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.tables WHERE object_id=OBJECT_ID('dbo.$table')") -ne 1) { throw ('Required table missing: ' + $table) }
        if ((Get-AppPermission ('dbo.' + $table) 'OBJECT' 'CONTROL') -ne 0 -or
            (Get-AppPermission ('dbo.' + $table) 'OBJECT' 'DELETE') -ne 0) { throw ('Unexpected broad rights on ' + $table) }
    }
    if ((Get-AppPermission 'dbo.PhanCong' 'OBJECT' 'UPDATE') -ne 0) { throw 'Unexpected table-wide UPDATE on PhanCong.' }
    foreach ($column in @('maPhanCong','maNhanVien','maCa','ngayLam','nhiemVu','maPhanCongGoc')) {
        if ((Get-AppPermission 'dbo.PhanCong' 'OBJECT' 'UPDATE' $column) -ne 0) { throw ('Unexpected UPDATE on PhanCong.' + $column) }
    }
    foreach ($column in @('trangThai','lyDoHuy')) {
        if ([int](Invoke-Scalar "SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.PhanCong') AND name='$column'") -ne 1) { throw ('Required column missing: ' + $column) }
    }
    # Never turn an explicit DENY into a column GRANT. User/role/public token checked.
    $denies = Invoke-Scalar @"
DECLARE @denies int;
EXECUTE AS USER = '$appUser';
BEGIN TRY
  SELECT @denies=COUNT(*) FROM sys.database_permissions
  WHERE state='D' AND permission_name IN ('SELECT','INSERT','UPDATE','CONTROL')
    AND grantee_principal_id IN (SELECT principal_id FROM sys.user_token)
    AND (class=0 OR (class=3 AND major_id=SCHEMA_ID('dbo')) OR
      (class=1 AND major_id IN (OBJECT_ID('dbo.NhanVien'),OBJECT_ID('dbo.TaiKhoan'),
       OBJECT_ID('dbo.CaLamViec'),OBJECT_ID('dbo.PhanCong'),
       OBJECT_ID('dbo.ThamSoHeThong'),OBJECT_ID('dbo.NhatKyThaoTac'))));
  REVERT;
END TRY
BEGIN CATCH
  REVERT;
  THROW;
END CATCH;
SELECT @denies;
"@
    if ($null -eq $denies -or $denies -is [DBNull] -or [int]$denies -ne 0) { throw 'Explicit DENY or unknown evaluation; ask DBA, no override.' }
}
function Invoke-MissingGrant($Permission) {
    # All mutating SQL is behind BOTH Apply and explicit confirmation, with a transaction.
    if (!$Apply -or !$confirmed -or $null -eq $transaction) { throw 'Grant write gate is closed.' }
    if ((Invoke-Scalar 'SELECT DB_NAME()') -cne $database) { throw 'Connected database changed.' }
    if ($Permission -notin $required) { throw 'Permission outside module allowlist.' }
    if ((Get-AppPermission ('dbo.' + $Permission.Table) 'OBJECT' $Permission.Privilege $Permission.Column) -eq 1) { return }
    $columnClause = if ($Permission.Column) { ' ([' + $Permission.Column + '])' } else { '' }
    $sql = 'GRANT ' + $Permission.Privilege + ' ON OBJECT::[dbo].[' + $Permission.Table + ']' + $columnClause + ' TO [' + $appUser + ']'
    [void](Invoke-Scalar $sql)
    if ((Get-AppPermission ('dbo.' + $Permission.Table) 'OBJECT' $Permission.Privilege $Permission.Column) -ne 1) { throw 'Grant did not produce required effective permission.' }
}

try {
    $admin.Open()
    Assert-Boundary
    $missing = @()
    foreach ($permission in $required) {
        $present = (Get-AppPermission ('dbo.' + $permission.Table) 'OBJECT' $permission.Privilege $permission.Column) -eq 1
        [pscustomobject]@{ Table=$permission.Table; Privilege=$permission.Privilege; Column=$permission.Column; Before=$(if ($present) {'PRESENT'} else {'MISSING'}) }
        if (!$present) { $missing += $permission }
    }
    if ($Check) {
        # No transaction, no GRANT, no DDL, no business-data probe in this branch.
        if ($missing.Count -gt 0) { throw ('NOT READY: ' + $missing.Count + ' missing permissions. Check made no database changes.') }
        Write-Output 'CHECK READY: effective module permissions present; no database changes.'
        return
    }
    $expected = 'GRANT SCHEDULE ' + $database + ' ' + $appUser
    Write-Host ('Apply grants only ' + $missing.Count + ' missing object/column permissions. No data/schema writes.')
    $answer = Read-Host ('Type exactly: ' + $expected)
    if ($answer -cne $expected) { throw 'STOP: grant change not authorized.' }
    $confirmed = $true
    $transaction = $admin.BeginTransaction()
    Assert-Boundary
    foreach ($permission in $missing) { Invoke-MissingGrant $permission }
    Assert-Boundary
    foreach ($permission in $required) {
        if ((Get-AppPermission ('dbo.' + $permission.Table) 'OBJECT' $permission.Privilege $permission.Column) -ne 1) { throw 'Final permission verification failed.' }
    }
    $transaction.Commit()
    $committed = $true
    Write-Output 'APPLY COMPLETE: missing grants committed. Run -Check to capture final effective permissions.'
} catch {
    if ($null -ne $transaction -and !$committed) {
        try { $transaction.Rollback() } catch { Write-Warning 'Rollback not confirmed. DBA must inspect before retry.' }
    }
    # No connection strings or raw SQL exception payloads in logs.
    $failure = $_.Exception
    while ($null -ne $failure -and $failure -isnot [System.Data.SqlClient.SqlException]) { $failure = $failure.InnerException }
    if ($null -ne $failure) {
        throw ('SQL readiness blocked (SQL error ' + $failure.Number + '). Check selected administrator authentication, target, schema and permissions; do not broaden grants automatically.')
    }
    throw
} finally {
    $secret = if ($null -ne $admin.Credential) { $admin.Credential.Password } else { $null }
    try { if ($null -ne $transaction) { $transaction.Dispose() } } finally { try { $admin.Dispose() } finally { if ($null -ne $secret) { $secret.Dispose() }; $secret = $null } }
}
