@echo off
chcp 65001 >nul
rem Kiem thu tu dong tren CSDL THAY THE Apache Derby (khong phai SQL Server). Can lib\test\derby-10.14.2.0.jar
cd /d "%~dp0.."
if exist out\test rmdir /s /q out\test
mkdir out\test
dir /s /b src\main\java\*.java src\test\java\*.java > out\test-sources.txt
javac -encoding UTF-8 --release 17 -cp "lib\test\*" -d out\test @out\test-sources.txt
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out\test;lib\test\*" vn.n08.nhakhoa.KiemThuTichHop
if "%1"=="--giao-dien" java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out\test;lib\test\*" vn.n08.nhakhoa.KiemThuGiaoDien out\anh-giao-dien
