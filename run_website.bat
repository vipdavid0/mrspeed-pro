@echo off
echo Searching for PHP installation...
set PHP_BIN=php
if exist "C:\Program Files\PHP\8.2.34\ts\x64\php.exe" set PHP_BIN=C:\Program Files\PHP\8.2.34\ts\x64\php.exe
if exist "C:\xampp\php\php.exe" set PHP_BIN=C:\xampp\php\php.exe
if exist "C:\laragon\bin\php\php-8.3.0\php.exe" set PHP_BIN=C:\laragon\bin\php\php-8.3.0\php.exe
if exist "C:\php\php.exe" set PHP_BIN=C:\php\php.exe

echo Starting Mr Speed Test local PHP web server at http://localhost:8080
echo Press Ctrl+C to stop the server.
"%PHP_BIN%" -S localhost:8080 -t web
pause
