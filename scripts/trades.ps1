[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [Parameter()]
    [ValidateSet('seed', 'list', 'get', 'update-status', 'delete')]
    [string]$Action = 'seed',

    [Parameter()]
    [string]$BaseUrl = 'http://localhost:8081',

    [Parameter()]
    [int]$Count = 5,

    [Parameter()]
    [long]$Id,

    [Parameter()]
    [ValidateSet('NEW', 'CANCELLED', 'EXECUTED')]
    [string]$Status,

    [Parameter()]
    [string]$Symbol,

    [Parameter()]
    [int]$Page = 0,

    [Parameter()]
    [int]$Size = 10,

    [Parameter()]
    [string]$SampleFile = (Join-Path $PSScriptRoot 'trade-samples.json'),

    [Parameter()]
    [switch]$Raw
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-ApiUrl {
    param(
        [Parameter(Mandatory)]
        [string]$Path,

        [Parameter()]
        [hashtable]$Query = @{}
    )

    $base = $BaseUrl.TrimEnd('/')
    $url = "$base$Path"

    $queryParts = @()
    foreach ($key in $Query.Keys) {
        $value = $Query[$key]
        if ($null -ne $value -and "$value" -ne '') {
            $queryParts += ('{0}={1}' -f [uri]::EscapeDataString($key), [uri]::EscapeDataString("$value"))
        }
    }

    if ($queryParts.Count -gt 0) {
        $url = '{0}?{1}' -f $url, ($queryParts -join '&')
    }

    return $url
}

function Write-ApiError {
    param([Parameter(Mandatory)]$Exception)

    $message = $Exception.Exception.Message
    $response = $Exception.Exception.Response

    if ($response -and $response.GetResponseStream) {
        $stream = $response.GetResponseStream()
        if ($stream) {
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
            if ($body) {
                Write-Error "$message`n$body"
                return
            }
        }
    }

    Write-Error $message
}

function Invoke-Api {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('Get', 'Post', 'Patch', 'Delete')]
        [string]$Method,

        [Parameter(Mandatory)]
        [string]$Url,

        [Parameter()]
        $Body
    )

    try {
        if ($PSBoundParameters.ContainsKey('Body')) {
            $jsonBody = $Body | ConvertTo-Json -Depth 10
            return Invoke-RestMethod -Method $Method -Uri $Url -ContentType 'application/json' -Body $jsonBody
        }

        return Invoke-RestMethod -Method $Method -Uri $Url
    }
    catch {
        Write-ApiError $_
        throw
    }
}

function Get-SampleTrades {
    if (Test-Path $SampleFile) {
        $content = Get-Content $SampleFile -Raw
        if ($content.Trim()) {
            return @($content | ConvertFrom-Json)
        }
    }

    return @()
}

function New-RandomTrade {
    $symbols = 'AAPL', 'MSFT', 'NVDA', 'TSLA', 'AMZN', 'META', 'GOOGL', 'JPM', 'SPY', 'QQQ'
    $sides = 'BUY', 'SELL'

    return [pscustomobject]@{
        symbol = Get-Random -InputObject $symbols
        side = Get-Random -InputObject $sides
        quantity = Get-Random -Minimum 10 -Maximum 500
        price = [decimal](Get-Random -Minimum 25 -Maximum 750)
    }
}

function Format-TradeTable {
    param([Parameter(Mandatory)]$Trades)

    $Trades |
        Select-Object id, symbol, side, quantity, price, status, createdAt, updatedAt |
        Format-Table -AutoSize |
        Out-String |
        Write-Host
}

switch ($Action) {
    'seed' {
        $sampleTrades = Get-SampleTrades
        $payloads = New-Object System.Collections.Generic.List[object]

        foreach ($sample in $sampleTrades) {
            if ($payloads.Count -ge $Count) {
                break
            }

            $payloads.Add([pscustomobject]@{
                symbol = "$($sample.symbol)"
                side = "$($sample.side)"
                quantity = [int]$sample.quantity
                price = [decimal]$sample.price
            })
        }

        while ($payloads.Count -lt $Count) {
            $payloads.Add((New-RandomTrade))
        }

        $createdTrades = foreach ($payload in $payloads) {
            Invoke-Api -Method Post -Url (Get-ApiUrl -Path '/api/trades') -Body $payload
        }

        Write-Host "Created $($createdTrades.Count) trade(s)." -ForegroundColor Green
        if ($Raw) {
            $createdTrades | ConvertTo-Json -Depth 10
        }
        else {
            Format-TradeTable -Trades $createdTrades
        }
    }

    'list' {
        $query = @{
            page = $Page
            size = $Size
        }

        if ($PSBoundParameters.ContainsKey('Status')) {
            $query.status = $Status
        }

        if ($PSBoundParameters.ContainsKey('Symbol')) {
            $query.symbol = $Symbol
        }

        $response = Invoke-Api -Method Get -Url (Get-ApiUrl -Path '/api/trades' -Query $query)

        if ($Raw) {
            $response | ConvertTo-Json -Depth 10
        }
        else {
            Write-Host ("Page {0} | Size {1} | Total Elements {2}" -f $response.number, $response.size, $response.totalElements) -ForegroundColor Cyan
            Format-TradeTable -Trades $response.content
        }
    }

    'get' {
        if (-not $PSBoundParameters.ContainsKey('Id')) {
            throw 'The -Id parameter is required for the get action.'
        }

        $trade = Invoke-Api -Method Get -Url (Get-ApiUrl -Path "/api/trades/$Id")
        $trade | ConvertTo-Json -Depth 10
    }

    'update-status' {
        if (-not $PSBoundParameters.ContainsKey('Id')) {
            throw 'The -Id parameter is required for the update-status action.'
        }

        if (-not $PSBoundParameters.ContainsKey('Status')) {
            throw 'The -Status parameter is required for the update-status action.'
        }

        $updatedTrade = Invoke-Api -Method Patch -Url (Get-ApiUrl -Path "/api/trades/$Id/status") -Body @{ status = $Status }

        Write-Host "Updated trade $Id to status $Status." -ForegroundColor Green
        $updatedTrade | ConvertTo-Json -Depth 10
    }

    'delete' {
        if (-not $PSBoundParameters.ContainsKey('Id')) {
            throw 'The -Id parameter is required for the delete action.'
        }

        if ($PSCmdlet.ShouldProcess("trade $Id", 'Cancel trade')) {
            Invoke-Api -Method Delete -Url (Get-ApiUrl -Path "/api/trades/$Id") | Out-Null
            Write-Host "Cancelled trade $Id." -ForegroundColor Yellow
        }
    }
}


