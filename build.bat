@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo [ERRO] O compilador javac nao foi encontrado. Para usar o Elevar GED, voce NAO precisa compilar.
  echo Use apenas run.bat. build.bat so e necessario para desenvolvimento do codigo Java.
  pause
  exit /b 1
)
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
javac --release 8 -encoding UTF-8 -cp "lib\*" -d build\classes src\ged\*.java
if errorlevel 1 goto :fail
> build\MANIFEST.MF echo Main-Class: ged.GedServer
>> build\MANIFEST.MF echo Class-Path: lib/pdfbox-1.8.16.jar lib/fontbox-1.8.16.jar lib/commons-logging-1.3.0.jar
>> build\MANIFEST.MF echo.
jar cfm elevar-ged.jar build\MANIFEST.MF -C build\classes .
if errorlevel 1 goto :fail
echo Elevar GED Java compilado com sucesso.
pause
exit /b 0
:fail
echo Falha na compilacao.
pause
exit /b 1
