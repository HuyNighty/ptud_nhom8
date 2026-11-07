[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not (Test-Path -LiteralPath (Join-Path $repo 'pom.xml') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $repo 'mvnw.cmd') -PathType Leaf)) {
    throw 'STOP: expected Maven project root beside this launcher.'
}
Push-Location $repo
try {
    & .\mvnw.cmd -q compile
    if ($LASTEXITCODE -ne 0) { throw 'STOP: Maven compile failed.' }
    $out = Join-Path $repo 'target/emergency-kit-rules-test'
    [System.IO.Directory]::CreateDirectory($out) | Out-Null
    $source = 'src/test/java/com/quanlynhakhoa/nghiepvu/kho/hopcapcuu/EmergencyKitRulesSelfTest.java'
    & javac --release 21 -encoding UTF-8 -cp (Join-Path $repo 'target/classes') -d $out $source
    if ($LASTEXITCODE -ne 0) { throw 'STOP: emergency-kit self-test compile failed.' }
    $classpath = (Join-Path $repo 'target/classes') + ';' + $out
    & java -cp $classpath 'com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.EmergencyKitRulesSelfTest'
    if ($LASTEXITCODE -ne 0) { throw 'STOP: emergency-kit self-test failed.' }
} finally { Pop-Location }
