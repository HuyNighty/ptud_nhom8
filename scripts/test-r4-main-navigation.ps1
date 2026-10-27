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
    $dest = Join-Path $repo 'target/r4-a2-navigation-test'
    [System.IO.Directory]::CreateDirectory($dest) | Out-Null
    & javac --release 21 -encoding UTF-8 -cp 'target/classes' -d $dest 'src/test/java/com/quanlynhakhoa/view/R4MainNavigationSelfTest.java'
    if ($LASTEXITCODE -ne 0) { throw 'R4-A2 test compile failed.' }
    $cp = (Join-Path $repo 'target/classes') + ';' + $dest
    & java -cp $cp com.quanlynhakhoa.view.R4MainNavigationSelfTest
    if ($LASTEXITCODE -ne 0) { throw 'R4-A2 navigation test failed.' }
} finally { Pop-Location }
