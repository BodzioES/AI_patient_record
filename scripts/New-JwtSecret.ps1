<#
.SYNOPSIS
    Generuje losowy sekret JWT (domyślnie 32 bajty = 256 bitów) w Base64.

.EXAMPLE
    .\scripts\New-JwtSecret.ps1
    Wypisuje nowy sekret.

.EXAMPLE
    .\scripts\New-JwtSecret.ps1 -UpdateEnv
    Wpisuje nowy sekret do JWT_SECRET w pliku .env (bez wypisywania go na ekran).
#>
param(
    [ValidateRange(32, 128)]
    [int]$Bytes = 32,
    [switch]$UpdateEnv,
    [string]$EnvPath = (Join-Path $PSScriptRoot '..\.env')
)

$buffer = New-Object byte[] $Bytes
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($buffer) } finally { $rng.Dispose() }
$secret = [Convert]::ToBase64String($buffer)

if (-not $UpdateEnv) {
    $secret
    return
}

if (-not (Test-Path $EnvPath)) {
    throw "Nie znaleziono pliku $EnvPath (skopiuj najpierw .env.example do .env)."
}
$lines = Get-Content -Path $EnvPath -Encoding UTF8
if (-not ($lines -match '^JWT_SECRET=')) {
    $lines += "JWT_SECRET=$secret"
} else {
    $lines = $lines | ForEach-Object { if ($_ -match '^JWT_SECRET=') { "JWT_SECRET=$secret" } else { $_ } }
}
# UTF-8 bez BOM, żeby docker compose poprawnie odczytał plik.
[System.IO.File]::WriteAllLines((Resolve-Path $EnvPath), [string[]]$lines, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "Zapisano nowy JWT_SECRET ($Bytes bajtów) w $EnvPath. Zrestartuj backend; zalogowani użytkownicy muszą zalogować się ponownie."
