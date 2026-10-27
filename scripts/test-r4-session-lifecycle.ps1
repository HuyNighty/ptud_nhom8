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
    $out = Join-Path $repo 'target/r4-a3-session-test'
    [System.IO.Directory]::CreateDirectory($out) | Out-Null
    $javaFile = 'src/test/java/com/quanlynhakhoa/view/R4SessionLifecycleSelfTest.java'
    & javac --release 21 -encoding UTF-8 -cp (Join-Path $repo 'target/classes') -d $out $javaFile
    if ($LASTEXITCODE -ne 0) { throw 'STOP: R4-A3 test compile failed.' }
    $classpath = (Join-Path $repo 'target/classes') + ';' + $out
    & java -cp $classpath 'com.quanlynhakhoa.view.R4SessionLifecycleSelfTest'
    if ($LASTEXITCODE -ne 0) { throw 'STOP: R4-A3 lifecycle test failed.' }
} finally { Pop-Location }