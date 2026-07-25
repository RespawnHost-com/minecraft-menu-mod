$ErrorActionPreference = "Stop"

$jdk8  = "C:\Program Files\BellSoft\LibericaJDK-8-Full"
$jdk17 = "C:\Program Files\BellSoft\LibericaJDK-17-Full"
$jdk21 = "C:\Program Files\BellSoft\LibericaJDK-21-Full"
$jdk26 = "C:\Program Files\BellSoft\LibericaJDK-26-Full"
$defaultJavaHome = $jdk21
$javaHomes = @{
    "versions\1.12.2-forge"    = $jdk8
    "versions\1.13.2-forge"    = $jdk8
    "versions\1.14.4-forge"    = $jdk8
    "versions\1.15.2-forge"    = $jdk8
    "versions\1.14.4-fabric"   = $jdk17
    "versions\1.15.2-fabric"   = $jdk17
    "versions\1.16.5-forge"    = $jdk8
    "versions\1.16.5-fabric"   = $jdk17
    "versions\1.17.1-forge"    = $jdk17
    "versions\1.17.1-fabric"   = $jdk17
    "versions\1.18.2-forge"    = $jdk17
    "versions\1.18.2-fabric"   = $jdk17
    "versions\1.19.2-forge"    = $jdk17
    "versions\1.19.2-fabric"   = $jdk17
    "versions\1.20.1-forge"    = $jdk17
    "versions\1.20.1-fabric"   = $jdk17
    "versions\26.1-fabric"     = $jdk26
    "versions\26.1-neoforge"   = $jdk26
}

$targets = @("core")
$targets += Get-ChildItem -Directory "versions" | ForEach-Object { "versions\$($_.Name)" }

$results = @()
foreach ($target in $targets) {
    $javaHome = if ($javaHomes.ContainsKey($target)) { $javaHomes[$target] } else { $defaultJavaHome }
    Write-Host "=== Building $target (JAVA_HOME=$javaHome) ==="
    $env:JAVA_HOME = $javaHome
    Push-Location $target
    try {
        & ".\gradlew.bat" build --no-daemon
        if ($LASTEXITCODE -ne 0) {
            $results += [pscustomobject]@{ Target = $target; Status = "FAILED" }
            Write-Host "=== $target FAILED, stopping ==="
            break
        }
        $results += [pscustomobject]@{ Target = $target; Status = "OK" }
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "=== Summary ==="
foreach ($result in $results) {
    Write-Host ("{0,-20} {1}" -f $result.Target, $result.Status)
}
if ($results.Status -contains "FAILED") {
    exit 1
}
