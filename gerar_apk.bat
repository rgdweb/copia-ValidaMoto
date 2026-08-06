@echo off
chcp 65001 > nul
title Gerador de APK - ValidMoto

echo =======================================================
echo          Gerador Automático de APK - ValidMoto
echo =======================================================
echo.

rem 1. Verificar JDK
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO] O Java (JDK 17+) nao foi encontrado no seu sistema.
    echo Por favor, instale o Android Studio ou o JDK 17+ para compilar.
    echo Download JDK: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

rem 2. Localizar o Android SDK no Windows
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    ) else if exist "C:\Android\Sdk" (
        set "ANDROID_HOME=C:\Android\Sdk"
    )
)

if "%ANDROID_HOME%"=="" (
    echo [AVISO] Nao foi possivel localizar automaticamente o Android SDK.
    echo.
    set /p "ANDROID_HOME=Digite o caminho da pasta do seu Android SDK (ex: C:\Users\SeuNome\AppData\Local\Android\Sdk): "
)

if not exist "%ANDROID_HOME%" (
    echo [ERRO] Caminho do Android SDK invalido ou nao encontrado: %ANDROID_HOME%
    echo Certifique-se de ter o Android Studio instalado com o SDK.
    pause
    exit /b 1
)

rem 3. Gerar local.properties se nao existir
if not exist "local.properties" (
    set "SDK_PATH=%ANDROID_HOME:\=\\%"
    echo sdk.dir=%ANDROID_HOME:\=\\%> local.properties
    echo [OK] Arquivo local.properties criado com sucesso.
)

rem 4. Executar compilacao do APK
echo.
echo [INFO] Compilando APK do ValidMoto... Aguarde...
echo.

if exist "gradlew.bat" (
    call gradlew.bat assembleDebug
) else (
    where gradle >nul 2>&1
    if %errorlevel% eq 0 (
        call gradle assembleDebug
    ) else (
        echo [ERRO] Nem gradlew.bat nem o comando 'gradle' foram encontrados.
        echo Abra a pasta no Android Studio para gerar o wrapper ou compilar.
        pause
        exit /b 1
    )
)

if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao do APK. Verifique as mensagens de erro acima.
    pause
    exit /b 1
)

echo.
echo =======================================================
echo [SUCESSO] APK gerado com sucesso!
echo =======================================================
echo.

set "APK_FILE=app\build\outputs\apk\debug\app-debug.apk"

if exist "%APK_FILE%" (
    echo APK localizado em:
    echo %cd%\%APK_FILE%
    echo.
    echo Abrindo a pasta do APK no Windows Explorer...
    explorer /select,"%cd%\%APK_FILE%"
) else (
    echo O arquivo APK deve estar na pasta: app\build\outputs\apk\debug\
)

echo.
pause
