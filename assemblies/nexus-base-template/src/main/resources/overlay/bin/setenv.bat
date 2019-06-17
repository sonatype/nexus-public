@echo off

rem
rem handle specific scripts; the SCRIPT_NAME is exactly the name of the Karaf
rem script; for example karaf.bat, start.bat, stop.bat, admin.bat, client.bat, ...
rem
rem if "%KARAF_SCRIPT%" == "SCRIPT_NAME" (
rem   Actions go here...
rem )

rem
rem general settings which should be applied for all scripts go here; please keep
rem in mind that it is possible that scripts might be executed more than once, e.g.
rem in example of the start script where the start script is executed first and the
rem karaf script afterwards.
rem

rem
rem The following section shows the possible configuration options for the default 
rem karaf scripts
rem
rem Window name of the windows console
rem SET KARAF_TITLE
rem Location of Java installation
rem SET JAVA_HOME
rem Minimum Java heap memory for the JVM
rem SET JAVA_MIN_MEM
rem Maximum Java heap memory for the JVM
rem SET JAVA_MAX_MEM
rem Maximum direct buffer memory for the JVM
rem SET DIRECT_MAX_MEM
rem Additional JVM options
rem SET EXTRA_JAVA_OPTS 
rem Karaf home folder
rem SET KARAF_HOME
rem Karaf data folder
rem SET KARAF_DATA
rem Karaf base folder
rem SET KARAF_BASE
rem Karaf etc folder
rem SET KARAF_ETC
rem First citizen Karaf options
rem SET KARAF_SYSTEM_OPTS
rem Additional available Karaf options
rem SET KARAF_OPTS
rem Enable debug mode
rem SET KARAF_DEBUG
