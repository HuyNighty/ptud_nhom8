# R3-A2 synthetic-only validation; no DB access or application launch.
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = (Get-Location).Path
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not [string]::Equals([System.IO.Path]::GetFullPath($root), $repo,
    [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'STOP: run policy test from the repository root.'
}
if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: missing Maven project files.'
}
Write-Host 'Running R3-A2 synthetic policy tests (NO DATABASE CONNECTION)...' -ForegroundColor Cyan
& .\mvnw.cmd -q test-compile
if ($LASTEXITCODE -ne 0) { throw 'Maven test-compile failed.' }
$bcryptJar = Join-Path $env:USERPROFILE '.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar'
if (-not (Test-Path -LiteralPath $bcryptJar -PathType Leaf)) {
    throw 'STOP: jBCrypt JAR not found in default Maven local repository.'
}
$classpath = (Join-Path $root 'target\test-classes') + ';' +
             (Join-Path $root 'target\classes') + ';' + $bcryptJar
& java -cp $classpath com.quanlynhakhoa.auth.AuthPolicySelfTest
if ($LASTEXITCODE -ne 0) { throw 'R3-A2 policy self-test failed.' }
Write-Host 'R3-A2 SYNTHETIC TEST ACCEPTANCE COMPLETE.' -ForegroundColor Green
