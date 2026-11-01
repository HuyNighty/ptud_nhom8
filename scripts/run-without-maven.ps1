[CmdletBinding()]
param([switch]$Smoke, [switch]$CheckDb, [switch]$InspectDb)
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repo
try {
    $sourceFiles = @(Get-ChildItem 'src/main/java' -Recurse -Filter '*.java' -File | ForEach-Object FullName)
    if ($sourceFiles.Count -eq 0) { throw 'No Java sources found.' }
    New-Item -ItemType Directory -Force 'out' | Out-Null
    & javac --release 21 -encoding UTF-8 -d out @sourceFiles
    if ($LASTEXITCODE -ne 0) { throw "javac exit code: $LASTEXITCODE" }
    $cp = 'out'
    $drivers = @(Get-ChildItem 'lib' -Filter 'mssql-jdbc*.jar' -File -ErrorAction SilentlyContinue)
    if ($drivers.Count -gt 0) { $cp += ';' + $drivers[0].FullName }
    $javaArgs = @('-cp', $cp, 'com.quanlynhakhoa.App')
    if ($Smoke) { $javaArgs += '--smoke' }
    if ($CheckDb) { $javaArgs += '--check-db' }
    if ($InspectDb) { $javaArgs += '--inspect-db' }
    & java @javaArgs
    if ($LASTEXITCODE -ne 0) { throw "Java exit code: $LASTEXITCODE" }
} finally { Pop-Location }
