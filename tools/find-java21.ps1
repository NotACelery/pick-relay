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

    # Prefer the plain `java -version` banner. It is stable across Temurin,
    # Oracle, Microsoft and launcher-managed runtimes and avoids depending on
    # the formatting of -XshowSettings under Windows PowerShell 5.1.
    try {
        $banner = ((& $JavaExe -version 2>&1) | Out-String)
        if ($banner -match 'version\s+"(?:1\.)?(\d+)(?:[\.\-+_"]|$)') {
            return [int]$Matches[1]
        }
    } catch {
    }

    # Fallback for unusual JVM banners.
    try {
        $properties = ((& $JavaExe -XshowSettings:properties -version 2>&1) | Out-String)
        if ($properties -match 'java\.specification\.version\s*=\s*(\d+)') {
            return [int]$Matches[1]
        }
    } catch {
    }

    return $null
}

# Explicit project override wins when it points to a valid Java 21 home.
Add-JavaHomeCandidate $env:PICK_RELAY_JAVA_HOME

# Then inspect the user's normal Java configuration.
Add-JavaHomeCandidate $env:JAVA_HOME

try {
    foreach ($command in @(Get-Command java.exe -All -ErrorAction SilentlyContinue)) {
        Add-Candidate $command.Source
    }
} catch {
}

# Minecraft launchers and common Windows JDK vendors. We deliberately search
# these even when PATH already contains another Java version (for example JDK 25).
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
