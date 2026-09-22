# Levanta el BFF leyendo la configuracion de infra\apps\.env
#
# Uso:  cd ms-vidasalud-bff ;  .\run-local.ps1

$jdk17 = "C:\Program Files\Java\jdk-17"
if (Test-Path $jdk17) {
    $env:JAVA_HOME = $jdk17
    $env:PATH = "$jdk17\bin;$env:PATH"
}

# Carga la configuracion compartida si el repo esta dentro del workspace
# completo. Si clonaste solo este repositorio, define las variables a mano
# (ver README) y el script funciona igual.
$compartido = Join-Path $PSScriptRoot "..\cargar-env.ps1"
if (Test-Path $compartido) {
    . $compartido
} else {
    Write-Host "No encontre ..\cargar-env.ps1: uso las variables ya definidas en la sesion." -ForegroundColor Yellow
}


# El BFF no tiene base de datos: el perfil de BD no le aplica.
Remove-Item env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Tenant : $env:AZURE_TENANT_ID" -ForegroundColor Cyan
Write-Host "API app: $env:AZURE_API_CLIENT_ID" -ForegroundColor Cyan
Write-Host "Issuer : https://login.microsoftonline.com/$env:AZURE_TENANT_ID/v2.0" -ForegroundColor Cyan
Write-Host ""
Write-Host "Arrancando BFF en http://localhost:8080 ..." -ForegroundColor Green
Write-Host ""

mvn spring-boot:run
