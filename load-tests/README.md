# FoodFlow Load Tests

The order-flow test measures both HTTP acceptance and the asynchronous path:

`POST /orders -> Kafka -> inventory reservation -> Kafka -> terminal order status`

It uses a constant order arrival rate and polls `GET /orders/{id}` until each
order becomes `CONFIRMED` or `REJECTED`.

## Run

Start the application stack:

```powershell
docker compose up --build -d
```

Run the default test at five orders per second for one minute:

```powershell
.\load-tests\run.ps1
```

Run a short smoke test:

```powershell
.\load-tests\run.ps1 -Rate 1 -Duration 15s -PreAllocatedVUs 5 -MaxVUs 20
```

Run a higher load:

```powershell
.\load-tests\run.ps1 -Rate 20 -Duration 5m -PreAllocatedVUs 50 -MaxVUs 250
```

The runner resets a dedicated `k6-load-test-item`, captures the starting order
ID, runs k6 in Docker, and then verifies:

- No new order remains `PENDING`.
- The inventory decrement equals the total confirmed quantity.
- k6 thresholds for failures, dropped iterations, POST latency, and workflow
  latency all pass.

The defaults require fewer than 1% failures, POST p95 below 500 ms, and
end-to-end workflow p95 below 5 seconds. Treat these as initial local baselines,
then replace them with requirements derived from the deployment environment.

For an insufficient-stock scenario:

```powershell
.\load-tests\run.ps1 -Rate 5 -Duration 30s -Stock 0 -ExpectedStatus REJECTED
```

