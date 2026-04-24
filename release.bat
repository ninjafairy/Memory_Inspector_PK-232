@echo off
rem Memory Inspector - release / uber-jar builder.
rem Produces a self-contained fat jar at releases\beta-<YYYY-MM-DD>\MemoryInspector.jar
rem that embeds jSerialComm so end-users don't need the lib\ folder to run it.
rem
rem Delegates compilation to build.bat so javac / jar.exe resolution lives in
rem one place; adds the extract-and-repackage step here.

setlocal EnableDelayedExpansion
pushd "%~dp0"
set "EXIT_CODE=0"

set "JSC_JAR=lib\jSerialComm-2.11.4.jar"
if not exist "%JSC_JAR%" (
    echo [release] ERROR: %JSC_JAR% not found. See truenorth.md H1.
    set "EXIT_CODE=1"
    goto :end
)

echo [release] Running build.bat...
call build.bat
if errorlevel 1 (
    echo [release] build.bat FAILED; aborting release.
    set "EXIT_CODE=1"
    goto :end
)

call :resolve_jar
if "%JAR_EXE%"=="" (
    echo [release] ERROR: Could not locate jar.exe.
    echo         Set JAVA_HOME to your JDK root or add the JDK's \bin to PATH.
    set "EXIT_CODE=1"
    goto :end
)

rem Compute today's date via PowerShell (wmic is deprecated on recent Windows).
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd"') do set "RELEASE_DATE=%%i"
if "%RELEASE_DATE%"=="" (
    echo [release] ERROR: Could not determine today's date via PowerShell.
    set "EXIT_CODE=1"
    goto :end
)

set "RELEASE_DIR=releases\beta-%RELEASE_DATE%"
if not exist "%RELEASE_DIR%" mkdir "%RELEASE_DIR%"

rem Staging dir: extract jSerialComm + copy our classes together, repackage.
set "STAGE=out\fatjar-stage"
if exist "%STAGE%" rd /s /q "%STAGE%"
mkdir "%STAGE%"

echo [release] Extracting jSerialComm-2.11.4.jar...
pushd "%STAGE%"
"%JAR_EXE%" xf "..\..\%JSC_JAR%"
popd

rem Drop jSerialComm's META-INF entirely; jar cfe below writes a fresh
rem MANIFEST.MF that declares app.Main as the Main-Class. Leaving
rem jSerialComm's original MANIFEST / signature files would either
rem overwrite ours or invalidate the repackaged jar at load time.
if exist "%STAGE%\META-INF" rd /s /q "%STAGE%\META-INF"

echo [release] Copying compiled classes from out\ ...
for /d %%D in (out\*) do (
    set "_NAME=%%~nxD"
    if /I not "!_NAME!"=="fatjar-stage" if /I not "!_NAME!"=="test" (
        xcopy /s /e /y /q /i "%%D" "%STAGE%\!_NAME!" > nul
    )
)

set "OUT_JAR=%RELEASE_DIR%\MemoryInspector.jar"
echo [release] Packaging %OUT_JAR% ...
"%JAR_EXE%" cfe "%OUT_JAR%" app.Main -C "%STAGE%" .
if errorlevel 1 (
    echo [release] Fat-jar packaging FAILED.
    set "EXIT_CODE=1"
    goto :end
)

rd /s /q "%STAGE%"

for %%F in ("%OUT_JAR%") do set "OUT_KB=%%~zF"
set /a "OUT_KB=%OUT_KB% / 1024"
echo [release] Done. Fat jar: %OUT_JAR% (%OUT_KB% KB)
goto :end


:resolve_jar
set "JAR_EXE="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jar.exe" (
        set "JAR_EXE=%JAVA_HOME%\bin\jar.exe"
        goto :resolve_jar_done
    )
)
for /f "tokens=1,* delims==" %%a in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do (
    set "_JH=%%b"
)
if defined _JH (
    if "!_JH:~0,1!"==" " set "_JH=!_JH:~1!"
    if exist "!_JH!\bin\jar.exe" set "JAR_EXE=!_JH!\bin\jar.exe"
)
:resolve_jar_done
goto :eof


:end
popd
endlocal & exit /b %EXIT_CODE%
