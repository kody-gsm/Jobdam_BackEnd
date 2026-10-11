$ErrorActionPreference = "Stop"

$baseUrl = "https://jobdam.kro.kr/backend"
$email = Read-Host "Student test account email"
$securePassword = Read-Host "Student test account password" -AsSecureString
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = $null

try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
    $body = @{ email = $email; password = $plainPassword } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri ($baseUrl + "/auth/login") -ContentType "application/json" -Body $body
    if (-not $login.accessToken) {
        throw "Login response did not contain an access token."
    }

    $env:BASE_URL = $baseUrl
    $env:STUDENT_TOKEN = $login.accessToken
    $env:TEST_DATE = Get-Date -Format "yyyy-MM-dd"
    Write-Host "Login succeeded. Starting read-only load test: 10 to 100 virtual users."
    & k6 run (Join-Path $PSScriptRoot "ramp-student-reads-100.js")
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "k6 exited with code $LASTEXITCODE. Review failed checks and response times."
    }
}
finally {
    $env:BASE_URL = $null
    $env:STUDENT_TOKEN = $null
    $env:TEST_DATE = $null
    $login = $null
    $body = $null
    $plainPassword = $null
    if ($passwordPtr) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    }
    $securePassword.Dispose()
}
