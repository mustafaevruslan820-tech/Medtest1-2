# Runs both icon scripts (install + welcome). See setup-all-icons.ps1
$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "setup-all-icons.ps1")
