[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Apply,
    [string]$SqlAdminUser = 'sa'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Check -eq $Apply) { throw 'Choose exactly one mode: -Check or -Apply' }
if ($SqlAdminUser -notmatch '^[a-zA-Z0-9._-]{1,60}$') { throw 'Invalid SQL admin name' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path (Join-Path $repo 'pom.xml')) -or
    -not (Test-Path (Join-Path $repo '.n08-dev.local.properties'))) {
    throw 'Missing Maven project or isolated SQL dev config'
}
if ($Apply) {
    Write-Host 'GRANT INSERT ON dbo.TaiKhoan ONLY for the restricted N08Dev SQL login.' -ForegroundColor Yellow
    Write-Host 'No DELETE, account updates, schema changes or app-account writes.' -ForegroundColor Yellow
    $answer = Read-Host 'Type GRANT to authorize this database permission change'
    if ($answer -cne 'GRANT') { throw 'STOP: no authorization' }
}
$classPathFile = Join-Path $env:TEMP ('n08-core06b1-cp-' + [guid]::NewGuid().ToString('N') + '.txt')
$oldName = [Environment]::GetEnvironmentVariable('N08_CORE06_SQL_ADMIN_USER', 'Process')
$oldPassword = [Environment]::GetEnvironmentVariable('N08_CORE06_SQL_ADMIN_PASSWORD', 'Process')
Push-Location $repo
try {
    & .\mvnw.cmd -q compile dependency:build-classpath "-Dmdep.outputFile=$classPathFile"
    if ($LASTEXITCODE -ne 0) { throw 'Maven failed' }
    $deps = [System.IO.File]::ReadAllText($classPathFile).Trim()
    if ([string]::IsNullOrWhiteSpace($deps)) { throw 'Empty classpath' }
    $secure = Read-Host 'Local SQL admin password (hidden)' -AsSecureString
    $bstr = [IntPtr]::Zero
    try {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    }
    [Environment]::SetEnvironmentVariable('N08_CORE06_SQL_ADMIN_USER', $SqlAdminUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_CORE06_SQL_ADMIN_PASSWORD', $plain, 'Process')
    $plain = $null
    $mode = if ($Apply) { '--apply' } else { '--check' }
    & java -cp ((Join-Path $repo 'target/classes') + ';' + $deps) 'com.quanlynhakhoa.dev.AccountCreateSqlGrants' $mode
    if ($LASTEXITCODE -ne 0) { throw 'SQL grant preflight/apply failed' }
} finally {
    [Environment]::SetEnvironmentVariable('N08_CORE06_SQL_ADMIN_USER', $oldName, 'Process')
    [Environment]::SetEnvironmentVariable('N08_CORE06_SQL_ADMIN_PASSWORD', $oldPassword, 'Process')
    Remove-Item -LiteralPath $classPathFile -Force -ErrorAction SilentlyContinue
    Pop-Location
}
