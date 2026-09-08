# GabesTV - Script de Testes Automatizados e Build
[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " GabesTV - Validacao de Inicializacao e Testes" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Configurar JDK 21
$jdkPath = "C:\Users\GP\.jdks\jbr-21.0.11"
if (Test-Path $jdkPath) {
    $env:JAVA_HOME = $jdkPath
    $env:PATH = "$jdkPath\bin;" + $env:PATH
    Write-Host "[1/4] JAVA_HOME configurado para: $jdkPath" -ForegroundColor Green
} else {
    Write-Host "[1/4] JDK 21 nao encontrado em $jdkPath. Usando: $env:JAVA_HOME" -ForegroundColor Yellow
}

# 2. Executar testes unitarios e testes reais de inicializacao (Robolectric)
Write-Host ""
Write-Host "[2/4] Executando suite de testes (Unitarios + Inicializacao da MainActivity)..." -ForegroundColor Yellow
./gradlew testDebugUnitTest --rerun-tasks
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "FALHA: Testes automatizados falharam!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "OK: Todos os testes unitarios e de inicializacao da Activity passaram!" -ForegroundColor Green

# 3. Compilar APK Debug
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "[3/4] Compilando APK Debug (assembleDebug)..." -ForegroundColor Yellow
    ./gradlew assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "FALHA: Compilacao do APK Debug falhou!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "OK: APK Debug compilado com sucesso!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[3/4] Etapa de compilacao ignorada (SkipBuild)." -ForegroundColor DarkGray
}

# 4. Validar artefato final
$apkPath = "app/build/outputs/apk/debug/app-debug.apk"
Write-Host ""
Write-Host "[4/4] Validando artefato final..." -ForegroundColor Yellow
if (Test-Path $apkPath) {
    $apkItem = Get-Item $apkPath
    $sizeMb = [math]::Round($apkItem.Length / 1MB, 2)
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " Relatorio de Validacao" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " Arquivo APK: $($apkItem.FullName)"
    Write-Host " Tamanho:     $sizeMb MB"
    Write-Host " Data/Hora:   $($apkItem.LastWriteTime)"
    Write-Host ""
    Write-Host " STATUS: SUCESSO - PRONTO PARA TESTE NO DISPOSITIVO" -ForegroundColor Green
    Write-Host " Comando para instalar no celular conectado via USB:" -ForegroundColor Cyan
    Write-Host "   adb install -r $apkPath"
    Write-Host ""
} else {
    Write-Host "Aviso: O arquivo APK nao foi encontrado em $apkPath." -ForegroundColor Yellow
}
