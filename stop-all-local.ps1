# Stops every background job started by start-all-local.ps1.
# Run from the repo root: .\stop-all-local.ps1

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
    $job = Get-Job -Name $service -ErrorAction SilentlyContinue
    if ($job) {
        Write-Host "Stopping $service..."
        Stop-Job -Name $service
        Remove-Job -Name $service
    }
}

Write-Host "Done. (mvn's own child java.exe process can occasionally linger - check Task Manager for a stray java.exe on ports 8080-8085/8761/8888 if a restart won't bind.)"
