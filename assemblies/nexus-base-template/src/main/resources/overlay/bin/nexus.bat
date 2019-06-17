@echo off

if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%
set PROGNAME=%~nx0%
set ARGS=%*

rem Sourcing environment settings for karaf similar to tomcats setenv
SET KARAF_SCRIPT="karaf.bat"
if exist "%DIRNAME%setenv.bat" (
  call "%DIRNAME%setenv.bat"
)

rem Check console window title. Set to Karaf by default
if not "%KARAF_TITLE%" == "" (
    title %KARAF_TITLE%
) else (
    title Karaf
)

rem Check/Set up some easily accessible MIN/MAX params for JVM mem usage
if "%JAVA_MIN_MEM%" == "" (
    set JAVA_MIN_MEM=1200M
)
if "%JAVA_MAX_MEM%" == "" (
    set JAVA_MAX_MEM=1200M
)
if "%DIRECT_MAX_MEM%" == "" (
    set DIRECT_MAX_MEM=2G
)

goto BEGIN

:warn
    echo %PROGNAME%: %*
goto :EOF

:BEGIN

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

if not "%KARAF_HOME%" == "" (
    call :warn Ignoring predefined value for KARAF_HOME
)
set KARAF_HOME=%DIRNAME%..
if not exist "%KARAF_HOME%" (
    call :warn KARAF_HOME is not valid: "%KARAF_HOME%"
    goto END
)

if not "%KARAF_BASE%" == "" (
    if not exist "%KARAF_BASE%" (
       call :warn KARAF_BASE is not valid: "%KARAF_BASE%"
       goto END
    )
)
if "%KARAF_BASE%" == "" (
  set "KARAF_BASE=%KARAF_HOME%"
)

if not "%KARAF_DATA%" == "" (
    if not exist "%KARAF_DATA%" (
        call :warn KARAF_DATA is not valid: "%KARAF_DATA%"
        goto END
    )
)
if "%KARAF_DATA%" == "" (
    set "KARAF_DATA=%KARAF_BASE%\..\sonatype-work\nexus3"
)

if not "%KARAF_ETC%" == "" (
    if not exist "%KARAF_ETC%" (
        call :warn KARAF_ETC is not valid: "%KARAF_ETC%"
        goto END
    )
)
if "%KARAF_ETC%" == "" (
    set "KARAF_ETC=%KARAF_BASE%\etc\karaf"
)

if not "%KARAF_LOG%" == "" (
    if not exist "%KARAF_LOG%" (
        call :warn KARAF_LOG is not valid: "%KARAF_LOG%"
        goto END
    )
)
if "%KARAF_LOG%" == "" (
    set "KARAF_LOG=%KARAF_DATA%\log"
)

set LOCAL_CLASSPATH=%CLASSPATH%

set CLASSPATH=%LOCAL_CLASSPATH%;%KARAF_BASE%\conf
set DEFAULT_JAVA_DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
set DEFAULT_JAVA_DEBUGS_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005

if "%LOCAL_CLASSPATH%" == "" goto :KARAF_CLASSPATH_EMPTY
    set CLASSPATH=%LOCAL_CLASSPATH%;%KARAF_BASE%\conf
    goto :KARAF_CLASSPATH_END
:KARAF_CLASSPATH_EMPTY
    set CLASSPATH=%KARAF_BASE%\conf
:KARAF_CLASSPATH_END

set CLASSPATH_INITIAL=%CLASSPATH%

rem Setup Karaf Home
if exist "%KARAF_HOME%\conf\karaf-rc.cmd" call %KARAF_HOME%\conf\karaf-rc.cmd
if exist "%HOME%\karaf-rc.cmd" call %HOME%\karaf-rc.cmd

rem Support for loading native libraries
set PATH=%PATH%;%KARAF_BASE%\lib;%KARAF_HOME%\lib

rem Setup the Java Virtual Machine
if not "%JAVA%" == "" goto :Check_JAVA_END
    if not "%JAVA_HOME%" == "" goto :TryJDKEnd
        call :warn JAVA_HOME not set; results may vary
