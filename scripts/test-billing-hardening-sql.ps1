[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = Join-Path $repo '.n08-dev.local.properties'
if (!(Test-Path -LiteralPath (Join-Path $repo 'pom.xml')) -or !(Test-Path -LiteralPath $configPath)) {
    throw 'Run against the existing configured Maven development worktree.'
}

# Read target identifiers only. Never use or output the application password here.
$settings = @{}
foreach ($line in [IO.File]::ReadAllLines($configPath)) {
    foreach ($key in @('dev.database', 'dev.port', 'dev.user')) {
        if ($line.StartsWith($key + '=')) { $settings[$key] = $line.Substring($key.Length + 1).Trim() }
    }
}
$database = $settings['dev.database']
$appUser = $settings['dev.user']
if ($database -notmatch '^N08Dev_[0-9a-f]{8}$' -or
    $appUser -cne ('n08dev_' + $database.Substring(7)) -or
    $settings['dev.port'] -notmatch '^\d{1,5}$') { throw 'Invalid isolated development target.' }
$port = [int]$settings['dev.port']
if ($port -lt 1 -or $port -gt 65535) { throw 'Invalid local SQL port.' }

# Match the configured TCP endpoint to its local instance before using shared memory.
# This avoids SSPI/SPN issues with TCP loopback while preserving the exact DB target.
$instances = Get-ItemProperty -LiteralPath 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL'
$instanceName = $null
foreach ($instance in $instances.PSObject.Properties) {
    if ($instance.Name -like 'PS*') { continue }
    $tcp = Get-ItemProperty -LiteralPath ('HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\' + $instance.Value + '\MSSQLServer\SuperSocketNetLib\Tcp\IPAll') -ErrorAction SilentlyContinue
    if ($null -eq $tcp) { continue }
    if ($tcp.TcpPort -eq [string]$port -or $tcp.TcpDynamicPorts -eq [string]$port) {
        if ($null -ne $instanceName) { throw 'Ambiguous development SQL instance.' }
        $instanceName = $instance.Name
    }
}
if ($null -eq $instanceName -or $instanceName -notmatch '^[a-zA-Z0-9_]+$') { throw 'No matching local SQL instance.' }
$server = if ($instanceName -eq 'MSSQLSERVER') { 'lpc:.' } else { 'lpc:.\' + $instanceName }
Add-Type -AssemblyName System.Data
$builder = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder['Data Source'] = $server
$builder['Initial Catalog'] = $database
$builder['Integrated Security'] = $true
$builder['Connect Timeout'] = 8
$builder['Application Name'] = 'N08-Billing-Workflow-Migration'
$admin = New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)

$testDatabase='N08Dev_'+[Guid]::NewGuid().ToString('N').Substring(0,8)
try {
    $admin.Open();$admin.ChangeDatabase('master')
    $command=$admin.CreateCommand()
    try {
        $command.CommandText="CREATE DATABASE [$testDatabase]";[void]$command.ExecuteNonQuery()
        $admin.ChangeDatabase($testDatabase)
        foreach($migration in Get-ChildItem -LiteralPath (Join-Path $repo 'sql/bootstrap') -Filter 'V*.sql' | Sort-Object Name){
            $command.CommandTimeout=90;$command.CommandText=[IO.File]::ReadAllText($migration.FullName);[void]$command.ExecuteNonQuery()
        }
        $command.CommandText="CREATE USER [$appUser] FOR LOGIN [$appUser]; ALTER ROLE db_owner ADD MEMBER [$appUser];";[void]$command.ExecuteNonQuery()
        [IO.File]::WriteAllText((Join-Path $repo 'target/billing-test-database.txt'),$testDatabase)
        Write-Host 'Created isolated synthetic billing test database (configured application data is untouched).'
    }finally{$command.Dispose()}
}finally{$admin.Dispose()}
Push-Location $repo
try{
    & .\mvnw.cmd -q test-compile
    if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
    $cp='target/classes;target/test-classes;'+([IO.File]::ReadAllText((Join-Path $repo 'target/test-runtime-classpath.txt'))).Trim()
    & java -cp $cp com.quanlynhakhoa.nghiepvu.hoadon.BillingHardeningSqlIT --synthetic-database $testDatabase
    if($LASTEXITCODE -ne 0){throw 'Billing hardening SQL acceptance failed'}
}finally{Pop-Location}
