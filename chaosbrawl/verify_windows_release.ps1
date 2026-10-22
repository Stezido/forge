param(
  [Parameter(Mandatory=$true)][string]$Archive,
  [Parameter(Mandatory=$true)][string]$Manifest,
  [Parameter(Mandatory=$true)][string]$Destination,
  [string]$Jdk = 'C:\Program Files\Java\jdk-25'
)
$ErrorActionPreference = 'Stop'
$release = Get-Content -Raw $Manifest | ConvertFrom-Json
if ((Get-FileHash -Algorithm SHA256 $Archive).Hash.ToLower() -ne $release.artifacts.windows.sha256) { throw 'Release ZIP checksum mismatch' }
if (Test-Path $Destination) { throw 'Refusing to reuse an installation or test directory' }
New-Item -ItemType Directory $Destination | Out-Null
& tar.exe -xf $Archive -C $Destination
if ($LASTEXITCODE -ne 0) { throw 'Release extraction failed' }
$pkg = Join-Path $Destination ('ChaosBrawl-Forge-' + $release.version + '-Windows-x64')
$checks = Join-Path $Destination 'checks'
New-Item -ItemType Directory $checks | Out-Null
Set-Content (Join-Path $pkg 'Decks\constructed\Release persistence.dck') "[metadata]`nName=Release persistence`n[Main]`n60 Island"
$deckHash = (Get-FileHash (Join-Path $pkg 'Decks\constructed\Release persistence.dck')).Hash
Set-Location $pkg
$env:SENTRY_ENABLED = 'false'
foreach ($name in @('VerifyWindowsLaunchers','VerifyCustomMechanics','VerifyPackageImages')) {
  & "$Jdk\bin\javac.exe" --release 21 -cp 'support\forge.jar' -d $checks (Join-Path $PSScriptRoot ($name + '.java'))
  if ($LASTEXITCODE -ne 0) { throw "Audit compilation failed: $name" }
}
function Invoke-PackagedJava([string[]]$JavaArgs, [string]$LogName) {
  $arguments = @($JavaArgs | ForEach-Object { '"' + $_ + '"' })
  $proc = Start-Process -FilePath (Join-Path $pkg 'support\runtime\bin\java.exe') -ArgumentList $arguments -WorkingDirectory $pkg -Wait -PassThru -RedirectStandardOutput (Join-Path $checks ($LogName + '.log')) -RedirectStandardError (Join-Path $checks ($LogName + '-stderr.log'))
  if ($proc.ExitCode -ne 0) {
    Get-Content (Join-Path $checks ($LogName + '.log')) -Tail 20
    Get-Content (Join-Path $checks ($LogName + '-stderr.log')) -Tail 20
    throw "Packaged Java check failed: $LogName"
  }
}
Invoke-PackagedJava @('-cp', "$checks;support\forge.jar", 'VerifyWindowsLaunchers', $pkg) 'launchers'
Invoke-PackagedJava @('-Xmx4g', '-Djava.awt.headless=true', '-cp', "$checks;support\forge.jar", 'VerifyPackageImages', $pkg, [string]$release.nonTokenRecords) 'images'
foreach ($jar in @('forge.jar','forge-adventure.jar')) {
  Invoke-PackagedJava @('-Xmx4g', '-Dforge.chaosbrawl=true', '-Dsentry.enabled=false', '-Djava.awt.headless=true', '-cp', "$checks;support\$jar", 'VerifyCustomMechanics') ($jar + '-mechanics')
}
$launches = @()
foreach ($entry in @('Play Chaos Brawl.lnk','Play Adventure.lnk','Chaos Brawl Forge.exe','Chaos Brawl Adventure.exe')) {
  # Snapshot existing process IDs; only stop the newly launched package process.
  $before = @(Get-Process javaw -ErrorAction SilentlyContinue | ForEach-Object Id)
  Start-Process -FilePath (Join-Path $pkg $entry) -WorkingDirectory $env:TEMP
  $gui = $null
  for ($attempt = 0; $attempt -lt 30; $attempt++) {
    Start-Sleep -Seconds 1
    $gui = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object { $before -notcontains $_.Id -and $_.MainWindowHandle -ne 0 -and $_.Path -eq (Join-Path $pkg 'support\runtime\bin\javaw.exe') } | Select-Object -First 1
    if ($gui) { break }
  }
  if (!$gui) { throw "Shipped launcher did not create a GUI window: $entry" }
  try {
    $command = (Get-CimInstance Win32_Process -Filter "ProcessId=$($gui.Id)").CommandLine
    if (!$command.Contains('-Dforge.chaosbrawl=true')) { throw "Custom mechanics disabled by launcher: $entry" }
    $expectedJar = if ($entry.Contains('Adventure')) { 'forge-adventure.jar' } else { 'forge.jar' }
    if (!$command.Contains($expectedJar)) { throw "Wrong engine selected: $entry" }
    $launches += @{entry=$entry;customMode=$true;windowTitle=$gui.MainWindowTitle}
    Write-Output "NATIVE_LAUNCH_PASSED: $entry with custom mechanics enabled"
  } finally { Stop-Process -Id $gui.Id }
}
if ((Get-FileHash (Join-Path $pkg 'Decks\constructed\Release persistence.dck')).Hash -ne $deckHash) { throw 'Synthetic deck not preserved' }
@{ version=$release.version;zipSha256=$release.artifacts.windows.sha256;launchers=$launches;beerDraftTapCasesPerEngine=21;engines=2;imagePrintings=$release.nonTokenRecords;syntheticDeckPreserved=$true } | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $Destination 'windows-verification.json')
Write-Output 'WINDOWS_RELEASE_PASSED'
