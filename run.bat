@echo off
echo =====================
echo Starting Ardor Server
echo =====================
echo.
if exist jdk (
    echo Using product Open JDK
    set javaCmd=jdk\bin\java.exe
    goto startJava
)

echo using workstation default JRE
set javaCmd=java.exe
echo. 
echo JRE location
where java.exe
echo.
java -version
echo.
	
:startJava	
    echo Server will start in a new command window. 
	start "Ardor Server %~dp0" "%javaCmd%" -cp classes;lib\*;conf;addons\classes;addons\lib\*;javafx-sdk\lib\* -Dnxt.runtime.mode=desktop nxt.Nxt