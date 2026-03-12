# ─── CONFIG ────────────────────────────────────────────────
$TOKEN = "Bearer ey......."     # Customer JWT token
$BASE_URL = "http://localhost:8080"
$BODY = @"
{
  "shippingAddress": {
    "fullName":       "Manish Singh",
    "phoneNumber":    "8999999999",
    "streetAddress":  "123 Test Lane",
    "city":           "Bangalore",
    "state":          "Karnataka",
    "zipCode":        "560001",
    "country":        "India"
  }
}
"@
# ───────────────────────────────────────────────────────────

$headers = @{
  "Authorization" = $TOKEN
  "Content-Type"  = "application/json"
}

Write-Host "Firing 2 concurrent checkout requests..."

# Fire both requests as background jobs (true parallel)
$job1 = Start-Job -ScriptBlock {
  Invoke-RestMethod -Uri "$using:BASE_URL/api/v1/orders/checkout" `
    -Method POST -Headers $using:headers -Body $using:BODY
}

$job2 = Start-Job -ScriptBlock {
  Invoke-RestMethod -Uri "$using:BASE_URL/api/v1/orders/checkout" `
    -Method POST -Headers $using:headers -Body $using:BODY
}

# Wait for both and print results
$r1 = Receive-Job -Job $job1 -Wait
$r2 = Receive-Job -Job $job2 -Wait

Write-Host "`n✅ Response 1:" ($r1 | ConvertTo-Json -Depth 5)
Write-Host "`n✅ Response 2:" ($r2 | ConvertTo-Json -Depth 5)

# Cleanup
Remove-Job $job1, $job2
