# Starts every service in the background with plain `mvn spring-boot:run`,
# no Docker involved — PowerShell equivalent of start-all-local.sh.
#
# Run from the repo root, after loading your .env:
#   . .\load-env.ps1
#   .\start-all-local.ps1
#
# Logs go to .\logs\<service>.log. Each service runs as a background job;
# stop-all-local.ps1 stops them by job name.

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

New-Item -ItemType Directory -Force -Path "logs" | Out-Null

# Order matters: discovery-server first (everything else registers with
# it), then auth-service (everyone else depends on it for users/ELO),
# then the rest, then the gateway last since it fronts everyone else.
$services = @(
    "discovery-server",
    "config-server",
    "auth-service",
    "game-engine-service",
    "matchmaking-service",
    "chat-service",
    "notification-service",
    "api-gateway"
)

foreach ($service in $services) {
    Write-Host "Starting $service..."

    $logFile = Join-Path $PSScriptRoot "logs\$service.log"

    Start-Job -Name $service -ScriptBlock {
        param($serviceDir, $log)
        Set-Location $serviceDir
        mvn -q spring-boot:run *> $log
    } -ArgumentList (Join-Path $PSScriptRoot $service), $logFile | Out-Null

    Start-Sleep -Seconds 3  # give each service a moment before the next starts registering
}

Write-Host ""
Write-Host "All services launching as background jobs."
Write-Host "Check status with:  Get-Job"
Write-Host "Tail a log with:    Get-Content logs\auth-service.log -Wait -Tail 20"
Write-Host "Discovery dashboard: http://localhost:8761"
Write-Host "Gateway:             http://localhost:8080"
Write-Host "Stop everything with: .\stop-all-local.ps1"