:TryJRE
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment"
    if not exist __reg1.txt goto :TryJDK
    type __reg1.txt | find "CurrentVersion" > __reg2.txt
    if errorlevel 1 goto :TryJDK
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JavaTemp=%%~x
    if errorlevel 1 goto :TryJDK
    set JavaTemp=%JavaTemp%##
    set JavaTemp=%JavaTemp:                ##=##%
    set JavaTemp=%JavaTemp:        ##=##%
    set JavaTemp=%JavaTemp:    ##=##%
    set JavaTemp=%JavaTemp:  ##=##%
    set JavaTemp=%JavaTemp: ##=##%
    set JavaTemp=%JavaTemp:##=%
    del __reg1.txt
    del __reg2.txt
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\%JavaTemp%"
    if not exist __reg1.txt goto :TryJDK
    type __reg1.txt | find "JavaHome" > __reg2.txt
    if errorlevel 1 goto :TryJDK
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JAVA_HOME=%%~x
    if errorlevel 1 goto :TryJDK
    del __reg1.txt
    del __reg2.txt
    goto TryJDKEnd
:TryJDK
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit"
    if not exist __reg1.txt (
        goto TryRegJRE
    )
    type __reg1.txt | find "CurrentVersion" > __reg2.txt
    if errorlevel 1 (
        goto TryRegJRE
    )
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JavaTemp=%%~x
    if errorlevel 1 (
        goto TryRegJRE
    )
    set JavaTemp=%JavaTemp%##
    set JavaTemp=%JavaTemp:                ##=##%
    set JavaTemp=%JavaTemp:        ##=##%
    set JavaTemp=%JavaTemp:    ##=##%
    set JavaTemp=%JavaTemp:  ##=##%
    set JavaTemp=%JavaTemp: ##=##%
    set JavaTemp=%JavaTemp:##=%
    del __reg1.txt
    del __reg2.txt
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\%JavaTemp%"
    if not exist __reg1.txt (
        goto TryRegJRE
    )
    type __reg1.txt | find "JavaHome" > __reg2.txt
    if errorlevel 1 (
        goto TryRegJRE
    )
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JAVA_HOME=%%~x
    if errorlevel 1 (
        goto TryRegJRE
    )
    del __reg1.txt
    del __reg2.txt
:TryRegJRE
    rem try getting the JAVA_HOME from registry
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment" /v CurrentVersion`) DO (
       set JAVA_VERSION=%%A
    )
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment\%JAVA_VERSION%" /v JavaHome`) DO (
       set JAVA_HOME=%%A %%B
    )
    if not exist "%JAVA_HOME%" (
       goto TryRegJDK
	)
	goto TryJDKEnd
:TryRegJDK
    rem try getting the JAVA_HOME from registry
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Development Kit" /v CurrentVersion`) DO (
       set JAVA_VERSION=%%A
    )
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Development Kit\%JAVA_VERSION%" /v JavaHome`) DO (
       set JAVA_HOME=%%A %%B
    )
    if not exist "%JAVA_HOME%" (
       call :warn Unable to retrieve JAVA_HOME from Registry
    )
	goto TryJDKEnd
:TryJDKEnd
    if not exist "%JAVA_HOME%" (
        call :warn JAVA_HOME is not valid: "%JAVA_HOME%"
        goto END
    )
    set JAVA=%JAVA_HOME%\bin\java
:Check_JAVA_END

rem Retrieve java version
for /f tokens^=2-5^ delims^=.-_+^" %%j in ('"%JAVA%" -fullversion 2^>^&1') do (
    if %%j==1 (set JAVA_VERSION=%%k) else (set JAVA_VERSION=%%j)
)

if not exist "%JAVA_HOME%\bin\server\jvm.dll" (
    if not exist "%JAVA_HOME%\jre\bin\server\jvm.dll" (
        echo WARNING: Running Karaf on a Java HotSpot Client VM because server-mode is not available.
        echo Install Java Developer Kit to fix this.
        echo For more details see http://java.sun.com/products/hotspot/whitepaper.html#client
    )
)
set DEFAULT_JAVA_OPTS=-Xms%JAVA_MIN_MEM% -Xmx%JAVA_MAX_MEM% -XX:MaxDirectMemorySize=%DIRECT_MAX_MEM% -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput -XX:LogFile=%KARAF_LOG%\jvm.log -XX:-OmitStackTraceInFastThrow -Djava.net.preferIPv4Stack=true

if "%JAVA_OPTS%" == "" set JAVA_OPTS=%DEFAULT_JAVA_OPTS%

if "%EXTRA_JAVA_OPTS%" == "" goto :KARAF_EXTRA_JAVA_OPTS_END
    set JAVA_OPTS=%JAVA_OPTS% %EXTRA_JAVA_OPTS%
