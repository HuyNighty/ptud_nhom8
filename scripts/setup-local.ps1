# N08 M0-CORE-02 - ONE-TIME LOCAL SQL SERVER SETUP (Docker or native, SQL Auth).
# Starts NO containers or services; never targets existing databases or N08Dental.
[CmdletBinding()]
param(
    [ValidateRange(1,65535)][int]$Port = 1433,
    [string]$SqlAdminUser = 'sa'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'sql/bootstrap/V4__create_audit_log.sql') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: expected N08 Maven project root and SQL bootstrap beside this launcher.'
}
if (Test-Path -LiteralPath (Join-Path $repo '.n08-dev.local.properties')) {
    throw 'STOP: a local config already exists. Do not overwrite it or create a second database.'
}
if ($SqlAdminUser -cnotmatch '^[a-zA-Z0-9._-]{1,60}$') {
    throw 'STOP: SQL Authentication login has an unexpected format.'
}
$random = New-Object byte[] 4
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($random) } finally { $rng.Dispose() }
$suffix = ([System.BitConverter]::ToString($random)).Replace('-', '').ToLowerInvariant()
$database = 'N08Dev_' + $suffix
Write-Host 'N08 LOCAL DEVELOPMENT SETUP' -ForegroundColor Cyan
Write-Host "Target: 127.0.0.1:$Port / NEW database $database" -ForegroundColor Yellow
Write-Host 'Only the local SQL Server is supported. This will CREATE an isolated DB and SQL login.' -ForegroundColor Yellow
Write-Host 'If using SQL Server Express, configure TCP/IP and a fixed port first.' -ForegroundColor Yellow
$confirmation = Read-Host 'Type CREATE to explicitly authorize provisioning a NEW local development database'
if ($confirmation -cne 'CREATE') { throw 'STOP: consent not granted. No SQL requests made.' }
$adminSecure = Read-Host "Password for local SQL Server account $SqlAdminUser (hidden)" -AsSecureString
$appSecure = Read-Host 'Choose a NEW Swing dev.admin password (12-72 UTF-8 bytes, hidden)' -AsSecureString
$appConfirm = Read-Host 'Confirm NEW Swing password (hidden)' -AsSecureString
$adminPlain = (New-Object System.Net.NetworkCredential('local', $adminSecure)).Password
$appPlain = (New-Object System.Net.NetworkCredential('local', $appSecure)).Password
$appConfirmPlain = (New-Object System.Net.NetworkCredential('local', $appConfirm)).Password
if ($appPlain -cne $appConfirmPlain) { throw 'STOP: application passwords do not match. No SQL requests made.' }
$size = [System.Text.Encoding]::UTF8.GetByteCount($appPlain)
if ($size -lt 12 -or $size -gt 72) { throw 'STOP: password length must be 12-72 UTF-8 bytes. No SQL requests made.' }
$keys = @('N08_SETUP_DB','N08_SETUP_PORT','N08_SETUP_ADMIN_USER','N08_SETUP_ADMIN_PASSWORD','N08_SETUP_APP_PASSWORD')
$old = @{}
foreach ($key in $keys) { $old[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
try {
    [Environment]::SetEnvironmentVariable('N08_SETUP_DB', $database, 'Process')
    [Environment]::SetEnvironmentVariable('N08_SETUP_PORT', [string]$Port, 'Process')
    [Environment]::SetEnvironmentVariable('N08_SETUP_ADMIN_USER', $SqlAdminUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_SETUP_ADMIN_PASSWORD', $adminPlain, 'Process')
    [Environment]::SetEnvironmentVariable('N08_SETUP_APP_PASSWORD', $appPlain, 'Process')
    Push-Location $repo
    try {
        # Run the setup entrypoint explicitly: pom.xml pins exec:java to App.
        # Maven resolves the real runtime dependency classpath, not SQL credentials.
        $classpathFile = Join-Path $env:TEMP ('N08_Setup_Classpath_' + [guid]::NewGuid().ToString('N') + '.txt')
        try {
            & .\mvnw.cmd -q compile dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
            if ($LASTEXITCODE -ne 0) {
                throw 'STOP: Maven compilation or classpath resolution failed. No SQL calls made by setup.'
            }
            if (-not (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
                throw 'STOP: dependency classpath file missing. No SQL calls made by setup.'
            }
            $dependencies = [System.IO.File]::ReadAllText($classpathFile).Trim()
            if ([string]::IsNullOrWhiteSpace($dependencies)) {
                throw 'STOP: dependency classpath is empty. No SQL calls made by setup.'
            }
            $classPath = (Join-Path $repo 'target/classes') + ';' + $dependencies
            & java -cp $classPath 'com.quanlynhakhoa.dev.DevSetup'
            if ($LASTEXITCODE -ne 0) {
                throw 'STOP: DevSetup failed; SQL resources may exist. Do NOT retry automatically.'
            }
            if (-not (Test-Path -LiteralPath (Join-Path $repo '.n08-dev.local.properties') -PathType Leaf)) {
                throw 'STOP: DevSetup exited without creating local config. Inspect SQL resources before retrying.'
            }
        } finally {
            Remove-Item -LiteralPath $classpathFile -Force -ErrorAction SilentlyContinue
        }
    } finally { Pop-Location }
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $old[$key], 'Process') }
    $adminPlain = $null
    $appPlain = $null
    $appConfirmPlain = $null
    if ($null -ne $adminSecure) { $adminSecure.Dispose() }
    if ($null -ne $appSecure) { $appSecure.Dispose() }
    if ($null -ne $appConfirm) { $appConfirm.Dispose() }
}
Write-Host 'NEXT: Open this Maven project in IntelliJ and Run com.quanlynhakhoa.App.' -ForegroundColor Green
Write-Host 'Login username: dev.admin; use the password you chose above.' -ForegroundColor Green
Write-Host 'IMPORTANT: Never commit .n08-dev.local.properties.' -ForegroundColor Yellow
