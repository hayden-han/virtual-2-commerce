import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ============================================================================
// Smoke Test 설정 (기본 동작 확인용)
// - 최소한의 부하로 시스템이 정상 동작하는지 확인
// - Stress Test 전에 실행 권장
// ============================================================================
export const options = {
  vus: 1,              // 1명의 가상 사용자
  duration: '30s',     // 30초 동안 실행
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 응답 시간 3초 이내
    http_req_failed: ['rate<0.1'],      // 에러율 10% 미만
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEBUG = __ENV.DEBUG === 'true';

const MEMBER_IDS = JSON.parse(__ENV.MEMBER_IDS || '[1, 2, 3, 4, 5]');
const PRODUCTS = JSON.parse(__ENV.PRODUCTS || '[{"id": 1, "price": 10000}]');

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function generateIdempotencyKey() {
  return `smoke-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export default function () {
  const memberId = getRandomElement(MEMBER_IDS);
  const product = getRandomElement(PRODUCTS);

  const payload = JSON.stringify({
    orderItems: [
      {
        productSummaryId: product.id,
        quantity: 1,
        price: product.price,
      },
    ],
    paymentSummary: {
      method: 'POINT',
      totalAmount: product.price,
      discountAmount: 0,
      chargeAmount: product.price,
    },
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Member-Id': String(memberId),
      'Idempotency-Key': generateIdempotencyKey(),
    },
  };

  console.log(`[Smoke] Sending order request for member ${memberId}, product ${product.id}`);

  const response = http.post(`${BASE_URL}/api/v1/orders`, payload, params);

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response has orderId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.orderId !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (success) {
    const body = JSON.parse(response.body);
    console.log(`[Smoke] Order created successfully: orderId=${body.orderId}`);
  } else {
    console.log(`[Smoke] Order failed: ${response.status} - ${response.body}`);
  }

  sleep(2);
}

export function handleSummary(data) {
  return {
    'results/smoke-test-result.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
