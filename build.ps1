# Builds the White Waves MIDlet: javac (against CLDC1.1/MIDP2.0 API from KEmulator) -> preverify -> jar -> jad.
# Requires KEmulator (winget install --id shinovon.KEmnn), which bundles the CLDC/MIDP API stubs and preverify.exe.

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$srcDir = Join-Path $root 'src'
$buildDir = Join-Path $root 'build'
$classesDir = Join-Path $buildDir 'classes'
$verifiedDir = Join-Path $buildDir 'verified'
$jarPath = Join-Path $buildDir 'white-waves.jar'
$jadPath = Join-Path $buildDir 'white-waves.jad'
$manifestSrc = Join-Path $root 'MANIFEST.MF'
$jadTemplate = Join-Path $root 'white-waves.jad'

$kemulatorPkg = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like 'shinovon.KEmnn*' } | Select-Object -First 1
if (-not $kemulatorPkg) {
    throw "KEmulator not found. Install with: winget install --id shinovon.KEmnn"
}
$uei = Join-Path $kemulatorPkg.FullName 'kemnnmod\uei'
$cldc = Join-Path $uei 'cldc11.jar'
$midp = Join-Path $uei 'midp21.jar'
$preverify = Join-Path $uei 'preverify.exe'
$bootcp = "$cldc;$midp"

if (-not (Test-Path $cldc) -or -not (Test-Path $midp) -or -not (Test-Path $preverify)) {
    throw "CLDC/MIDP API or preverify.exe not found under $uei"
}

Write-Output "Cleaning build/..."
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $verifiedDir | Out-Null

Write-Output "Compiling..."
$sourceFiles = @(Get-ChildItem -Path $srcDir -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -source 7 -target 7 -nowarn -bootclasspath $bootcp -d $classesDir @sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed"
}

Write-Output "Preverifying..."
& $preverify -classpath "$bootcp;$classesDir" -d $verifiedDir $classesDir
if ($LASTEXITCODE -ne 0) {
    throw "Preverify failed"
}

Write-Output "Patching class file version for CLDC (45.3)..."
# JDK17's javac cannot emit class files older than major version 51 (-source/-target 7 is
# its floor), but KEmulator's bundled ASM class reader only accepts the classic CLDC-preverified
# major.minor version 45.3. preverify.exe does not rewrite this header field itself, so patch it
# by hand after preverify - the actual verification data preverify wrote is untouched.
Get-ChildItem -Path $verifiedDir -Recurse -Filter '*.class' | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $bytes[4] = 0; $bytes[5] = 3   # minor = 3
    $bytes[6] = 0; $bytes[7] = 45  # major = 45
    [System.IO.File]::WriteAllBytes($_.FullName, $bytes)
}

Write-Output "Packaging jar..."
& jar cfm $jarPath $manifestSrc -C $verifiedDir .
if ($LASTEXITCODE -ne 0) {
    throw "Jar packaging failed"
}

Write-Output "Writing jad..."
$jarSize = (Get-Item $jarPath).Length
$jadLines = Get-Content $jadTemplate | ForEach-Object {
    if ($_ -match '^MIDlet-Jar-Size:') { "MIDlet-Jar-Size: $jarSize" } else { $_ }
}
[System.IO.File]::WriteAllLines($jadPath, $jadLines, [System.Text.Encoding]::ASCII)

Write-Output "Done: $jarPath ($jarSize bytes)"
