# Verify the SAME local DB configuration used by IntelliJ App, without changing schema.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $repo '.n08-dev.local.properties'))) {
    throw 'STOP: local configuration missing; run scripts/setup-local.ps1 first.'
}
Push-Location $repo
try {
    & .\mvnw.cmd -q compile 'exec:java' '-Dexec.args=--team-db'
    if ($LASTEXITCODE -ne 0) { throw 'STOP: local JDBC database verification failed.' }
} finally { Pop-Location }
