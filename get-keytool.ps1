# PowerShell script to find and setup keytool path
# Usage: .\get-keytool.ps1

Write-Host "🔍 Searching for keytool..." -ForegroundColor Cyan

# Common keytool locations
$possiblePaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\jdk\bin\keytool.exe",
    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
    "C:\Program Files\Java\jdk-17\bin\keytool.exe",
    "C:\Program Files\Java\jdk-11\bin\keytool.exe",
    "$env:JAVA_HOME\bin\keytool.exe"
)

# Find keytool
$foundPath = $null
foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $foundPath = $path
        break
    }
}

# If not found in common locations, search more broadly
if (-not $foundPath) {
    Write-Host "Searching in Program Files..." -ForegroundColor Yellow

    $searchPaths = @(
        "C:\Program Files\Android\Android Studio",
        "C:\Program Files\Java",
        "$env:LOCALAPPDATA\Android"
    )

    foreach ($searchPath in $searchPaths) {
        if (Test-Path $searchPath) {
            $found = Get-ChildItem -Path $searchPath -Filter "keytool.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                $foundPath = $found.FullName
                break
            }
        }
    }
}

if ($foundPath) {
    Write-Host "✓ Found keytool at: $foundPath" -ForegroundColor Green

    # Get the directory path
    $binPath = Split-Path -Parent $foundPath

    # Check if already in PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -like "*$binPath*") {
        Write-Host "✓ Path is already in your PATH environment variable" -ForegroundColor Green
    } else {
        Write-Host "`n📝 This path is NOT in your PATH environment variable." -ForegroundColor Yellow
        Write-Host "`nOptions to add it:" -ForegroundColor Cyan
        Write-Host "1. Temporarily (current session only):" -ForegroundColor White
        Write-Host "   `$env:Path += `";$binPath`"" -ForegroundColor Gray
        Write-Host "`n2. Permanently (requires restart):" -ForegroundColor White
        Write-Host "   [Environment]::SetEnvironmentVariable('Path', `$env:Path + ';$binPath', 'User')" -ForegroundColor Gray
        Write-Host "`n3. Manual (GUI method):" -ForegroundColor White
        Write-Host "   - Press Win+R, type 'sysdm.cpl', press Enter" -ForegroundColor Gray
        Write-Host "   - Go to 'Advanced' tab → 'Environment Variables'" -ForegroundColor Gray
        Write-Host "   - Edit 'Path' and add: $binPath" -ForegroundColor Gray

        $response = Read-Host "`nWould you like to add it temporarily now? (y/n)"
        if ($response -eq 'y' -or $response -eq 'Y') {
            $env:Path += ";$binPath"
            Write-Host "✓ Added to PATH for this session!" -ForegroundColor Green
        }
    }

    Write-Host "`n🔧 Test it by running:" -ForegroundColor Cyan
    Write-Host "keytool -version" -ForegroundColor Gray

    Write-Host "`n📋 Example commands:" -ForegroundColor Cyan
    Write-Host "# Check debug keystore:" -ForegroundColor Gray
    Write-Host "keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android" -ForegroundColor Gray

} else {
    Write-Host "✗ Could not find keytool.exe" -ForegroundColor Red
    Write-Host "`nPlease ensure Java JDK or Android Studio is installed." -ForegroundColor Yellow
    Write-Host "Visit: https://developer.android.com/studio" -ForegroundColor Cyan
    exit 1
}
