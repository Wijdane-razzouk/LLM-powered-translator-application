@echo off
setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"

REM Start backend
if exist "%ROOT%translator_arab\target\translator_arab.jar" (
  start "Translator API" cmd /k "cd /d ""%ROOT%translator_arab"" && java -jar target\translator_arab.jar"
) else (
  start "Translator API" cmd /k "cd /d ""%ROOT%translator_arab"" && mvn -q exec:java -Dexec.mainClass=org.translate.com.EmbeddedServer"
)

REM Start Whisper server (edit command to match your setup)
start "Whisper Server" cmd /k ""C:\Users\HP\whisper-bin\Release\whisper-server.exe" -m "C:\Users\HP\whisper-bin\models\ggml-base.bin" --port 9000"

echo.
echo Backend launch requested. Update Whisper command in start_all.bat if needed.
echo.
endlocal
