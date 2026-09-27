# Load .env variables into the current PowerShell session.
# Run with:
#   . .\load-env.ps1

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "No .env file found at $envFile"
    return
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

    # Skip blank lines and comments
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2

    if ($parts.Length -eq 2) {
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()

        Set-Item -Path "Env:$key" -Value $value

        Write-Host "Set $key"
    }
}

Write-Host ""
Write-Host "Environment loaded from .env. Now run, e.g.:"
Write-Host "  cd auth-service; mvn spring-boot:run"