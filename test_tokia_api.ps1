# Script de teste da API Tokia
# Execute: .\test_tokia_api.ps1

param(
    [string]$ApiKey = ""
)

if ($ApiKey -eq "") {
    $propsFile = "C:\Users\Cliente\Documents\Teste26\BlocoDeNotas-Trabalho\local.properties"
    if (Test-Path $propsFile) {
        $content = Get-Content $propsFile -Raw
        $match = [regex]::Match($content, 'TOOKIA_API_KEY=(.+)')
        if ($match.Success) {
            $ApiKey = $match.Groups[1].Value.Trim()
        }
    }
}

if ($ApiKey -eq "" -or $ApiKey -eq "sua_chave_aqui") {
    Write-Host "ERRO: Configure sua API Key em local.properties" -ForegroundColor Red
    Write-Host "  Local: C:\Users\Cliente\Documents\Teste26\BlocoDeNotas-Trabalho\local.properties" -ForegroundColor Yellow
    Write-Host "  Formato: TOOKIA_API_KEY=sua_chave_aqui" -ForegroundColor Yellow
    exit 1
}

Write-Host "Testando API Tokia..." -ForegroundColor Cyan
Write-Host "Base URL: https://api.usetokia.com/v1" -ForegroundColor Gray

$body = @{
    model = "gpt-3.5-turbo"
    messages = @(
        @{ role = "user"; content = "Responda apenas: OK" }
    )
    max_tokens = 10
} | ConvertTo-Json -Depth 3

$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod `
        -Uri "https://api.usetokia.com/v1/chat/completions" `
        -Method Post `
        -Headers $headers `
        -Body $body `
        -TimeoutSec 30

    $content = $response.choices[0].message.content
    Write-Host "SUCESSO! Resposta da API:" -ForegroundColor Green
    Write-Host "  $content" -ForegroundColor White
    Write-Host ""
    Write-Host "A API Tokia esta funcionando corretamente!" -ForegroundColor Green
} catch {
    Write-Host "ERRO na comunicacao com a API:" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Yellow
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "  Detalhes: $errorBody" -ForegroundColor Yellow
    }
}
