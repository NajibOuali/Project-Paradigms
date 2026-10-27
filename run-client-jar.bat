@echo off
cd /d "%~dp0"
if not exist "jars\TCPClient.jar" (
    echo Building TCPClient.jar...
    call gradlew.bat clientJar
)
start javaw -jar "jars\TCPClient.jar"
