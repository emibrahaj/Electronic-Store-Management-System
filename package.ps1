param(
    [string]$JavaFxLib = $env:JAVAFX_LIB,
    [switch]$Installer,
    [string]$Version = "1.0.0"
)
$ErrorActionPreference = "Stop"
if (-not $JavaFxLib) {
    $JavaFxLib = Join-Path $PSScriptRoot ".build/lib"
    if (-not (Test-Path (Join-Path $JavaFxLib "javafx*controls*.jar"))) { & (Join-Path $PSScriptRoot "setup.ps1") }
}
& (Join-Path $PSScriptRoot "build.ps1") -JavaFxLib $JavaFxLib
$work = Join-Path $PSScriptRoot (".build/package-" + [guid]::NewGuid().ToString("N"))
$inputDir = Join-Path $work "input"
New-Item -ItemType Directory -Force $inputDir | Out-Null
Copy-Item -Path (Join-Path $JavaFxLib "*.jar") -Destination $inputDir
Copy-Item -Path (Join-Path $PSScriptRoot ".build/dependencies/*.jar") -Destination $inputDir
# The manifest names every dependency, so the native launcher finds the same jars as development.
$dependencyNames = (Get-ChildItem -LiteralPath $inputDir -Filter *.jar | ForEach-Object { $_.Name }) -join " "
$header = "Class-Path: $dependencyNames"
$manifestLines = @("Manifest-Version: 1.0", "Main-Class: Main.Launcher")
while ($header.Length -gt 70) {
    $manifestLines += $header.Substring(0,70)
    $header = " " + $header.Substring(70)
}
$manifestLines += $header
$manifestLines += ""
$manifest = Join-Path $work "MANIFEST.MF"
[System.IO.File]::WriteAllText($manifest, ($manifestLines -join "`r`n") + "`r`n", [System.Text.Encoding]::ASCII)
# Explicit application packages exclude all regression runner classes from distribution.
$jarArgs = @("--create", "--file", (Join-Path $inputDir "storepos.jar"), "--manifest", $manifest)
foreach ($packageName in @("Main", "Model", "DAO", "Controller", "Util", "View")) {
    $jarArgs += @("-C", (Join-Path $PSScriptRoot ".build/classes"), $packageName)
}
& jar @jarArgs
if ($LASTEXITCODE -ne 0) { throw "Application jar creation failed." }
$dist = Join-Path $PSScriptRoot ("dist/StorePOS-" + $Version + "-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Force $dist | Out-Null
& jpackage --type app-image --input $inputDir --dest $dist --name StorePOS --app-version $Version --main-jar storepos.jar --main-class Main.Launcher --vendor StorePOS --add-modules java.base,java.desktop,java.sql,java.naming,java.logging,java.xml,jdk.unsupported,jdk.crypto.ec
if ($LASTEXITCODE -ne 0) { throw "Bundled application creation failed." }
if ($Installer) {
    # WiX is only a local build dependency; nothing is installed system-wide.
    $wix = Join-Path $PSScriptRoot ".build/tools/wix3"
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
        if (-not (Test-Path (Join-Path $wix "candle.exe"))) {
            $zip = Join-Path $work "wix.zip"
            Invoke-WebRequest -UseBasicParsing -Uri "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip" -OutFile $zip
            Expand-Archive -LiteralPath $zip -DestinationPath $wix -Force
        }
        $env:PATH = "$wix;$env:PATH"
    }
    & jpackage --type msi --app-image (Join-Path $dist "StorePOS") --dest $dist --name StorePOS --app-version $Version --vendor StorePOS --win-per-user-install --win-menu --win-shortcut --win-upgrade-uuid 8ba1e250-7494-47f7-bf33-a4bb6e3c5dc0
    if ($LASTEXITCODE -ne 0) { throw "MSI creation failed. The portable app is still available at $dist/StorePOS." }
}
Write-Output "Built application: $dist"
