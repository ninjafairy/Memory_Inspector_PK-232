@echo off
rem Memory Inspector - test harness.
rem Compiles test\*.java against out\ (main classes) and runs the JUnit 5
rem console launcher (truenorth.md H2). Test-only; never shipped.

setlocal
pushd "%~dp0"
set "EXIT_CODE=0"

set "JUNIT_JAR="
for %%f in (lib\junit-platform-console-standalone-*.jar) do set "JUNIT_JAR=%%f"

if "%JUNIT_JAR%"=="" (
    echo [test] JUnit 5 console launcher jar not found.
    echo        Expected:  lib\junit-platform-console-standalone-^<ver^>.jar
    echo        Download:  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/
    set "EXIT_CODE=1"
    goto :end
)

if not exist out (
    echo [test] No main-classes output at out\. Run build.bat first.
    set "EXIT_CODE=1"
    goto :end
)

if not exist out\test mkdir out\test

dir /s /b test\*.java > out\test-sources.txt 2>nul
if errorlevel 1 (
    echo [test] No test sources under test\ yet.
    if exist out\test-sources.txt del /q out\test-sources.txt
    goto :end
)

echo [test] Using %JUNIT_JAR%
echo [test] Compiling tests...
javac -encoding UTF-8 -d out\test -cp "out;%JUNIT_JAR%" @out\test-sources.txt
if errorlevel 1 (
    echo [test] Test compilation FAILED.
    set "EXIT_CODE=1"
    goto :end
)

echo [test] Running JUnit 5 console launcher...
java -jar "%JUNIT_JAR%" execute --class-path "out;out\test" --scan-class-path
set "EXIT_CODE=%errorlevel%"

:end
popd
endlocal & exit /b %EXIT_CODE%
