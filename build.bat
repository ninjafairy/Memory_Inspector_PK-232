@echo off
rem Memory Inspector - dev build (Java 17+, plain javac, no Maven/Gradle).
rem Produces a thin jar at MemoryInspector.jar; jSerialComm stays on classpath.
rem Fat/uber jar is deferred to M8 (see truenorth.md Sec. 5.10).

setlocal EnableDelayedExpansion
pushd "%~dp0"
set "EXIT_CODE=0"

set "JSC_JAR=lib\jSerialComm-2.11.4.jar"
if not exist "%JSC_JAR%" (
    echo [build] ERROR: %JSC_JAR% not found. See truenorth.md H1.
    set "EXIT_CODE=1"
    goto :end
)

if not exist out  mkdir out
if not exist Logs mkdir Logs

call :resolve_jar
if "%JAR_EXE%"=="" (
    echo [build] ERROR: Could not locate jar.exe.
    echo         Set JAVA_HOME to your JDK root ^(e.g. C:\Program Files\Java\jdk-23^)
    echo         or add the JDK's \bin to PATH.
    set "EXIT_CODE=1"
    goto :end
)

dir /s /b src\*.java > out\sources.txt 2>nul
if errorlevel 1 (
    echo [build] No Java sources under src\ yet. Nothing to compile.
    if exist out\sources.txt del /q out\sources.txt
    goto :end
)

echo [build] Compiling...
javac -encoding UTF-8 -d out -cp "%JSC_JAR%" @out\sources.txt
if errorlevel 1 (
    echo [build] javac FAILED.
    set "EXIT_CODE=1"
    goto :end
)

echo [build] Packaging MemoryInspector.jar ^(thin jar^)...
"%JAR_EXE%" cfe MemoryInspector.jar app.Main -C out .
if errorlevel 1 (
    echo [build] jar packaging FAILED.
    set "EXIT_CODE=1"
    goto :end
)

echo [build] Done. Output: MemoryInspector.jar
goto :end


:resolve_jar
rem Prefer explicit JAVA_HOME; else probe the running JDK via java -XshowSettings.
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
    rem Strip a leading space if present (the output is "    java.home = <path>").
    if "!_JH:~0,1!"==" " set "_JH=!_JH:~1!"
    if exist "!_JH!\bin\jar.exe" set "JAR_EXE=!_JH!\bin\jar.exe"
)
:resolve_jar_done
goto :eof


:end
popd
endlocal & exit /b %EXIT_CODE%