:KARAF_EXTRA_JAVA_OPTS_END

if "%KARAF_DEBUG%" == "" goto :KARAF_DEBUG_END
    if "%1" == "stop" goto :KARAF_DEBUG_END
    if "%1" == "client" goto :KARAF_DEBUG_END
    if "%1" == "status" goto :KARAF_DEBUG_END
    rem Use the defaults if JAVA_DEBUG_OPTS was not set
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUG_OPTS%

    set JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%
    call :warn Enabling Java debug options: %JAVA_DEBUG_OPTS%
:KARAF_DEBUG_END

if "%KARAF_PROFILER%" == "" goto :KARAF_PROFILER_END
    set KARAF_PROFILER_SCRIPT=%KARAF_HOME%\conf\profiler\%KARAF_PROFILER%.cmd

    if exist "%KARAF_PROFILER_SCRIPT%" goto :KARAF_PROFILER_END
    call :warn Missing configuration for profiler '%KARAF_PROFILER%': %KARAF_PROFILER_SCRIPT%
    goto END
:KARAF_PROFILER_END

rem Setup the classpath
pushd "%KARAF_HOME%\lib\boot"
for %%G in (*.jar) do call:APPEND_TO_CLASSPATH %%G
popd
goto CLASSPATH_END

: APPEND_TO_CLASSPATH
set filename=%~1
set suffix=%filename:~-4%
if %suffix% equ .jar set CLASSPATH=%CLASSPATH%;%KARAF_HOME%\lib\boot\%filename%
goto :EOF

:CLASSPATH_END

rem Execute the JVM or the load the profiler
if "%KARAF_PROFILER%" == "" goto :RUN
    rem Execute the profiler if it has been configured
    call :warn Loading profiler script: %KARAF_PROFILER_SCRIPT%
    call %KARAF_PROFILER_SCRIPT%

:RUN
    SET OPTS=-Dkaraf.startLocalConsole=true -Dkaraf.startRemoteShell=true
    SET MAIN=org.sonatype.nexus.karaf.NexusMain
    SET SHIFT=false

:RUN_LOOP
    if "%1" == "stop" goto :EXECUTE_STOP
    if "%1" == "status" goto :EXECUTE_STATUS
    if "%1" == "run" goto :EXECUTE_CONSOLE
    if "%1" == "console" goto :EXECUTE_CONSOLE
    if "%1" == "start" goto :EXECUTE_SERVER
    if "%1" == "server" goto :EXECUTE_SERVER
    if "%1" == "daemon" goto :EXECUTE_DAEMON
    if "%1" == "client" goto :EXECUTE_CLIENT
    if "%1" == "clean" goto :EXECUTE_CLEAN
    if "%1" == "debug" goto :EXECUTE_DEBUG
    if "%1" == "debugs" goto :EXECUTE_DEBUGS
    goto :EXECUTE

:EXECUTE_STOP
    SET MAIN=org.apache.karaf.main.Stop
    shift
    goto :RUN_LOOP

:EXECUTE_STATUS
    SET MAIN=org.apache.karaf.main.Status
    shift
    goto :RUN_LOOP

:EXECUTE_CONSOLE
    shift
    goto :RUN_LOOP

:EXECUTE_SERVER
    SET OPTS=-Dkaraf.startLocalConsole=false -Dkaraf.startRemoteShell=true
    shift
    goto :RUN_LOOP

:EXECUTE_DAEMON
    SET OPTS=-Dkaraf.startLocalConsole=false -Dkaraf.startRemoteShell=true
    SET KARAF_DAEMON=true
    shift
    goto :RUN_LOOP

:EXECUTE_CLIENT
    SET OPTS=-Dkaraf.startLocalConsole=true -Dkaraf.startRemoteShell=false
    shift
    goto :RUN_LOOP

:EXECUTE_CLEAN
    pushd "%KARAF_DATA%" && (rmdir /S /Q "%KARAF_DATA%" 2>nul & popd)
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUG
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUG_OPTS%
    set JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUGS
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUGS_OPTS%
    set JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%
    shift
    goto :RUN_LOOP

