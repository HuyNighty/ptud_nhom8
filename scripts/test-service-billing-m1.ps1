[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repo
try {
    & .\mvnw.cmd -q test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Maven test-compile failed' }
    $classpath = 'target/classes;target/test-classes'
    foreach ($test in @('ServiceBillingSelfTest', 'ServiceBillingUiSelfTest', 'InvoiceCreationSelfTest', 'ClinicalBillingSourceSelfTest', 'PaymentSelfTest', 'CatalogSeparationSelfTest', 'CancellationSelfTest', 'CancellationConcurrencySelfTest', 'LatestShellUiSelfTest', 'BillingRefinementSelfTest')) {
        & java '-Djava.awt.headless=true' -cp $classpath "com.quanlynhakhoa.billing.$test"
        if ($LASTEXITCODE -ne 0) { throw "$test failed" }
    }
} finally { Pop-Location }
