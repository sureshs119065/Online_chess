# PowerShell equivalent of the bash ".env loading" trick from the README,
# which does nothing on Windows. Run this once per terminal session, from
# the repo root, before starting any service:
#
#   . .\load-env.ps1
#
# Note the leading ". " (dot-space) - that "dot-sources" the script so the
# environment variables it sets stick around in YOUR shell afterward.
# Running it as ".\load-env.ps1" (no dot) runs it in a child scope and the
# variables disappear the moment the script finishes, which looks like it
# silently did nothing.

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "No .env file found at $envFile - copy .env.example to .env and fill in real values first."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

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
