$ErrorActionPreference = "Stop"

$repositoryPath = (Get-Location).Path
Set-Location -LiteralPath $repositoryPath

try {
    $javaCommand = "java"
    $jarCommand = "jar"
    if ($env:JAVA_HOME) {
        $javaHomeCommand = Join-Path $env:JAVA_HOME "bin\java.exe"
        $jarHomeCommand = Join-Path $env:JAVA_HOME "bin\jar.exe"
        if ((Test-Path -LiteralPath $javaHomeCommand) -and (Test-Path -LiteralPath $jarHomeCommand)) {
            $javaCommand = $javaHomeCommand
            $jarCommand = $jarHomeCommand
        }
    }
    if (-not $env:GRADLE_USER_HOME -and $env:USERPROFILE) {
        $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
    }

    $savedErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $javaVersion = (& $javaCommand -version 2>&1 | Out-String)
    $javaExitCode = $LASTEXITCODE
    $ErrorActionPreference = $savedErrorActionPreference
    if ($javaExitCode -ne 0 -or $javaVersion -notmatch 'version "21(?:\.|\")') {
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

    $entries = & $jarCommand tf $jar.FullName
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
