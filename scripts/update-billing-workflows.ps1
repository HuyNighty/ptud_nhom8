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
$builder['Application Name'] = 'N08-Billing-Workflow-Migration'
$admin = New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)

try {
    $admin.Open()
    $command=$admin.CreateCommand()
    try {
        if ($Check) {
            $command.CommandText="SELECT CASE WHEN OBJECT_ID('dbo.LanThuHoaDon','U') IS NOT NULL AND OBJECT_ID('dbo.DieuChinhThanhToan','U') IS NOT NULL AND COL_LENGTH('dbo.HoaDon','tenBenhNhanChot') IS NOT NULL THEN 1 ELSE 0 END"
            if ([int]$command.ExecuteScalar() -ne 1) { throw 'Billing migration is required.' }
            Write-Host 'PASS: billing workflow schema exists.'
            return
        }
        $command.CommandTimeout=90
        $command.CommandText=[System.IO.File]::ReadAllText((Join-Path $repo 'sql/bootstrap/V8__harden_billing_workflows.sql'))
        [void]$command.ExecuteNonQuery()
        $command.CommandText="GRANT SELECT, INSERT, UPDATE ON dbo.LanThuHoaDon TO [$appUser]; GRANT SELECT, INSERT ON dbo.DieuChinhThanhToan TO [$appUser]; GRANT SELECT ON dbo.DongTienHoaDon TO [$appUser]; GRANT UPDATE (trangThai,maTaiKhoanHuy,thoiGianHuy,lyDoHuy) ON dbo.ThanhToan TO [$appUser];"
        [void]$command.ExecuteNonQuery()
    } finally { $command.Dispose() }
    Write-Host 'PASS: billing workflow schema and application permissions updated in the configured isolated local database.'
} finally { $admin.Dispose() }
