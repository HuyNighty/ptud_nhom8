# Explicit interactive demo launcher - R3-A3 sandbox database only.
[CmdletBinding()]
param(
    [string]$Database = 'N08AuthIT_485d35bd',
    [string]$User = ''
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Database -cnotmatch '^N08AuthIT_[0-9a-fA-F]{8}$') { throw 'STOP: only isolated N08AuthIT_ databases allowed.' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: expected Maven project root beside this launcher.'
}
$keys = @('N08_DB_URL', 'N08_DB_USER', 'N08_DB_PASSWORD')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$secure = $null
try {
    if ([string]::IsNullOrWhiteSpace($User)) { $User = Read-Host 'SQL Server login' }
    if ([string]::IsNullOrWhiteSpace($User) -or $User -match '[;{}]') { throw 'Invalid SQL login.' }
    $secure = Read-Host 'SQL Server password (hidden)' -AsSecureString
    $password = (New-Object System.Net.NetworkCredential($User, $secure)).Password
    $url = "jdbc:sqlserver://127.0.0.1:14333;databaseName=${Database};encrypt=true;trustServerCertificate=true;loginTimeout=8;applicationName=N08-R3A3-Sandbox-Login"
    [Environment]::SetEnvironmentVariable('N08_DB_URL', $url, 'Process')
    [Environment]::SetEnvironmentVariable('N08_DB_USER', $User, 'Process')
    [Environment]::SetEnvironmentVariable('N08_DB_PASSWORD', $password, 'Process')
    $password = $null
    Write-Host "GUI AUTH SANDBOX: $Database on 127.0.0.1:14333" -ForegroundColor Cyan
    Write-Host 'Login attempts may UPDATE synthetic TaiKhoan rows in this sandbox.' -ForegroundColor Yellow
    Write-Host 'No production database is accepted by the GUI connection guard.' -ForegroundColor Green
    Push-Location $repo
    try {
        & .\scripts\run.ps1
        if ($LASTEXITCODE -is [int] -and $LASTEXITCODE -ne 0) { throw "Application exited with code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    if ($null -ne $secure) { $secure.Dispose() }
}
