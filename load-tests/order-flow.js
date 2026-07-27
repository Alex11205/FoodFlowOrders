import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

function numberFromEnvironment(name, fallback) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(`${name} must be a positive number`);
  }
  return value;
}

const baseUrl = (__ENV.BASE_URL || 'http://order-service:8080').replace(/\/$/, '');
const foodId = numberFromEnvironment('FOOD_ID', 1);
const quantity = numberFromEnvironment('QUANTITY', 1);
const rate = numberFromEnvironment('RATE', 5);
const duration = __ENV.DURATION || '1m';
const preAllocatedVUs = numberFromEnvironment('PRE_ALLOCATED_VUS', 20);
const maxVUs = numberFromEnvironment('MAX_VUS', 100);
const pollIntervalSeconds = numberFromEnvironment('POLL_INTERVAL_SECONDS', 0.2);
const workflowTimeoutMs = numberFromEnvironment('WORKFLOW_TIMEOUT_MS', 10000);
const maxErrorRate = numberFromEnvironment('MAX_ERROR_RATE', 0.01);
const maxPostP95Ms = numberFromEnvironment('MAX_POST_P95_MS', 500);
const maxWorkflowP95Ms = numberFromEnvironment('MAX_WORKFLOW_P95_MS', 5000);
const expectedStatus = (__ENV.EXPECTED_STATUS || 'CONFIRMED').toUpperCase();

if (!['CONFIRMED', 'REJECTED', 'ANY'].includes(expectedStatus)) {
  throw new Error('EXPECTED_STATUS must be CONFIRMED, REJECTED, or ANY');
}
if (preAllocatedVUs > maxVUs) {
  throw new Error('PRE_ALLOCATED_VUS cannot be greater than MAX_VUS');
}
if (maxErrorRate >= 1) {
  throw new Error('MAX_ERROR_RATE must be less than 1');
}

const workflowDuration = new Trend('order_workflow_duration', true);
const workflowCompleted = new Rate('order_workflow_completed');
const expectedStatusReached = new Rate('expected_status_reached');
const pollAttempts = new Trend('order_poll_attempts');
const terminalOrders = new Counter('orders_terminal');

export const options = {
  scenarios: {
    order_flow: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration,
      preAllocatedVUs,
      maxVUs,
      gracefulStop: '15s',
    },
  },
  thresholds: {
    'http_req_failed{endpoint:create_order}': [`rate<${maxErrorRate}`],
    'http_req_duration{endpoint:create_order}': [`p(95)<${maxPostP95Ms}`],
    'http_req_failed{endpoint:get_order}': [`rate<${maxErrorRate}`],
    order_workflow_completed: [`rate>=${1 - maxErrorRate}`],
    expected_status_reached: [`rate>=${1 - maxErrorRate}`],
    order_workflow_duration: [`p(95)<${maxWorkflowP95Ms}`],
    dropped_iterations: ['count==0'],
    checks: [`rate>=${1 - maxErrorRate}`],
  },
};

const requestHeaders = {
  headers: { 'Content-Type': 'application/json' },
  tags: {
    endpoint: 'create_order',
    name: 'POST /orders',
  },
  timeout: '5s',
};

function parseJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function recordIncompleteWorkflow(polls) {
  workflowCompleted.add(false);
  expectedStatusReached.add(false);
  pollAttempts.add(polls);
  terminalOrders.add(1, { status: 'INCOMPLETE' });
}

export default function () {
  const workflowStartedAt = Date.now();
  const createResponse = http.post(
    `${baseUrl}/orders`,
    JSON.stringify({ foodId, quantity }),
    requestHeaders
  );
  const createdOrder = parseJson(createResponse);
  const orderId = createdOrder && createdOrder.orderId;

  const accepted = check(createResponse, {
    'order accepted with 201': (response) => response.status === 201,
    'order response contains an ID': () => Number.isFinite(Number(orderId)),
    'new order starts PENDING': () => createdOrder && createdOrder.status === 'PENDING',
  }, { workflow: 'order' });

  if (!accepted || !orderId) {
    recordIncompleteWorkflow(0);
    return;
  }

  let status = createdOrder.status;
  let polls = 0;

  while (Date.now() - workflowStartedAt < workflowTimeoutMs) {
    const getResponse = http.get(`${baseUrl}/orders/${orderId}`, {
      tags: {
        endpoint: 'get_order',
        name: 'GET /orders/:id',
      },
      timeout: '5s',
    });
    polls += 1;

    if (getResponse.status === 200) {
      const currentOrder = parseJson(getResponse);
      if (currentOrder && currentOrder.status) {
        status = currentOrder.status;
      }
    }

    if (status === 'CONFIRMED' || status === 'REJECTED') {
      break;
    }
    sleep(pollIntervalSeconds);
  }

  const terminal = status === 'CONFIRMED' || status === 'REJECTED';
  const matchesExpectation =
    terminal && (expectedStatus === 'ANY' || status === expectedStatus);

  workflowCompleted.add(terminal);
  expectedStatusReached.add(matchesExpectation);
  pollAttempts.add(polls);
  terminalOrders.add(1, { status: terminal ? status : 'INCOMPLETE' });

  if (terminal) {
    workflowDuration.add(Date.now() - workflowStartedAt, { status });
  }

  check({ status, terminal }, {
    'order reaches a terminal status': (result) => result.terminal,
    'order reaches the expected status': (result) =>
      result.terminal &&
      (expectedStatus === 'ANY' || result.status === expectedStatus),
  }, { workflow: 'order' });
}
