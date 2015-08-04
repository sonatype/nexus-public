@REM
@REM Sonatype Nexus (TM) Open Source Version
@REM Copyright (c) 2008-present Sonatype, Inc.
@REM All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
@REM
@REM This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
@REM which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
@REM
@REM Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
@REM of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
@REM Eclipse Foundation. All other trademarks are the property of their respective owners.
@REM

@if "%WRAPPER_DEBUG%" == "" @echo off

if "%OS%"=="Windows_NT" goto begin
echo Unsupported Windows version: %OS%
pause
goto :eof

:begin
setlocal enableextensions

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.\

set DIST_BITS=32
if "%PROCESSOR_ARCHITECTURE%" == "AMD64" goto amd64
if not "%ProgramW6432%" == "" set DIST_BITS=64
goto pickwrapper

:amd64
set DIST_BITS=64

:pickwrapper
set WRAPPER_EXE=%DIRNAME%jsw\windows-x86-%DIST_BITS%\wrapper.exe
if exist "%WRAPPER_EXE%" goto pickconfig
echo Missing wrapper executable: %WRAPPER_EXE%
pause
goto end

:pickconfig
set WRAPPER_CONF=%DIRNAME%\jsw\conf\wrapper.conf
if exist "%WRAPPER_CONF%" goto execute
echo Missing wrapper config: %WRAPPER_CONF%
pause
goto end

:execute
for /F %%v in ('echo %1^|findstr "^console$ ^start$ ^stop$ ^restart$ ^install$ ^uninstall"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { console : start : stop : restart : install : uninstall }
    pause
    goto end
) else (
    shift
)

call :%COMMAND%
if errorlevel 1 pause
goto end

:console
"%WRAPPER_EXE%" -c "%WRAPPER_CONF%"
goto :eof

:start
"%WRAPPER_EXE%" -t "%WRAPPER_CONF%"
goto :eof

:stop
"%WRAPPER_EXE%" -p "%WRAPPER_CONF%"
goto :eof

:install
"%WRAPPER_EXE%" -i "%WRAPPER_CONF%"
goto :eof

:uninstall
"%WRAPPER_EXE%" -r "%WRAPPER_CONF%"
goto :eof

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof

:end
endlocal

:finish
cmd /C exit /B %ERRORLEVEL%
