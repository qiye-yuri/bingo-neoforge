$ErrorActionPreference = "Stop"

$repositoryPath = (Get-Location).Path
Set-Location -LiteralPath $repositoryPath

try {
    $javaVersion = (& cmd.exe /d /c "java -version 2>&1" | Out-String)
    if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "21(?:\.|\")') {
        throw "Java 21 was not found. Install Java 21 and add java to PATH."
    }

    Write-Host "Building Neo Bingo with Gradle..." -ForegroundColor Cyan
    & "$repositoryPath\gradlew.bat" build --no-configuration-cache
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE."
    }

    $libraryDirectory = Join-Path $repositoryPath "build\libs"
    Write-Host "Scanning $libraryDirectory"
    $jar = $null
    $candidates = @(Get-ChildItem -LiteralPath $libraryDirectory -Filter "*.jar" -File |
        Sort-Object LastWriteTimeUtc -Descending)
    foreach ($candidate in $candidates) {
        if ($candidate.Name -notmatch '-(?:sources|javadoc|plain)\.jar$') {
            $jar = $candidate
            break
        }
    }
    if ($null -eq $jar) {
        throw "No installable mod JAR was found in build/libs."
    }

    $entries = & jar tf $jar.FullName
    if ($LASTEXITCODE -ne 0 -or $entries -notcontains "META-INF/neoforge.mods.toml") {
        throw "The generated JAR does not contain NeoForge mod metadata."
    }

    $distDirectory = Join-Path $repositoryPath "dist"
    New-Item -ItemType Directory -Force -Path $distDirectory | Out-Null
    $output = Join-Path $distDirectory $jar.Name
    Copy-Item -LiteralPath $jar.FullName -Destination $output -Force

    $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $($jar.Name)" | Set-Content -LiteralPath "$output.sha256" -Encoding ascii

    Write-Host ""
    Write-Host "Build complete. Installable file:" -ForegroundColor Green
    Write-Host $output
    Write-Host "SHA-256: $hash"
    exit 0
} catch {
    Write-Host ""
    Write-Host "Build failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
