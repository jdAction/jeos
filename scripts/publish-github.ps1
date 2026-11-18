<#
.SYNOPSIS
    Pushes the local repository to the GitHub fork jdAction/jeos.

.DESCRIPTION
    https://github.com is not reachable over HTTPS from this network (TCP 443 times out), so the
    push goes through SSH. The machine's SSH key has to be registered on the GitHub account first;
    this script verifies that before touching the remote.
#>
[CmdletBinding()]
param(
    [string]$Remote = 'github',
    [string]$Url = 'git@github.com:jdAction/jeos.git',
    [string]$Branch = 'master',
    [string]$RepoRoot = ''
)

if (-not $RepoRoot) { $RepoRoot = Split-Path -Parent $PSScriptRoot }
$ErrorActionPreference = 'Stop'

Write-Host "==> checking SSH authentication to github.com"
$auth = & ssh -T -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 git@github.com 2>&1
if ($LASTEXITCODE -ne 1 -and $auth -notmatch 'successfully authenticated') {
    throw "SSH authentication failed: $auth"
}
Write-Host "    $auth"

Push-Location -LiteralPath $RepoRoot
try {
    $exists = (& git remote) -contains $Remote
    if ($exists) {
        & git remote set-url $Remote $Url
    } else {
        & git remote add $Remote $Url
    }
    if ($LASTEXITCODE -ne 0) { throw 'cannot configure the remote' }

    Write-Host "==> pushing $Branch and tags to $Url"
    & git push $Remote $Branch --tags
    if ($LASTEXITCODE -ne 0) { throw 'git push failed' }
} finally {
    Pop-Location
}