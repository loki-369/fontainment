@rem =========================================================================
@rem  Gradle startup script for Windows - Custom Fontainment Offline Runner
@rem =========================================================================

@echo off

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set GRADLE_EXE=
for /d %%d in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.14-all\*") do (
    if exist "%%d\gradle-8.14\bin\gradle.bat" (
        set GRADLE_EXE="%%d\gradle-8.14\bin\gradle.bat"
    )
)

if not defined GRADLE_EXE (
    for /d %%d in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.9-bin\*") do (
        if exist "%%d\gradle-8.9\bin\gradle.bat" (
            set GRADLE_EXE="%%d\gradle-8.9\bin\gradle.bat"
        )
    )
)

if not defined GRADLE_EXE (
    echo [Fontainment Build] Local cached Gradle distribution not found. Please install Android Studio or configure Gradle.
    exit /b 1
)

%GRADLE_EXE% %*
