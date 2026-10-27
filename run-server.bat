@echo off
cd /d "%~dp0"
call gradlew.bat :server:run
