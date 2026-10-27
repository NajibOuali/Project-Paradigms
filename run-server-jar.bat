@echo off
cd /d "%~dp0"
if not exist "jars\TCPServer.jar" (
    echo Building TCPServer.jar...
    call gradlew.bat serverJar
)
start javaw -jar "jars\TCPServer.jar"
