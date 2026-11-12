@echo off
setlocal

set NOMBRE=ConsumptionTier
set VERSION=0.2
set MODULOS=java.desktop,java.logging,java.net.http,java.xml,jdk.unsupported

set CONSOLA=
if /i "%~1"=="debug" (
    set CONSOLA=--win-console
    echo Modo debug: la app se abrira con una consola.
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: no se encuentra jpackage.
    echo Usa el JDK 21 y agrega su carpeta bin al PATH, o abre este script desde el terminal de IntelliJ.
    exit /b 1
)

if not exist target\consumption-app.jar (
    echo ERROR: no existe target\consumption-app.jar.
    echo Haz clean y package en el panel Maven de IntelliJ y vuelve a ejecutar.
    exit /b 1
)

echo === 1/3 Creando el ejecutable ===
if exist target\entrada rmdir /s /q target\entrada
if exist target\dist rmdir /s /q target\dist
mkdir target\entrada
copy /y target\consumption-app.jar target\entrada\ >nul

jpackage --type app-image ^
  --name %NOMBRE% ^
  --app-version %VERSION% ^
  --vendor "Nicolas Valencia" ^
  --description "Biblioteca personal de juegos, libros, peliculas y series" ^
  --input target\entrada ^
  --main-jar consumption-app.jar ^
  --main-class consumption.Launcher ^
  --add-modules %MODULOS% ^
  %CONSOLA% ^
  --dest target\dist
if errorlevel 1 (
    echo ERROR: fallo jpackage. Comprueba que usas el JDK 21.
    exit /b 1
)

echo === 2/3 Agregando el LEEME ===
if exist portable\LEEME.txt copy /y portable\LEEME.txt "target\dist\%NOMBRE%\LEEME.txt" >nul

echo === 3/3 Comprimiendo ===
powershell -NoProfile -Command "Compress-Archive -Path 'target\dist\%NOMBRE%' -DestinationPath 'target\%NOMBRE%-%VERSION%-win.zip' -Force"
if errorlevel 1 (
    echo ERROR: no se pudo crear el zip.
    exit /b 1
)

echo.
echo Listo: target\dist\%NOMBRE%\%NOMBRE%.exe
echo Zip:   target\%NOMBRE%-%VERSION%-win.zip
endlocal