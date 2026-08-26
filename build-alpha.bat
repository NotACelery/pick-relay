@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "GRADLE_VERSION=8.10.2"
set "GRADLE_SHA256=31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26"
set "DIST_ROOT=%CD%\.gradle-dist"
set "DIST_DIR=%DIST_ROOT%\gradle-%GRADLE_VERSION%"
set "DIST_ZIP=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"
set "JAVA_EXE="

if not exist "gradle.properties" goto :metadata_missing
for /f "usebackq tokens=1,* delims==" %%A in ("gradle.properties") do (
    if "%%A"=="mod_version" set "MOD_VERSION=%%B"
    if "%%A"=="minecraft_version" set "MINECRAFT_VERSION=%%B"
)
if not defined MOD_VERSION goto :metadata_missing
if not defined MINECRAFT_VERSION goto :metadata_missing

set "EXPECTED_JAR=build\libs\pickrelay-%MINECRAFT_VERSION%-%MOD_VERSION%.jar"
title Pick Relay - %MOD_VERSION% build

echo ============================================================
echo        PICK RELAY - RELEASE BUILD %MOD_VERSION%
echo        MINECRAFT %MINECRAFT_VERSION%
echo ============================================================
echo Directory: %CD%
echo.

rem ------------------------------------------------------------
rem 1. Find Java through PATH, JAVA_HOME, or Prism-managed runtimes.
rem ------------------------------------------------------------
where java.exe >nul 2>nul
if not errorlevel 1 (
    for /f "delims=" %%J in ('where java.exe') do if not defined JAVA_EXE set "JAVA_EXE=%%J"
)

if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVA_EXE (
    echo Java is not in PATH. Searching for a Prism Launcher installation...
    for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$roots=@($env:APPDATA+'\PrismLauncher\java',$env:LOCALAPPDATA+'\PrismLauncher\java',$env:LOCALAPPDATA+'\Programs\PrismLauncher\java',$env:ProgramFiles+'\PrismLauncher\java',$env:ProgramFiles+'\Eclipse Adoptium',$env:ProgramFiles+'\Java'); foreach($root in $roots){if(Test-Path -LiteralPath $root){$found=Get-ChildItem -LiteralPath $root -Filter java.exe -File -Recurse -ErrorAction SilentlyContinue ^| Where-Object {$_.FullName -match '\\bin\\java\.exe$'} ^| Select-Object -First 1; if($found){$found.FullName; break}}}"`) do if not defined JAVA_EXE set "JAVA_EXE=%%J"
)

if not defined JAVA_EXE goto :java_missing
if not exist "%JAVA_EXE%" goto :java_missing

for %%I in ("%JAVA_EXE%") do set "JAVA_BIN=%%~dpI"
for %%I in ("%JAVA_BIN%..") do set "JAVA_HOME=%%~fI"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Java found:
echo   %JAVA_EXE%
"%JAVA_EXE%" -version
if errorlevel 1 goto :java_broken

echo.
rem ------------------------------------------------------------
rem 2. Download a known-compatible Gradle if needed.
rem ------------------------------------------------------------
if not exist "%DIST_DIR%\bin\gradle.bat" (
    if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
    if errorlevel 1 goto :mkdir_failed

    if exist "%DIST_ZIP%" del /q "%DIST_ZIP%"

    echo Downloading Gradle %GRADLE_VERSION%...
    where curl.exe >nul 2>nul
    if not errorlevel 1 (
        curl.exe -L --fail --retry 3 --output "%DIST_ZIP%" "https://downloads.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
        if errorlevel 1 curl.exe -L --fail --retry 3 --output "%DIST_ZIP%" "https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
    ) else (
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -Uri 'https://downloads.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST_ZIP%' -ErrorAction Stop } catch { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST_ZIP%' -ErrorAction Stop }"
    )
    if errorlevel 1 goto :download_failed
    if not exist "%DIST_ZIP%" goto :download_failed

    for /f %%H in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%DIST_ZIP%').Hash.ToLowerInvariant()"') do set "ACTUAL_GRADLE_SHA256=%%H"
    if /I not "!ACTUAL_GRADLE_SHA256!"=="%GRADLE_SHA256%" goto :checksum_failed

    echo Extracting Gradle...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%DIST_ZIP%' -DestinationPath '%DIST_ROOT%' -Force"
    if errorlevel 1 goto :extract_failed
)

if not exist "%DIST_DIR%\bin\gradle.bat" goto :gradle_missing

rem ------------------------------------------------------------
rem 3. Build.
rem ------------------------------------------------------------
echo.
echo Building Pick Relay...
echo The first build may download NeoForge dependencies.
echo.
call "%DIST_DIR%\bin\gradle.bat" --no-daemon clean build --stacktrace
if errorlevel 1 goto :build_failed

if not exist "%EXPECTED_JAR%" goto :jar_missing

echo.
echo ============================================================
echo BUILD COMPLETED SUCCESSFULLY
echo Generated JAR:
echo   %CD%\%EXPECTED_JAR%
echo ============================================================
goto :success

:metadata_missing
echo ERROR: gradle.properties does not define mod_version and minecraft_version.
goto :failure

:java_missing
echo.
echo ERROR: Java was not found to run Gradle. Java 21 is required.
echo Prism can run Minecraft with an internal Java runtime without adding it to PATH.
goto :failure

:java_broken
echo ERROR: The Java installation found cannot be executed.
goto :failure

:mkdir_failed
echo ERROR: Could not create "%DIST_ROOT%".
goto :failure

:download_failed
echo.
echo ERROR: Gradle could not be downloaded from either official host.
goto :failure

:checksum_failed
echo.
echo ERROR: Downloaded Gradle archive failed SHA-256 verification.
if exist "%DIST_ZIP%" del /q "%DIST_ZIP%"
goto :failure

:extract_failed
echo ERROR: Gradle was downloaded but could not be extracted.
echo Delete the .gradle-dist directory and run this file again.
goto :failure

:gradle_missing
echo ERROR: "%DIST_DIR%\bin\gradle.bat" does not exist after extraction.
goto :failure

:build_failed
echo.
echo ============================================================
echo BUILD FAILED
echo Copy everything from "FAILURE: Build failed" to the end and send it.
echo ============================================================
goto :failure

:jar_missing
echo ERROR: Gradle finished, but the expected JAR was not found:
echo   %CD%\%EXPECTED_JAR%
goto :failure

:failure
echo.
echo This window will remain open so the error can be read or copied.
pause
endlocal & exit /b 1

:success
echo.
echo You can copy the JAR to the Prism Launcher instance's mods directory.
pause
endlocal & exit /b 0
