$ErrorActionPreference = "Stop"

$baseUrl = "https://jobdom-fromt-git-test-vercel-preview-uen1.vercel.app/backend"
$email = Read-Host "Test account email"
$securePassword = Read-Host "Test account password" -AsSecureString
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = $null

try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
    $body = @{ email = $email; password = $plainPassword } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Method Post -Uri ($baseUrl + "/auth/login") -ContentType "application/json" -Body $body

    if (-not $login.accessToken) {
        throw "Login response did not contain an access token."
    }

    $env:STUDENT_TOKEN = $login.accessToken
    Write-Host "Login succeeded. Starting the read-only load test."
    & k6 run (Join-Path $PSScriptRoot "ramp-student-reads.js")

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "k6 exited with code $LASTEXITCODE. Review the failed checks."
    }
}
finally {
    $env:STUDENT_TOKEN = $null
    $plainPassword = $null
    if ($passwordPtr) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    }
    $securePassword.Dispose()
}
