@echo off
setlocal

set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set GRADLE_HOME=

for /d %%D in ("%GRADLE_USER_HOME%\wrapper\dists\gradle-9.7.0-all\*") do (
    if exist "%%~fD\gradle-9.7.0\bin\gradle.bat" (
        set GRADLE_HOME=%%~fD\gradle-9.7.0
        goto found
    )
)

:found
if not defined GRADLE_HOME (
    echo gradle-9.7.0 not found in %GRADLE_USER_HOME%\wrapper\dists
    exit /b 1
)

"%GRADLE_HOME%\bin\gradle.bat" %*
