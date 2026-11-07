param([string]$JavaFxLib = $env:JAVAFX_LIB)
$ErrorActionPreference = "Stop"
$lib = Join-Path $PSScriptRoot ".build/dependencies"
New-Item -ItemType Directory -Force $lib | Out-Null
$artifacts = @(
    @{Path="org/xerial/sqlite-jdbc/3.47.2.0"; Name="sqlite-jdbc-3.47.2.0.jar"},
    @{Path="org/slf4j/slf4j-api/2.0.16"; Name="slf4j-api-2.0.16.jar"},
    @{Path="org/slf4j/slf4j-nop/2.0.16"; Name="slf4j-nop-2.0.16.jar"}
)
foreach ($artifact in $artifacts) {
    $target = Join-Path $lib $artifact.Name
    if (-not (Test-Path -LiteralPath $target)) {
        $temporary = "$target.download"
        try {
            Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/$($artifact.Path)/$($artifact.Name)" -OutFile $temporary
            Move-Item -LiteralPath $temporary -Destination $target -Force
        } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary } }
    }
}
if (-not $JavaFxLib) {
    $fx = Join-Path $PSScriptRoot ".build/lib"
    New-Item -ItemType Directory -Force $fx | Out-Null
    foreach ($component in @("base", "graphics", "controls")) {
        if (-not (Get-ChildItem -LiteralPath $fx -Filter "javafx*$component*.jar")) {
            $name = "javafx-$component-21.0.2-win.jar"
            Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/org/openjfx/javafx-$component/21.0.2/$name" -OutFile (Join-Path $fx $name)
        }
    }
}
