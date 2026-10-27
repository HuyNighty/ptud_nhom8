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
    if ($LASTEXITCODE -ne 0) { throw 'Maven compile failed.' }
    $destination = Join-Path $repo 'target/r4-a1-self-test'
    [System.IO.Directory]::CreateDirectory($destination) | Out-Null
    & javac --release 21 -encoding UTF-8 -cp 'target/classes' -d $destination 'src/test/java/com/quanlynhakhoa/security/R4RoleSessionSelfTest.java'
    if ($LASTEXITCODE -ne 0) { throw 'R4-A1 test compile failed.' }
    $classpath = (Join-Path $repo 'target/classes') + ';' + $destination
    & java -cp $classpath com.quanlynhakhoa.security.R4RoleSessionSelfTest
    if ($LASTEXITCODE -ne 0) { throw 'R4-A1 self-test failed.' }
} finally { Pop-Location }
