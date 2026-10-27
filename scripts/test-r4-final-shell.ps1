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
    # Branch is intentionally not pinned: run tests on feature branches.
    & .\mvnw.cmd -q compile
    if ($LASTEXITCODE -ne 0) { throw 'STOP: Maven compile failed.' }
    $out = Join-Path $repo 'target/r4-a4-authorization-test'
    [System.IO.Directory]::CreateDirectory($out) | Out-Null
    $javaFile = 'src/test/java/com/quanlynhakhoa/security/R4FinalShellSelfTest.java'
    & javac --release 21 -encoding UTF-8 -cp (Join-Path $repo 'target/classes') -d $out $javaFile
    if ($LASTEXITCODE -ne 0) { throw 'STOP: R4-A4 test compile failed.' }
    $classpath = (Join-Path $repo 'target/classes') + ';' + $out
    & java -cp $classpath 'com.quanlynhakhoa.security.R4FinalShellSelfTest'
    if ($LASTEXITCODE -ne 0) { throw 'STOP: R4-A4 authorization test failed.' }
} finally { Pop-Location }
