[CmdletBinding()]
param([switch]$SqlWrite, [switch]$SqlRead, [string]$TestId)
$ErrorActionPreference = 'Stop'
if ($SqlWrite -and $SqlRead) { throw 'Choose only one SQL acceptance mode' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repo
try {
    & .\mvnw.cmd -q test-compile dependency:build-classpath '-Dmdep.outputFile=target/service-billing-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
    $classpath = 'target/classes;target/test-classes;' + (Get-Content -Raw -LiteralPath 'target/service-billing-classpath.txt').Trim()
    if ($SqlWrite -or $SqlRead) {
        if (-not $TestId) { throw 'Supply a fresh synthetic -TestId starting with TST (max 10 characters)' }
        $mode = if ($SqlWrite) { '--write' } else { '--read' }
        & java -cp $classpath com.quanlynhakhoa.billing.ServiceBillingSqlIT $mode $TestId
    } else {
        & java -cp $classpath com.quanlynhakhoa.billing.ui.ServiceBillingLauncher
    }
    if ($LASTEXITCODE -ne 0) { throw 'Service/Billing launcher failed' }
} finally { Pop-Location }
