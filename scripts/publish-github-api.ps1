<#
.SYNOPSIS
    Publishes the local commit to GitHub through the REST API (works when github.com:443 is
    unreachable but api.github.com is).

.DESCRIPTION
    The local history is a squashed snapshot, so the commit is re-created on top of the fork's
    current master: every file of the local HEAD becomes a blob, one tree is assembled from those
    blobs, a commit is created with the fork master as its parent and the master ref is moved
    forward (a fast-forward, history is preserved). An annotated tag is created as well.

.PARAMETER Token
    A GitHub token with contents:write on jdAction/jeos. Defaults to $env:GH_TOKEN and then to
    the first line of %TEMP%\gh-token.txt.

.PARAMETER DryRun
    Builds the payload, prints what would be pushed and touches nothing.
#>
[CmdletBinding()]
param(
    [string]$Token,
    [string]$Owner = 'jdAction',
    [string]$Repo = 'jeos',
    [string]$Branch = 'master',
    [string]$Tag = 'v0.9.17',
    [string]$RepoRoot = '',
    [switch]$DryRun
)

if (-not $RepoRoot) { $RepoRoot = Split-Path -Parent $PSScriptRoot }
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

if (-not $Token) { $Token = $env:GH_TOKEN }
if (-not $Token -and (Test-Path -LiteralPath "$env:TEMP\gh-token.txt")) {
    $Token = (Get-Content -LiteralPath "$env:TEMP\gh-token.txt" -TotalCount 1).Trim()
}
if (-not $Token -and -not $DryRun) { throw 'no token: pass -Token, set $env:GH_TOKEN or write %TEMP%\gh-token.txt' }

$headers = @{
    'Accept'               = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent'           = 'jeos-release'
}
if ($Token) { $headers['Authorization'] = "Bearer $Token" }

$api = "https://api.github.com/repos/$Owner/$Repo"

function Invoke-GitHub([string]$Method, [string]$Uri, $Body) {
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers; TimeoutSec = 60 }
    if ($null -ne $Body) { $params['Body'] = ($Body | ConvertTo-Json -Depth 20 -Compress); $params['ContentType'] = 'application/json; charset=utf-8' }
    if ($DryRun -and $Method -ne 'GET') {
        Write-Host "  [dry-run] $Method $Uri"
        return $null
    }
    Invoke-RestMethod @params
}

Write-Host "==> repository $Owner/$Repo"
$repoInfo = Invoke-GitHub GET $api $null
Write-Host "    default branch: $($repoInfo.default_branch), pushed_at: $($repoInfo.pushed_at)"
if ($repoInfo.default_branch -ne $Branch) { throw "default branch is $($repoInfo.default_branch), not $Branch" }

$refUri = "$api/git/ref/heads/$Branch"
$base = Invoke-GitHub GET $refUri $null
$baseSha = $base.object.sha
Write-Host "==> base commit $baseSha"

Push-Location -LiteralPath $RepoRoot
try {
    $entries = & git ls-tree -r HEAD | ForEach-Object {
        if ($_ -match '^(\d{6})\s+\w+\s+([0-9a-f]{40})\t(.+)$') {
            [pscustomobject]@{ Mode = $Matches[1]; Sha = $Matches[2]; Path = $Matches[3] }
        }
    }
    Write-Host "==> $($entries.Count) files in HEAD"

    $treeEntries = New-Object System.Collections.ArrayList
    $i = 0
    foreach ($entry in $entries) {
        $i++
        $full = Join-Path $RepoRoot $entry.Path
        $bytes = [System.IO.File]::ReadAllBytes($full)
        $b64 = [Convert]::ToBase64String($bytes)
        if ($DryRun) {
            [void]$treeEntries.Add([pscustomobject]@{ path = $entry.Path; mode = $entry.Mode; type = 'blob'; sha = "dry-$i" })
            continue
        }
        $blob = Invoke-GitHub POST "$api/git/blobs" @{ content = $b64; encoding = 'base64' }
        [void]$treeEntries.Add([pscustomobject]@{ path = $entry.Path; mode = $entry.Mode; type = 'blob'; sha = $blob.sha })
        if ($i % 25 -eq 0) { Write-Host "    blobs $i/$($entries.Count)" }
    }

    $tree = Invoke-GitHub POST "$api/git/trees" @{ tree = $treeEntries.ToArray() }
    $treeSha = if ($tree) { $tree.sha } else { 'dry-tree' }
    Write-Host "==> tree $treeSha"

    $authorName = (& git log -1 --format=%an).Trim()
    $authorEmail = (& git log -1 --format=%ae).Trim()
    $authorDate = (& git log -1 --format=%aI).Trim()
    $message = (& git log -1 --format=%B) -join "`n"
    if (-not $message.Trim()) { $message = "jeos 0.9.17: fix upstream issues and prepare Central publication" }
    $identity = @{ name = $authorName; email = $authorEmail; date = $authorDate }

    $commit = Invoke-GitHub POST "$api/git/commits" @{
        message = $message
        tree    = $treeSha
        parents = @($baseSha)
        author  = $identity
    }
    $commitSha = if ($commit) { $commit.sha } else { 'dry-commit' }
    Write-Host "==> commit $commitSha"

    Invoke-GitHub PATCH "$api/git/refs/heads/$Branch" @{ sha = $commitSha; force = $false } | Out-Null
    Write-Host "==> $Branch now points at $commitSha"

    if ($Tag) {
        $tagObject = Invoke-GitHub POST "$api/git/tags" @{
            tag     = $Tag
            message = "jeos $Tag"
            object  = $commitSha
            type    = 'commit'
            tagger  = $identity
        }
        if ($tagObject) {
            Invoke-GitHub POST "$api/git/refs" @{ ref = "refs/tags/$Tag"; sha = $tagObject.sha } | Out-Null
            Write-Host "==> tag $Tag created ($($tagObject.sha))"
        }
    }

    if (-not $DryRun) {
        $check = Invoke-GitHub GET "$api/git/ref/heads/$Branch" $null
        Write-Host "==> verified $Branch at $($check.object.sha)"
    }
} finally {
    Pop-Location
}