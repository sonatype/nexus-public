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

:pickwrapper
set WRAPPER_EXE=%DIRNAME%wrapper.exe
if exist "%WRAPPER_EXE%" goto pickconfig
echo Missing wrapper executable: %WRAPPER_EXE%
pause
goto end

:pickconfig
set WRAPPER_CONF=%~f1
if not "%WRAPPER_CONF%" == "" goto execute
set WRAPPER_CONF=%DIRNAME%..\..\jsw\conf\wrapper.conf
if exist "%WRAPPER_CONF%" goto execute
echo Missing wrapper config: %WRAPPER_CONF%
pause
goto end

:execute
"%WRAPPER_EXE%" -p "%WRAPPER_CONF%"
goto end

:end
endlocal

if not errorlevel 1 goto finish
pause

:finish
cmd /C exit /B %ERRORLEVEL%
