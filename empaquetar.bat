@echo off
setlocal

if not exist target\consumption-app.jar (
    echo ERROR: no existe target\consumption-app.jar.
    echo Haz clean y package en el panel Maven de IntelliJ y vuelve a ejecutar.
    exit /b 1
)

echo === 1/2 Creando el ejecutable ===
if exist target\entrada rmdir /s /q target\entrada
if exist target\dist rmdir /s /q target\dist
mkdir target\entrada
copy /y target\consumption-app.jar target\entrada\ >nul

jpackage --type app-image ^
  --name ConsumptionTier ^
  --app-version 1.0 ^
  --input target\entrada ^
  --main-jar consumption-app.jar ^
  --main-class consumption.Launcher ^
  --dest target\dist
if errorlevel 1 (
    echo ERROR: fallo jpackage. Comprueba que usas el JDK 21.
    exit /b 1
)

echo === 2/2 Comprimiendo ===
powershell -NoProfile -Command "Compress-Archive -Path 'target\dist\ConsumptionTier' -DestinationPath 'target\ConsumptionTier-win.zip' -Force"

echo.
echo Listo: target\dist\ConsumptionTier\ConsumptionTier.exe
echo Zip:   target\ConsumptionTier-win.zip
endlocal