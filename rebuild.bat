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

@REM keep track of what folder we're currently in so we can return there later
set here=%cd%

@REM Backup nexus.properties
copy private\assemblies\nexus-pro\target\sonatype-work\nexus3\etc\nexus.properties .

@REM Detect docker
if not defined DOCKER_HOST (set docker="-Dno-docker=true")

@REM allow user to specify custom thread count on the command line
if "%~1"=="-T" goto :customThreads

call mvn clean install -DskipTests -T 4 %docker%
goto :continue

:customThreads
call mvn clean install -DskipTests -T %~2 %docker%

:continue
@REM Exit if maven did not build successfully
IF %ERRORLEVEL% NEQ 0 (
   EXIT /B %ERRORLEVEL%
)

@REM Enable Debug mode
set KARAF_DEBUG=true

@REM Set NEXUS_RESOURCE_DIRS for UI development
for /f "usebackq tokens=*" %%a in (`powershell "(Get-ChildItem -Recurse |  Where-Object { $_.FullName -match '((src\\main\\resources\\static)|(src\\test\\ft-resources))$' } | ForEach-Object { $_.parent.FullName }) -join ','"`) do set NEXUS_RESOURCE_DIRS=%%a

@REM Restore nexus.properties
mkdir private\assemblies\nexus-pro\target\sonatype-work\nexus3\etc\
move nexus.properties private\assemblies\nexus-pro\target\sonatype-work\nexus3\etc\nexus.properties

cd private\assemblies\nexus-pro\target

@REM Prefer 7-zip if present for perf
if exist "C:\Program Files\7-Zip\7z.exe" (
    "C:\Program Files\7-Zip\7z.exe" x "nexus-professional-*-SNAPSHOT-bundle.zip" -aoa
) else (
    REM Requires PowerShell 5.1 - https://docs.microsoft.com/en-us/powershell/scripting/setup/installing-windows-powershell?view=powershell-5.1#upgrading-existing-windows-powershell
    powershell "Expand-Archive nexus-professional-*-SNAPSHOT-bundle.zip ."
)

cd nexus*\bin

nexus /run

cd %here%
