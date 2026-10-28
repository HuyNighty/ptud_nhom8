[CmdletBinding()]
param([switch]$Smoke, [switch]$CheckDb, [switch]$InspectDb)
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repo
try {
    if (-not (Test-Path '.\mvnw.cmd')) { throw 'Maven Wrapper missing' }
    $mavenArgs = @('-q', 'compile', 'exec:java')
    if ($Smoke) { $mavenArgs += '-Dexec.args=--smoke' }
    if ($CheckDb) { $mavenArgs += '-Dexec.args=--check-db' }
    if ($InspectDb) { $mavenArgs += '-Dexec.args=--inspect-db' }
    & .\mvnw.cmd @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw "Maven/Java exit code: $LASTEXITCODE" }
} finally { Pop-Location }
