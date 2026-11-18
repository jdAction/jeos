<#
.SYNOPSIS
    Publishes io.github.jdaction:jeos to Maven Central through the Central Portal.

.DESCRIPTION
    Requires, outside of this script:
      * a Central Portal account owning the io.github.jdaction namespace
      * a <server><id>central</id> entry in %USERPROFILE%\.m2\settings.xml with the portal token
      * a GPG signing key whose public part is uploaded to keys.openpgp.org
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = '',
    [switch]$SkipChecks
)

if (-not $RepoRoot) { $RepoRoot = Split-Path -Parent $PSScriptRoot }
$ErrorActionPreference = 'Stop'
$settings = Join-Path $env:USERPROFILE '.m2\settings.xml'

if (-not $SkipChecks) {
    if (-not (Test-Path -LiteralPath $settings)) {
        throw "missing $settings - add a <server><id>central</id> entry with the portal token first"
    }
    if (-not (Select-String -LiteralPath $settings -Pattern '<id>\s*central\s*</id>' -Quiet)) {
        throw "no <server><id>central</id> in $settings"
    }
    $gpg = Get-Command gpg -ErrorAction SilentlyContinue
    if ($null -eq $gpg) {
        $candidate = 'C:\Program Files\Git\usr\bin\gpg.exe'
        if (Test-Path -LiteralPath $candidate) { $gpg = $candidate } else { throw 'gpg not found' }
    }
    $keys = & $gpg --list-secret-keys 2>&1
    if (-not $keys) { throw 'no GPG secret key available for signing' }
}

Push-Location -LiteralPath $RepoRoot
try {
    & mvn -B -P release deploy
    if ($LASTEXITCODE -ne 0) { throw 'mvn deploy failed' }
    Write-Host '==> upload finished; open https://central.sonatype.com/publishing to publish the deployment'
} finally {
    Pop-Location
}