@echo off
if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%
set PROGNAME=%~nx0%

SET KARAF_TITLE=Sonatype Nexus

goto BEGIN

:USAGE
    echo "%PROGNAME% { console | start | stop | restart | status }"
goto :EOF

:BEGIN

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:RUN
    SET SHIFT=false
    if "%1" == "console" goto :EXECUTE_CONSOLE
    if "%1" == "start" goto :EXECUTE_START
    if "%1" == "stop" goto :EXECUTE_STOP
    if "%1" == "restart" goto :EXECUTE_RESTART
    if "%1" == "status" goto :EXECUTE_STATUS
    goto :USAGE

:EXECUTE_CONSOLE
    shift
    "%DIRNAME%karaf.bat" %1 %2 %3 %4 %5 %6 %7 %8
    goto :EOF

:EXECUTE_START
    shift
    "%DIRNAME%start.bat" %1 %2 %3 %4 %5 %6 %7 %8
    goto :EOF

:EXECUTE_STOP
    shift
    "%DIRNAME%stop.bat" %1 %2 %3 %4 %5 %6 %7 %8
    goto :EOF

:EXECUTE_RESTART
    shift
    call "%DIRNAME%stop.bat" 2>NUL
    "%DIRNAME%start.bat" %1 %2 %3 %4 %5 %6 %7 %8
    goto :EOF

:EXECUTE_STATUS
    shift
    "%DIRNAME%status.bat" %1 %2 %3 %4 %5 %6 %7 %8
    goto :EOF

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:END

endlocal

if not "%PAUSE%" == "" pause

:END_NO_PAUSE

