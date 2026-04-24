#requires -version 5
# Memory Inspector - dev build (PowerShell mirror of build.bat).
# Produces a thin jar; fat/uber jar is deferred to M8.

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $jscJar = 'lib\jSerialComm-2.11.4.jar'
    if (-not (Test-Path $jscJar)) {
        throw "$jscJar not found. See truenorth.md H1."
    }

    foreach ($d in @('out', 'Logs')) {
        if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d | Out-Null }
    }

    # Resolve jar.exe. Prefer JAVA_HOME, else probe the live JDK via java's own settings.
    # (java writes its banner to stderr, so we relax ErrorActionPreference around the probe.)
    $jarExe = $null
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jar.exe'))) {
        $jarExe = Join-Path $env:JAVA_HOME 'bin\jar.exe'
    }
    else {
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $settings = (& java -XshowSettings:properties -version 2>&1 | Out-String) -split "`r?`n"
        }
        finally {
            $ErrorActionPreference = $prevEap
        }
        $match = $settings | Select-String -Pattern '^\s*java\.home\s*=\s*(.+)$' | Select-Object -First 1
        if ($match) {
            $javaHome = $match.Matches[0].Groups[1].Value.Trim()
            $candidate = Join-Path $javaHome 'bin\jar.exe'
            if (Test-Path $candidate) { $jarExe = $candidate }
        }
    }
    if (-not $jarExe) {
        throw "Could not locate jar.exe. Set JAVA_HOME to the JDK root (e.g. C:\Program Files\Java\jdk-23) or add its \bin to PATH."
    }

    $sources = Get-ChildItem -Path src -Recurse -Filter *.java -ErrorAction SilentlyContinue
    if (-not $sources) {
        Write-Host '[build] No Java sources under src\ yet. Nothing to compile.'
        return
    }

    $sourceList = 'out\sources.txt'
    $sources | ForEach-Object { $_.FullName } | Set-Content -Path $sourceList -Encoding ASCII

    Write-Host "[build] Compiling $($sources.Count) source file(s)..."
    & javac -encoding UTF-8 -d out -cp $jscJar "@$sourceList"
    if ($LASTEXITCODE -ne 0) { throw "javac failed ($LASTEXITCODE)" }

    Write-Host '[build] Packaging MemoryInspector.jar (thin jar)...'
    & $jarExe cfe MemoryInspector.jar app.Main -C out '.'
    if ($LASTEXITCODE -ne 0) { throw "jar failed ($LASTEXITCODE)" }

    Write-Host '[build] Done. Output: MemoryInspector.jar'
}
finally {
    Pop-Location
}
