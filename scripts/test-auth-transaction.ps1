# R3-A2B synthetic JDBC proxy test runner. Does not access SQL Server.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $root
if (-not (Test-Path -LiteralPath '.\mvnw.cmd' -PathType Leaf)) {
    throw 'Expected Maven wrapper at repository root.'
}
Write-Host 'Compiling R3-A2B main and synthetic test Java sources...' -ForegroundColor Cyan
& .\mvnw.cmd -q test-compile
if ($LASTEXITCODE -ne 0) { throw "Maven test-compile failed (exit $LASTEXITCODE)" }
$cp = (Join-Path $root 'target\classes') + ';' + (Join-Path $root 'target\test-classes')
& java -cp $cp com.quanlynhakhoa.auth.JdbcAuthTransactionSelfTest
if ($LASTEXITCODE -ne 0) { throw "Synthetic transaction test failed (exit $LASTEXITCODE)" }
Write-Host 'NO SQL SERVER CONNECTION; NO SQL/DATA/SCHEMA WRITES.' -ForegroundColor Green