:EXECUTE
    SET ARGS=%1 %2 %3 %4 %5 %6 %7 %8
    rem Execute the Java Virtual Machine
    cd "%KARAF_BASE%"

    rem When users want to update the lib version of, they just need to create
    rem a lib.next directory and on the new restart, it will replace the current lib directory.
    if exist "%KARAF_HOME%\lib.next" (
        echo Updating libs...
        RD /S /Q "%KARAF_HOME%\lib"
        MOVE /Y "%KARAF_HOME%\lib.next" "%KARAF_HOME%\lib"

        echo "Updating classpath..."
        set CLASSPATH=%CLASSPATH_INITIAL%
        pushd "%KARAF_HOME%\lib\boot"
        for %%G in (*.jar) do call:APPEND_TO_CLASSPATH %%G
        popd
    )

        rem If major version is greater than 1 (meaning Java 9 or 10), we don't use endorsed lib but module
        rem If major version is 1 (meaning Java 1.6, 1.7, 1.8), we use endorsed lib
        if %JAVA_VERSION% GTR 8 (
            "%JAVA%" %JAVA_OPTS% %OPTS% ^
                --add-reads=java.xml=java.logging ^
                --add-exports=java.base/org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED ^
                --patch-module=java.base=lib/endorsed/org.apache.karaf.specs.locator-4.2.6.jar ^
                --patch-module=java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-4.2.6.jar ^
                --add-opens=java.base/java.security=ALL-UNNAMED ^
                --add-opens=java.base/java.net=ALL-UNNAMED ^
                --add-opens=java.base/java.lang=ALL-UNNAMED ^
                --add-opens=java.base/java.util=ALL-UNNAMED ^
                --add-opens=java.naming/javax.naming.spi=ALL-UNNAMED ^
                --add-opens=java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED ^
                --add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED ^
                --add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED ^
                --add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED ^
                --add-exports=jdk.xml.dom/org.w3c.dom.html=ALL-UNNAMED ^
                --add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED ^
                -classpath "%CLASSPATH%" ^
                -Dkaraf.instances="%KARAF_HOME%\instances" ^
                -Dkaraf.home="%KARAF_HOME%" ^
                -Dkaraf.base="%KARAF_BASE%" ^
                -Dkaraf.etc="%KARAF_ETC%" ^
                -Dkaraf.log="%KARAF_LOG%" ^
                -Dkaraf.restart.jvm.supported=true ^
                -Djava.io.tmpdir="%KARAF_DATA%\tmp" ^
                -Dkaraf.data="%KARAF_DATA%" ^
                -Djava.util.logging.config.file="%KARAF_BASE%\etc\java.util.logging.properties" ^
                %KARAF_SYSTEM_OPTS% %KARAF_OPTS% %MAIN% %ARGS%
        ) else (
            "%JAVA%" %JAVA_OPTS% %OPTS% ^
                -classpath "%CLASSPATH%" ^
                -Djava.endorsed.dirs="%JAVA_HOME%\jre\lib\endorsed;%JAVA_HOME%\lib\endorsed;%KARAF_HOME%\lib\endorsed" ^
                -Dkaraf.instances="%KARAF_HOME%\instances" ^
                -Dkaraf.home="%KARAF_HOME%" ^
                -Dkaraf.base="%KARAF_BASE%" ^
                -Dkaraf.etc="%KARAF_ETC%" ^
                -Dkaraf.log="%KARAF_LOG%" ^
                -Dkaraf.restart.jvm.supported=true ^
                -Djava.io.tmpdir="%KARAF_DATA%\tmp" ^
                -Dkaraf.data="%KARAF_DATA%" ^
                -Djava.util.logging.config.file="%KARAF_BASE%\etc\java.util.logging.properties" ^
                %KARAF_SYSTEM_OPTS% %KARAF_OPTS% %MAIN% %ARGS%
        )

    rem If KARAF_DAEMON is defined, auto-restart is bypassed and control given
    rem back to the operating system
    if defined "%KARAF_DAEMON%" (
        rem If Karaf has been started by winsw, the process can be restarted
        rem by executing KARAF_DAEMON% restart!
        rem   https://github.com/kohsuke/winsw#restarting-service-from-itself
        if defined "%WINSW_EXECUTABLE%" (
            if ERRORLEVEL 10 (
                echo Restarting ...
                %KARAF_DAEMON% restart!
            )
        )
    ) else (
        if ERRORLEVEL 10 (
            echo Restarting JVM...
            goto EXECUTE
        )
    )


rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:END

endlocal

if not "%PAUSE%" == "" pause

:END_NO_PAUSE
    EXIT /B %ERRORLEVEL%
