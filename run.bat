@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "JAVA_CMD="

rem 1) JAVA_HOME, se configurado.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

rem 2) Java disponivel no PATH.
if not defined JAVA_CMD (
  for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA_CMD set "JAVA_CMD=%%J"
)

rem 3) Locais comuns de instalacao. Nao altera o computador.
if not defined JAVA_CMD (
  for /d %%D in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\Zulu\*") do (
    if not defined JAVA_CMD if exist "%%~fD\bin\java.exe" set "JAVA_CMD=%%~fD\bin\java.exe"
  )
)
if not defined JAVA_CMD if defined ProgramFiles(x86) (
  for /d %%D in ("%ProgramFiles(x86)%\Java\*" "%ProgramFiles(x86)%\Eclipse Adoptium\*") do (
    if not defined JAVA_CMD if exist "%%~fD\bin\java.exe" set "JAVA_CMD=%%~fD\bin\java.exe"
  )
)
if not defined JAVA_CMD (
  for /d %%D in ("%LOCALAPPDATA%\Programs\Eclipse Adoptium\*" "%LOCALAPPDATA%\Programs\Java\*") do (
    if not defined JAVA_CMD if exist "%%~fD\bin\java.exe" set "JAVA_CMD=%%~fD\bin\java.exe"
  )
)

if not defined JAVA_CMD (
  echo ================================================================
  echo  Elevar GED v0.1.3 - Java nao encontrado
  echo ================================================================
  echo O Elevar GED procurou o Java no JAVA_HOME, PATH e pastas comuns.
  echo Nenhuma instalacao foi localizada para este usuario.
  echo.
  echo O Elevar GED nao instala nada e nao requer privilegios de administrador.
  echo ================================================================
  pause
  exit /b 1
)

echo ================================================================
echo  Elevar GED v0.1.3 - Java Engine
echo ================================================================
echo Java localizado em:
echo %JAVA_CMD%
echo.
"%JAVA_CMD%" -version
if errorlevel 1 (
  echo [ERRO] O Java foi localizado, mas nao conseguiu iniciar.
  pause
  exit /b 1
)

echo.
echo Iniciando Elevar GED...
"%JAVA_CMD%" -Dfile.encoding=UTF-8 -Dged.base="%CD%" -jar elevar-ged.jar
if errorlevel 1 (
  echo.
  echo [ERRO] O motor Java foi encerrado com falha.
  pause
)
