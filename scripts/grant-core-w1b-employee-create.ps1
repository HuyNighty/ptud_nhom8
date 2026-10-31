[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Apply,
    [string]$SqlAdminUser = 'sa'
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Check -eq $Apply) { throw 'Choose exactly one: -Check or -Apply' }
if ($SqlAdminUser -notmatch '^[a-zA-Z0-9._-]{1,60}$') { throw 'Invalid SQL administrator name' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path (Join-Path $repo 'pom.xml')) -or
    -not (Test-Path (Join-Path $repo '.n08-dev.local.properties'))) {
    throw 'Missing Maven project or local N08Dev configuration'
}
if ($Apply) {
    Write-Host 'W1B grants ONLY INSERT ON dbo.NhanVien to restricted local N08Dev login.' -ForegroundColor Yellow
    Write-Host 'No UPDATE, DELETE, ALTER or CREATE grants. No employee rows are inserted.' -ForegroundColor Yellow
    $answer = Read-Host 'Type GRANT to authorize this single SQL privilege'
    if ($answer -cne 'GRANT') { throw 'STOP: permission change not authorized' }
}
$cpFile = Join-Path $env:TEMP ('n08-w1b-cp-' + [guid]::NewGuid().ToString('N') + '.txt')
$oldUser = [Environment]::GetEnvironmentVariable('N08_W1B_SQL_ADMIN_USER', 'Process')
$oldPassword = [Environment]::GetEnvironmentVariable('N08_W1B_SQL_ADMIN_PASSWORD', 'Process')
Push-Location $repo
try {
    & .\mvnw.cmd -q compile dependency:build-classpath "-Dmdep.outputFile=$cpFile"
    if ($LASTEXITCODE -ne 0) { throw 'Maven compile/dependency classpath failed' }
    $deps = [System.IO.File]::ReadAllText($cpFile).Trim()
    if ([string]::IsNullOrWhiteSpace($deps)) { throw 'Empty dependency classpath' }
    $secure = Read-Host 'Local SQL admin password (hidden)' -AsSecureString
    $bstr = [IntPtr]::Zero
    try {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
    }
    [Environment]::SetEnvironmentVariable('N08_W1B_SQL_ADMIN_USER', $SqlAdminUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_W1B_SQL_ADMIN_PASSWORD', $plain, 'Process')
    $plain = $null
    $mode = if ($Apply) { '--apply' } else { '--check' }
    & java -cp ((Join-Path $repo 'target/classes') + ';' + $deps) 'com.quanlynhakhoa.dev.WorkforceSqlGrants' $mode
    if ($LASTEXITCODE -ne 0) { throw 'W1B SQL gate failed' }
} finally {
    [Environment]::SetEnvironmentVariable('N08_W1B_SQL_ADMIN_USER', $oldUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_W1B_SQL_ADMIN_PASSWORD', $oldPassword, 'Process')
    Remove-Item -LiteralPath $cpFile -Force -ErrorAction SilentlyContinue
    Pop-Location
}
