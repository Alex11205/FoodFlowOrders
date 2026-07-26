# Testing FoodFlow

Use two levels of integration testing.

## Service integration tests

Each microservice owns a separate integration-test suite and a disposable
PostgreSQL Testcontainer:

- `foodfloworders/.../OrderIT` verifies `PENDING` to `CONFIRMED` or `REJECTED`
  transitions and duplicate inventory-event handling.
- `foodflow/.../InventoryIT` verifies reservation, rejection, and duplicate
  order-event handling.

Run either suite independently:

```powershell
mvn -pl foodfloworders -am verify
mvn -pl foodflow -am verify
```

Run all modules:

```powershell
mvn verify
```

These tests deliberately call the transactional event processors directly.
Kafka, Redis, and the other microservice are not required, so failures point to
the service whose transaction or database rule is broken.

## Cross-service end-to-end test

Keep one smaller test outside the service suites for the complete asynchronous
path:

1. Start the stack with `docker compose up --build -d`.
2. Insert or verify an inventory row.
3. POST an order to `http://localhost:8080/orders`.
4. Poll the order until it becomes `CONFIRMED` or `REJECTED`; do not use a fixed
   sleep for an asynchronous Kafka flow.
5. Assert that a confirmed order decremented inventory exactly once.
6. Inspect the retry and dead-letter topics when the terminal state is not
   reached before the timeout.

This Compose test proves container networking, Kafka serialization, consumer
groups, and the entire workflow. Keep most business cases in the faster
service-level suites instead of duplicating every scenario end to end.
