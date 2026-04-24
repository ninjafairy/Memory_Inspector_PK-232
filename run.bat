@echo off
rem Memory Inspector - dev launcher.
rem Uses javaw (no console window) per truenorth.md H3.
rem Diagnostics route through PacketLogger (Logs\memory_inspector.log).

setlocal
pushd "%~dp0"
set "EXIT_CODE=0"

set "JSC_JAR=lib\jSerialComm-2.11.4.jar"
if not exist "%JSC_JAR%" (
    echo [run] ERROR: %JSC_JAR% not found.
    set "EXIT_CODE=1"
    goto :end
)

if not exist Logs mkdir Logs

rem Prefer the packaged jar when present; fall back to out\ classes for fast iteration.
if exist MemoryInspector.jar (
    start "" javaw -cp "MemoryInspector.jar;%JSC_JAR%" app.Main %*
    goto :end
)
if exist out\app\Main.class (
    start "" javaw -cp "out;%JSC_JAR%" app.Main %*
    goto :end
)

echo [run] No compiled output found. Run build.bat first.
set "EXIT_CODE=1"

:end
popd
endlocal & exit /b %EXIT_CODE%
