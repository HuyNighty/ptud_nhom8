# Run in the new worktree only. SQL authentication is the default.
# Example: .\scripts\check-db.ps1 -Server localhost -Port 1433 -Database N08_NhaKhoa -Inspect
# Self-signed LOCAL SQL Server ONLY: add -TrustLocalCertificate explicitly.
[CmdletBinding()]
param(
    [string]$Server = 'localhost',
    [ValidateRange(1,65535)][int]$Port = 1433,
    [string]$Database = 'N08_NhaKhoa',
    [string]$User = '',
    [switch]$WindowsAuth,
    [switch]$TrustLocalCertificate,
    [switch]$Inspect
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $root 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $root 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: expected Maven project root beside this launcher.'
}
if (-not (Test-Path (Join-Path $root 'scripts/run.ps1'))) { throw 'STOP: run.ps1 is missing.' }
if ($Server -notmatch '^[a-zA-Z0-9._-]+$') { throw 'Invalid server name. Supply only a hostname or IPv4 address.' }
if ($Database -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Invalid database name. Check SSMS for the exact DB name.' }
if ($TrustLocalCertificate -and $Server -notin @('localhost', '127.0.0.1', '.')) {
    throw 'STOP: TrustLocalCertificate is restricted to the local SQL Server.'
}
if ($WindowsAuth -and -not [string]::IsNullOrWhiteSpace($User)) {
    throw 'Choose either -WindowsAuth or -User, not both.'
}
$keys = @('N08_DB_URL','N08_DB_USER','N08_DB_PASSWORD')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$secure = $null
try {
    $trust = if ($TrustLocalCertificate) { 'true' } else { 'false' }
    $url = "jdbc:sqlserver://${Server}:${Port};databaseName=${Database};encrypt=true;trustServerCertificate=${trust};loginTimeout=8;applicationName=N08-R2-Preflight"
    if ($WindowsAuth) {
        $url += ';integratedSecurity=true'
        [Environment]::SetEnvironmentVariable('N08_DB_USER', $null, 'Process')
        [Environment]::SetEnvironmentVariable('N08_DB_PASSWORD', $null, 'Process')
    } else {
        if ([string]::IsNullOrWhiteSpace($User)) {
            $User = Read-Host 'SQL Server login (not Windows login)'
        }
        if ([string]::IsNullOrWhiteSpace($User)) { throw 'SQL login is required unless -WindowsAuth is selected.' }
        if ($User -match '[;{}]') { throw 'Invalid user name.' }
        $secure = Read-Host 'SQL Server password (hidden)' -AsSecureString
        $password = (New-Object System.Net.NetworkCredential($User, $secure)).Password
        [Environment]::SetEnvironmentVariable('N08_DB_USER', $User, 'Process')
        [Environment]::SetEnvironmentVariable('N08_DB_PASSWORD', $password, 'Process')
        $password = $null
    }
    [Environment]::SetEnvironmentVariable('N08_DB_URL', $url, 'Process')
    Write-Host "TARGET: ${Server}:${Port} / ${Database} (no schema writes)" -ForegroundColor Cyan
    Write-Host "TLS: encrypt=true; trustServerCertificate=$trust" -ForegroundColor Cyan
    Push-Location $root
    try {
        if ($Inspect) { & .\scripts\run.ps1 -InspectDb }
        else { & .\scripts\run.ps1 -CheckDb }
        if ($LASTEXITCODE -is [int] -and $LASTEXITCODE -ne 0) { throw "Java failed: exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    if ($null -ne $secure) { $secure.Dispose() }
}
