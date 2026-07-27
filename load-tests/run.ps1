[CmdletBinding()]
param(
    [ValidateRange(1, 10000)]
    [int]$Rate = 5,

    [ValidatePattern('^\d+(ms|s|m|h)$')]
    [string]$Duration = '1m',

    [ValidateRange(0, 2147483647)]
    [int]$Stock = 100000,

    [ValidateRange(1, 100)]
    [int]$OrderQuantity = 1,

    [ValidateSet('CONFIRMED', 'REJECTED', 'ANY')]
    [string]$ExpectedStatus = 'CONFIRMED',

    [ValidateRange(1, 10000)]
    [int]$PreAllocatedVUs = 20,

    [ValidateRange(1, 50000)]
    [int]$MaxVUs = 100
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$exitCode = 0

function Invoke-PostgresScalar {
    param(
        [Parameter(Mandatory)]
        [string]$Service,

        [Parameter(Mandatory)]
        [string]$Database,

        [Parameter(Mandatory)]
        [string]$Sql
    )

    $output = & docker compose exec -T $Service `
        psql -qAt -v ON_ERROR_STOP=1 -U postgres -d $Database -c $Sql
    $commandExitCode = $LASTEXITCODE
    if ($commandExitCode -ne 0) {
        throw "PostgreSQL command failed for $Service"
    }

    return ([string]($output | Select-Object -First 1)).Trim()
}

Push-Location $repositoryRoot
try {
    if ($PreAllocatedVUs -gt $MaxVUs) {
        throw 'PreAllocatedVUs cannot be greater than MaxVUs'
    }

    $inventorySql = @"
INSERT INTO inventory(food_name, available_quantity)
VALUES ('k6-load-test-item', $Stock)
ON CONFLICT (food_name)
DO UPDATE SET available_quantity = EXCLUDED.available_quantity
RETURNING id;
"@

    $foodId = Invoke-PostgresScalar `
        -Service 'inventory-db' `
        -Database 'inventorydb' `
        -Sql $inventorySql

    $baselineOrderId = Invoke-PostgresScalar `
        -Service 'order-db' `
        -Database 'orderdb' `
        -Sql 'SELECT COALESCE(MAX(id), 0) FROM orders;'

    Write-Host "Load-test food ID: $foodId"
    Write-Host "Starting order ID: $baselineOrderId"

    & docker compose -f compose.yaml -f compose.load.yaml --profile load run --rm `
        -e "FOOD_ID=$foodId" `
        -e "QUANTITY=$OrderQuantity" `
        -e "EXPECTED_STATUS=$ExpectedStatus" `
        -e "RATE=$Rate" `
        -e "DURATION=$Duration" `
        -e "PRE_ALLOCATED_VUS=$PreAllocatedVUs" `
        -e "MAX_VUS=$MaxVUs" `
        k6
    $k6ExitCode = $LASTEXITCODE

    $confirmedQuantity = Invoke-PostgresScalar `
        -Service 'order-db' `
        -Database 'orderdb' `
        -Sql "SELECT COALESCE(SUM(quantity), 0) FROM orders WHERE id > $baselineOrderId AND food_id = $foodId AND status = 'CONFIRMED';"

    $pendingCount = Invoke-PostgresScalar `
        -Service 'order-db' `
        -Database 'orderdb' `
        -Sql "SELECT COUNT(*) FROM orders WHERE id > $baselineOrderId AND food_id = $foodId AND status = 'PENDING';"

    $remainingStock = Invoke-PostgresScalar `
        -Service 'inventory-db' `
        -Database 'inventorydb' `
        -Sql "SELECT available_quantity FROM inventory WHERE id = $foodId;"

    $expectedRemainingStock = $Stock - [int64]$confirmedQuantity
    Write-Host "Confirmed quantity: $confirmedQuantity"
    Write-Host "Pending orders: $pendingCount"
    Write-Host "Remaining stock: $remainingStock (expected $expectedRemainingStock)"

    if ([int64]$pendingCount -ne 0) {
        Write-Error 'Load test left orders in PENDING'
        $exitCode = 1
    }
    if ([int64]$remainingStock -ne $expectedRemainingStock) {
        Write-Error 'Inventory decrement does not match confirmed order quantity'
        $exitCode = 1
    }
    if ($k6ExitCode -ne 0) {
        $exitCode = $k6ExitCode
    }
}
finally {
    Pop-Location
}

exit $exitCode
