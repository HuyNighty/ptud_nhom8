@echo off
chcp 65001 >nul
rem Bien dich bang javac (JDK 17+), khong can Maven. Ket qua: out\classes
cd /d "%~dp0.."
if exist out\classes rmdir /s /q out\classes
mkdir out\classes
dir /s /b src\main\java\*.java > out\sources.txt
javac -encoding UTF-8 --release 17 -d out\classes @out\sources.txt
if errorlevel 1 exit /b 1
echo Bien dich xong: out\classes
