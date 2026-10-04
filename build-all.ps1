param(
    [string[]]$Variants = @(),
    [string]$ModVersion = '1.0.0',
    [hashtable]$JavaHomes = @{}
)
$ErrorActionPreference = 'Stop'
$repo = $PSScriptRoot
$matrix = Get-Content -LiteralPath (Join-Path $repo 'deploy/variants.json') -Raw | ConvertFrom-Json
if ($Variants.Count -gt 0) {
    foreach ($variant in $Variants) {
        if ($variant -notin $matrix.variant) { throw "Unknown variant: $variant" }
    }
    $matrix = $matrix | Where-Object { $_.variant -in $Variants }
}
$previousJavaHome = $env:JAVA_HOME
try {
    foreach ($entry in $matrix) {
        $javaVersion = [string]$entry.java
        $jdk = $JavaHomes[$javaVersion]
        if (-not $jdk) { $jdk = [Environment]::GetEnvironmentVariable("JAVA_HOME_$javaVersion") }
        if (-not $jdk) { $jdk = $previousJavaHome }
        if (-not $jdk -or -not (Test-Path -LiteralPath (Join-Path $jdk 'bin/java.exe'))) {
            throw "Set JAVA_HOME_$javaVersion or pass -JavaHomes @{ '$javaVersion' = 'path/to/jdk' }."
        }
        $javaInfo = & (Join-Path $jdk 'bin/java.exe') -version 2>&1 | Out-String
        $versionPattern = if ($javaVersion -eq '8') { 'version "1\.8\.' } else { 'version "' + $javaVersion + '[."]' }
        if ($javaInfo -notmatch $versionPattern) {
            throw "Variant $($entry.variant) needs JDK $javaVersion; selected JDK: $jdk"
        }
        $env:JAVA_HOME = $jdk
        $project = Join-Path $repo "versions/$($entry.variant)"
        Write-Host "Building $($entry.variant) with JDK $javaVersion"
        & (Join-Path $project 'gradlew.bat') -p $project clean build --no-daemon "-Pmod_version=$ModVersion"
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $($entry.variant)" }
        python (Join-Path $repo 'deploy/artifacts.py') --variant $entry.variant --version $ModVersion --destination (Join-Path $repo 'dist')
        if ($LASTEXITCODE -ne 0) { throw "Artifact collection failed: $($entry.variant)" }
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
