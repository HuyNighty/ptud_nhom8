@echo off
chcp 65001 >nul
rem Chay ung dung. Can: out\classes (scripts\build.bat), lib\mssql-jdbc-*.jar, config\db.properties
cd /d "%~dp0.."
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out\classes;lib\*" vn.n08.nhakhoa.App %*
