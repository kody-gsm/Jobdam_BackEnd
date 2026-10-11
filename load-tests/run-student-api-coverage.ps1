$ErrorActionPreference = "Stop"

$baseUrl = "https://jobdom-fromt-git-test-vercel-preview-uen1.vercel.app/backend"
$accountCountText = Read-Host "Number of student test accounts"
$accountCount = 0
if (-not [int]::TryParse($accountCountText, [ref]$accountCount) -or $accountCount -lt 1) {
    throw "Enter a positive account count."
}

$testDate = Read-Host "Future school date for reservation tests (YYYY-MM-DD)"
$parsedDate = [DateTime]::MinValue
if (-not [DateTime]::TryParseExact($testDate, "yyyy-MM-dd", [Globalization.CultureInfo]::InvariantCulture, [Globalization.DateTimeStyles]::None, [ref]$parsedDate)) {
    throw "Enter a date in YYYY-MM-DD format."
}

$accessTokens = @()
$refreshTokens = @()
for ($i = 1; $i -le $accountCount; $i++) {
    Write-Host "Test account $i of $accountCount"
    $email = Read-Host "Account email"
    $securePassword = Read-Host "Account password" -AsSecureString
    $passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = $null
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
        $body = @{ email = $email; password = $plainPassword } | ConvertTo-Json -Compress
        $login = Invoke-RestMethod -Method Post -Uri ($baseUrl + "/auth/login") -ContentType "application/json" -Body $body
        if (-not $login.accessToken -or -not $login.refreshToken) {
            throw "Login response did not contain both tokens."
        }
        $accessTokens += $login.accessToken
        $refreshTokens += $login.refreshToken
    }
    finally {
        $plainPassword = $null
        $body = $null
        if ($passwordPtr) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
        }
        $securePassword.Dispose()
    }
}

try {
    $env:BASE_URL = $baseUrl
    $env:STUDENT_TOKENS = ConvertTo-Json -InputObject $accessTokens -Compress
    $env:REFRESH_TOKENS = ConvertTo-Json -InputObject $refreshTokens -Compress
    $env:TEST_DATE = $testDate
    Write-Host "All logins succeeded. Starting one functional API pass per account."
    & k6 run (Join-Path $PSScriptRoot "student-api-coverage.js")
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "k6 exited with code $LASTEXITCODE. Review failed checks and HTTP responses."
    }
}
finally {
    $env:BASE_URL = $null
    $env:STUDENT_TOKENS = $null
    $env:REFRESH_TOKENS = $null
    $env:TEST_DATE = $null
    $accessTokens = $null
    $refreshTokens = $null
}
