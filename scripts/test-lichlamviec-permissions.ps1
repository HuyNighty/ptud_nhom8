[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'grant-lichlamviec-dev.ps1'
$errors = $null; $tokens = $null
$ast = [System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) { throw 'Grant script parse failed.' }
$actual = @(& $scriptPath -Plan)
$expected = @(
    'NhanVien|SELECT|', 'TaiKhoan|SELECT|', 'CaLamViec|SELECT|',
    'PhanCong|SELECT|', 'ThamSoHeThong|SELECT|', 'CaLamViec|INSERT|', 'PhanCong|INSERT|',
    'PhanCong|UPDATE|trangThai', 'PhanCong|UPDATE|lyDoHuy', 'NhatKyThaoTac|INSERT|'
)
$labels = @($actual | ForEach-Object { $_.Table + '|' + $_.Privilege + '|' + $_.Column })
if ($labels.Count -ne 10 -or (Compare-Object $expected $labels)) { throw 'Permission matrix differs from DAO scope.' }
foreach ($argsSet in @(@{}, @{Check=$true;Apply=$true}, @{Plan=$true;Apply=$true}, @{Plan=$true;Check=$true})) {
    $rejected = $false
    try { & $scriptPath @argsSet | Out-Null } catch { $rejected = $_.Exception.Message -like 'Choose exactly one*' }
    if (!$rejected) { throw 'Invalid mode combination did not fail before SQL.' }
}
# Load only the guarded mutation function, never the connection or -Apply entrypoint.
$grant = $ast.Find({ param($n) $n -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq 'Invoke-MissingGrant' }, $true)
if ($null -eq $grant) { throw 'Missing write gate.' }
. ([scriptblock]::Create($grant.Extent.Text))
$script:sqlCalls = 0
function Invoke-Scalar([string]$Sql) { $script:sqlCalls++; throw 'Test must never reach SQL.' }
foreach ($flags in @(@($false,$false,$null), @($false,$true,[pscustomobject]@{}), @($true,$false,[pscustomobject]@{}), @($true,$true,$null))) {
    $Apply=$flags[0]; $confirmed=$flags[1]; $transaction=$flags[2]
    $rejected = $false
    try { Invoke-MissingGrant $actual[0] } catch { $rejected = $_.Exception.Message -eq 'Grant write gate is closed.' }
    if (!$rejected) { throw 'Unconfirmed write gate was reachable.' }
}
if ($script:sqlCalls -ne 0) { throw 'Static tests reached SQL.' }
# Test authentication selection on unopened connection objects; mock prompt only.
$auth = $ast.Find({ param($n) $n -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq 'New-ScheduleAdminConnection' }, $true)
if ($null -eq $auth) { throw 'Missing authentication factory.' }
. ([scriptblock]::Create($auth.Extent.Text))
$script:prompts = 0
function Read-Host([string]$Prompt, [switch]$AsSecureString) {
    if (!$AsSecureString) { throw 'Password prompt must use SecureString.' }
    $script:prompts++
    # Synthetic credential, never used to open a connection.
    $secret = New-Object System.Security.SecureString
    foreach ($c in 'offline-fixture-only'.ToCharArray()) { $secret.AppendChar($c) }
    return $secret
}
foreach ($user in @('sa','local.admin')) {
    $c = New-ScheduleAdminConnection -Database 'N08Dev_1234abcd' -Port 1433 -SqlAdminUser $user
    try {
        $b = [System.Data.SqlClient.SqlConnectionStringBuilder]::new($c.ConnectionString)
        if ($c.State -ne 'Closed' -or $b.IntegratedSecurity -or $b.PersistSecurityInfo -or
            $b.DataSource -ne 'tcp:127.0.0.1,1433' -or $b.InitialCatalog -cne 'N08Dev_1234abcd' -or
            !$b.Encrypt -or !$b.TrustServerCertificate -or $b.Password -or $b.UserID -or
            $c.Credential.UserId -cne $user -or !$c.Credential.Password.IsReadOnly()) { throw 'SQL authentication contract failed.' }
    } finally { $secret=$c.Credential.Password; $c.Dispose(); $secret.Dispose() }
}
if ($script:prompts -ne 2) { throw 'SQL authentication did not securely prompt once per connection.' }
$c = New-ScheduleAdminConnection -Database 'N08Dev_1234abcd' -Port 1433 -WindowsAuth
try {
    $b = [System.Data.SqlClient.SqlConnectionStringBuilder]::new($c.ConnectionString)
    if ($c.State -ne 'Closed' -or !$b.IntegratedSecurity -or $null -ne $c.Credential -or $script:prompts -ne 2) { throw 'Windows authentication must not prompt or carry SQL credentials.' }
} finally { $c.Dispose() }
foreach ($case in @(
    @{Database='N08Dental';Port=1433}, @{Database='N08Dev_1234abcd';Port=0},
    @{Database='N08Dev_1234abcd';Port=1433;SqlAdminUser='n08dev_1234abcd'},
    @{Database='N08Dev_1234abcd';Port=1433;SqlAdminUser='bad;login'})) {
    $blocked=$false
    try { New-ScheduleAdminConnection @case | Out-Null } catch { $blocked=$true }
    if (!$blocked) { throw 'Invalid authentication/target accepted.' }
}
foreach ($planArgs in @(@{Plan=$true}, @{Plan=$true;WindowsAuth=$true}, @{Plan=$true;SqlAdminUser='local.admin'})) {
    $plan = @(& $scriptPath @planArgs)
    if ($plan.Count -ne 10) { throw 'Authentication option changed offline matrix.' }
}
if ($script:prompts -ne 2) { throw 'Invalid target or -Plan prompted for credentials.' }
Write-Output 'PASS: offline 10-permission matrix, parser, mode/write gates, SQL secure prompt and Windows authentication selection. All connections remained closed; no Apply entrypoint or SQL executed.'
