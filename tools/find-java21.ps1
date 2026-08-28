$ErrorActionPreference = 'SilentlyContinue'

$candidates = New-Object System.Collections.Generic.List[string]

function Add-Candidate {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        try {
            $resolved = (Resolve-Path -LiteralPath $Path).Path
            if (-not $candidates.Contains($resolved)) {
                $candidates.Add($resolved)
            }
        } catch {
        }
    }
}

function Add-JavaHomeCandidate {
    param([string]$Home)

    if ([string]::IsNullOrWhiteSpace($Home)) {
        return
    }

    Add-Candidate (Join-Path $Home 'bin\java.exe')
}

function Get-JavaMajor {
    param([string]$JavaExe)

    try {
        $banner = ((& $JavaExe -version 2>&1) | Out-String)
        if ($banner -match 'version\s+"(?:1\.)?(\d+)(?:[\.\-+_"]|$)') {
            return [int]$Matches[1]
        }
    } catch {
    }

    try {
        $properties = ((& $JavaExe -XshowSettings:properties -version 2>&1) | Out-String)
        if ($properties -match 'java\.specification\.version\s*=\s*(\d+)') {
            return [int]$Matches[1]
        }
    } catch {
    }

    return $null
}

Add-JavaHomeCandidate $env:PICK_RELAY_JAVA_HOME

Add-JavaHomeCandidate $env:JAVA_HOME

try {
    foreach ($command in @(Get-Command java.exe -All -ErrorAction SilentlyContinue)) {
        Add-Candidate $command.Source
    }
} catch {
}

$roots = @(
    (Join-Path $env:APPDATA 'PrismLauncher\java'),
    (Join-Path $env:LOCALAPPDATA 'PrismLauncher\java'),
    (Join-Path $env:LOCALAPPDATA 'Programs\PrismLauncher\java'),
    (Join-Path $env:ProgramFiles 'PrismLauncher\java'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Microsoft'),
    (Join-Path $env:ProgramFiles 'Zulu'),
    (Join-Path $env:ProgramFiles 'BellSoft'),
    (Join-Path $env:ProgramFiles 'Semeru')
)

foreach ($root in $roots) {
    if ([string]::IsNullOrWhiteSpace($root) -or -not (Test-Path -LiteralPath $root)) {
        continue
    }

    try {
        foreach ($file in @(Get-ChildItem -LiteralPath $root -Filter java.exe -File -Recurse -ErrorAction SilentlyContinue)) {
            if ($file.FullName -match '\\bin\\java\.exe$') {
                Add-Candidate $file.FullName
            }
        }
    } catch {
    }
}

foreach ($candidate in $candidates) {
    if ((Get-JavaMajor $candidate) -eq 21) {
        Write-Output $candidate
        exit 0
    }
}

exit 21
