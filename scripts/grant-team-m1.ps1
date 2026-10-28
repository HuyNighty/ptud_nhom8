[CmdletBinding()]
param(
    [switch]$Check,
    [switch]$Apply,
    [string]$SqlAdminUser = 'sa'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($Check -eq $Apply) { throw 'Choose exactly one mode: -Check or -Apply.' }
if ($SqlAdminUser -notmatch '^[a-zA-Z0-9._-]{1,60}$') {
    throw 'Invalid local SQL administrator username.'
}
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo '.n08-dev.local.properties') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: Maven project or local development configuration missing.'
}

if ($Apply) {
    Write-Host 'This grants ONLY the reviewed M1 SELECT/INSERT/UPDATE permissions on an isolated N08Dev database.' -ForegroundColor Yellow
    Write-Host 'No DELETE, ALTER, db_owner, schema migration or account changes.' -ForegroundColor Yellow
    $confirmation = Read-Host 'Type GRANT to authorize M1 local database permission changes'
    if ($confirmation -cne 'GRANT') { throw 'STOP: no authorization. No SQL writes.' }
}

$classPathFile = Join-Path $env:TEMP ('N08_M1_Classpath_' + [guid]::NewGuid().ToString('N') + '.txt')
$oldUser = [Environment]::GetEnvironmentVariable('N08_M1_SQL_ADMIN_USER', 'Process')
$oldPassword = [Environment]::GetEnvironmentVariable('N08_M1_SQL_ADMIN_PASSWORD', 'Process')
Push-Location $repo
try {
    & .\mvnw.cmd -q compile dependency:build-classpath "-Dmdep.outputFile=$classPathFile"
    if ($LASTEXITCODE -ne 0) { throw 'Maven compile/classpath failed. No SQL writes.' }
    if (-not (Test-Path -LiteralPath $classPathFile -PathType Leaf)) {
        throw 'Maven dependency classpath not generated.'
    }
    $deps = [System.IO.File]::ReadAllText($classPathFile).Trim()
    if ([string]::IsNullOrWhiteSpace($deps)) { throw 'Maven dependency classpath is empty.' }
    $javaClassPath = (Join-Path $repo 'target/classes') + ';' + $deps

    $secure = Read-Host 'Local SQL administrator password (hidden)' -AsSecureString
    $bstr = [IntPtr]::Zero
    try {
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
    [Environment]::SetEnvironmentVariable('N08_M1_SQL_ADMIN_USER', $SqlAdminUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_M1_SQL_ADMIN_PASSWORD', $plain, 'Process')
    $plain = $null
    $mode = if ($Apply) { '--apply' } else { '--check' }
    & java -cp $javaClassPath 'com.quanlynhakhoa.dev.TeamM1Permissions' $mode
    if ($LASTEXITCODE -ne 0) { throw 'M1 permission tool failed. Inspect SQL before retrying.' }
} finally {
    [Environment]::SetEnvironmentVariable('N08_M1_SQL_ADMIN_USER', $oldUser, 'Process')
    [Environment]::SetEnvironmentVariable('N08_M1_SQL_ADMIN_PASSWORD', $oldPassword, 'Process')
    Remove-Item -LiteralPath $classPathFile -Force -ErrorAction SilentlyContinue
    Pop-Location
}
