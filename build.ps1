param(
    [string]$JavaFxLib = $env:JAVAFX_LIB,
    [switch]$Test,
    [switch]$UiTest,
    [switch]$Run
)
$ErrorActionPreference = "Stop"
if (-not $JavaFxLib) {
    $JavaFxLib = Join-Path $PSScriptRoot ".build/lib"
    if (-not (Test-Path (Join-Path $JavaFxLib "javafx*controls*.jar"))) {
        & (Join-Path $PSScriptRoot "setup.ps1")
    }
}
# SDK jars use javafx.controls.jar; Maven jars use javafx-controls-<version>.jar.
if (-not (Test-Path -LiteralPath $JavaFxLib -PathType Container)) {
    throw "JavaFX directory does not exist: $JavaFxLib. Set JAVAFX_LIB or pass -JavaFxLib pointing to the JavaFX 21 SDK lib directory."
}
$JavaFxLib = (Resolve-Path -LiteralPath $JavaFxLib).Path
$javaFxJars = @(Get-ChildItem -LiteralPath $JavaFxLib -Filter *.jar -File)
foreach ($component in @("base", "graphics", "controls")) {
    $pattern = '^javafx[.-]' + $component + '(?:-[0-9][A-Za-z0-9.-]*)?\.jar$'
    if (-not ($javaFxJars | Where-Object { $_.Name -match $pattern })) {
        throw "JavaFX $component jar is missing from '$JavaFxLib'. Pass the SDK lib directory, which contains javafx.base.jar, javafx.graphics.jar, and javafx.controls.jar."
    }
}
$dependencyDir = Join-Path $PSScriptRoot ".build/dependencies"
if (-not (Test-Path (Join-Path $dependencyDir "sqlite-jdbc-3.47.2.0.jar")) -or
    -not (Test-Path (Join-Path $dependencyDir "slf4j-api-2.0.16.jar")) -or
    -not (Test-Path (Join-Path $dependencyDir "slf4j-nop-2.0.16.jar"))) {
    & (Join-Path $PSScriptRoot "setup.ps1") -JavaFxLib $JavaFxLib
}
$output = Join-Path $PSScriptRoot ".build/classes"
New-Item -ItemType Directory -Force $output | Out-Null
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot "Store/src") -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac --release 21 --module-path $JavaFxLib --add-modules javafx.controls -d $output $sources
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "Store/src/View/theme.css") -Destination (Join-Path $output "View/theme.css")
if ($Test) {
    & javac --release 21 --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" -d $output (Join-Path $PSScriptRoot "Store/test/RegressionTests.java")
    if ($LASTEXITCODE -ne 0) { throw "Test compilation failed." }
    & javac --release 21 --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" -d $output (Join-Path $PSScriptRoot "Store/test/LegacyMigrationTest.java")
    if ($LASTEXITCODE -ne 0) { throw "Migration test compilation failed." }
    Push-Location $PSScriptRoot
    try {
        & java --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" RegressionTests
        if ($LASTEXITCODE -ne 0) { throw "Regression tests failed." }
        & java --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" LegacyMigrationTest
        if ($LASTEXITCODE -ne 0) { throw "Legacy migration test failed." }
    } finally { Pop-Location }
}
if ($UiTest) {
    & javac --release 21 --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" -d $output (Join-Path $PSScriptRoot "Store/test/UiSmokeTest.java")
    if ($LASTEXITCODE -ne 0) { throw "UI smoke test compilation failed." }
    Push-Location $PSScriptRoot
    try {
        & java --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" UiSmokeTest
        if ($LASTEXITCODE -ne 0) { throw "UI smoke test failed." }
    } finally { Pop-Location }
}
if ($Run) {
    & java "-Dstore.home=$PSScriptRoot/Store" --module-path $JavaFxLib --add-modules javafx.controls -cp "$output;$dependencyDir/*" Main.Main
    if ($LASTEXITCODE -ne 0) { throw "Application failed." }
}
