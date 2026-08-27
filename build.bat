@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "BUILD_HELPER_REVISION=1.0.0"
set "GRADLE_VERSION=8.10.2"
set "GRADLE_SHA256=31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26"
set "DIST_ROOT=%CD%\.gradle-dist"
set "DIST_DIR=%DIST_ROOT%\gradle-%GRADLE_VERSION%"
set "DIST_ZIP=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"
set "JAVA_HOME_21="
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
echo Build helper revision: %BUILD_HELPER_REVISION%
echo Script: %~f0
echo Directory: %CD%
echo.

rem ------------------------------------------------------------
rem 1. Select Java 21.
rem
rem IMPORTANT: Gradle itself must START on Java 21. The Java
rem toolchain configured in build.gradle is too late to fix a
rem Gradle process that was already launched by Java 17/25.
rem ------------------------------------------------------------

rem Explicit per-project override. This is intentionally trusted when the
rem expected java.exe exists; java -version is printed immediately below.
if defined PICK_RELAY_JAVA_HOME (
    echo PICK_RELAY_JAVA_HOME is set to:
    echo   %PICK_RELAY_JAVA_HOME%
    if exist "%PICK_RELAY_JAVA_HOME%\bin\java.exe" (
        set "JAVA_HOME_21=%PICK_RELAY_JAVA_HOME%"
    ) else (
        echo WARNING: PICK_RELAY_JAVA_HOME does not contain bin\java.exe.
    )
    echo.
)

rem Common Temurin / Eclipse Adoptium layout. Do not care which Java is first
rem on PATH and do not care where the global JAVA_HOME points.
if not defined JAVA_HOME_21 if defined ProgramFiles (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
        if not defined JAVA_HOME_21 if exist "%%~fD\bin\java.exe" set "JAVA_HOME_21=%%~fD"
    )
)

rem Fallback: inspect every java.exe on PATH and accept paths belonging to a
rem jdk-21* directory. This covers the user's side-by-side JDK layout.
if not defined JAVA_HOME_21 (
    for /f "delims=" %%J in ('where java.exe 2^>nul') do (
        set "CANDIDATE_JAVA=%%~fJ"
        echo !CANDIDATE_JAVA! | findstr /I /C:"\jdk-21" >nul
        if not errorlevel 1 if not defined JAVA_HOME_21 (
            for %%B in ("%%~dpJ..") do set "JAVA_HOME_21=%%~fB"
        )
    )
)

rem Generic fallback for Prism or vendors with non-standard directory names.
if not defined JAVA_HOME_21 (
    set "JAVA_FINDER=%CD%\tools\find-java21.ps1"
    if exist "!JAVA_FINDER!" (
        for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -File "!JAVA_FINDER!"`) do (
            if not defined JAVA_HOME_21 (
                for %%B in ("%%~dpJ..") do set "JAVA_HOME_21=%%~fB"
            )
        )
    )
)

if not defined JAVA_HOME_21 goto :java21_missing
set "JAVA_EXE=%JAVA_HOME_21%\bin\java.exe"
if not exist "%JAVA_EXE%" goto :java21_missing

set "JAVA_HOME=%JAVA_HOME_21%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Java selected for this build:
echo   JAVA_HOME=%JAVA_HOME%
echo   JAVA_EXE=%JAVA_EXE%
echo.
"%JAVA_EXE%" -version
if errorlevel 1 goto :java_broken

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

rem Re-print the actual Gradle launcher Java now that Gradle definitely exists.
echo.
echo Gradle runtime:
call "%DIST_DIR%\bin\gradle.bat" --version | findstr /I /C:"Launcher JVM" /C:"Daemon JVM" /C:"JVM:"
echo.

rem ------------------------------------------------------------
rem 3. Build.
rem ------------------------------------------------------------
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

:java21_missing
echo.
echo ERROR: Pick Relay could not locate a Java 21 JDK.
echo.
echo Detected environment:
echo   PICK_RELAY_JAVA_HOME=%PICK_RELAY_JAVA_HOME%
echo   JAVA_HOME=%JAVA_HOME%
echo.
echo Java installations visible through PATH:
where java.exe 2>nul
if errorlevel 1 echo   ^(none^)
echo.
echo Expected examples:
echo   C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot

echo You can force the project JDK in the SAME CMD window with:
echo   set "PICK_RELAY_JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
echo   call build.bat
goto :failure

:java_broken
echo ERROR: The selected Java installation cannot be executed.
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
