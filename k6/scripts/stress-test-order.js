import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ============================================================================
// 커스텀 메트릭 정의
// ============================================================================
const orderSuccess = new Counter('order_success');
const orderFailed = new Counter('order_failed');
const lockFailures = new Counter('lock_failures');
const insufficientStock = new Counter('insufficient_stock');
const insufficientBalance = new Counter('insufficient_balance');
const orderDuration = new Trend('order_duration');
const successRate = new Rate('success_rate');

// ============================================================================
// Stress Test 설정
// ============================================================================
export const options = {
  stages: [
    { duration: '2m', target: 10 },    // Phase 1: Warm-up (워밍업)
    { duration: '5m', target: 50 },    // Phase 2: Ramp-up (점진적 증가)
    { duration: '5m', target: 100 },   // Phase 3: Stress (스트레스)
    { duration: '5m', target: 150 },   // Phase 4: Breaking Point (한계점 탐색)
    { duration: '2m', target: 0 },     // Phase 5: Recovery (복구 확인)
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],  // 응답 시간 기준
    http_req_failed: ['rate<0.01'],                   // 에러율 1% 미만
    order_success: ['count>0'],                       // 최소 1건 성공
    success_rate: ['rate>0.95'],                      // 성공률 95% 이상
  },
};

// ============================================================================
// 환경 변수 및 설정
// ============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DEBUG = __ENV.DEBUG === 'true';

// 테스트 데이터 - 실제 환경에 맞게 수정 필요
const MEMBER_IDS = JSON.parse(__ENV.MEMBER_IDS || '[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]');
const PRODUCTS = JSON.parse(__ENV.PRODUCTS || '[{"id": 1, "price": 10000}, {"id": 2, "price": 20000}, {"id": 3, "price": 15000}]');

// ============================================================================
// 유틸리티 함수
// ============================================================================
function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function generateIdempotencyKey() {
  return `stress-${Date.now()}-${__VU}-${__ITER}-${Math.random().toString(36).substr(2, 9)}`;
}

// ============================================================================
// 메인 테스트 함수
// ============================================================================
export default function () {
  const memberId = getRandomElement(MEMBER_IDS);
  const product = getRandomElement(PRODUCTS);
  const quantity = getRandomInt(1, 3);
  const totalAmount = product.price * quantity;

  const payload = JSON.stringify({
    orderItems: [
      {
        productSummaryId: product.id,
        quantity: quantity,
        price: product.price,
      },
    ],
    paymentSummary: {
      method: 'POINT',
      totalAmount: totalAmount,
      discountAmount: 0,
      chargeAmount: totalAmount,
    },
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Member-Id': String(memberId),
      'Idempotency-Key': generateIdempotencyKey(),
    },
    tags: {
      name: 'PlaceOrder',
    },
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
  const duration = Date.now() - startTime;

  orderDuration.add(duration);

  // 응답 검증
  const isSuccess = check(response, {
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

  // 메트릭 기록
  successRate.add(isSuccess ? 1 : 0);

  if (isSuccess) {
    orderSuccess.add(1);
  } else {
    orderFailed.add(1);

    // 실패 원인 분류
    const responseBody = response.body || '';

    if (responseBody.includes('잠시 후 다시 시도') || responseBody.includes('Lock')) {
      lockFailures.add(1);
      if (DEBUG) console.log(`[VU ${__VU}] 락 획득 실패`);
    } else if (responseBody.includes('재고가 부족')) {
      insufficientStock.add(1);
      if (DEBUG) console.log(`[VU ${__VU}] 재고 부족`);
    } else if (responseBody.includes('잔고') || responseBody.includes('금액이 부족')) {
      insufficientBalance.add(1);
      if (DEBUG) console.log(`[VU ${__VU}] 잔고 부족`);
    }

    if (DEBUG) {
      console.log(`[VU ${__VU}] Failed: ${response.status} - ${responseBody.substring(0, 200)}`);
    }
  }

  // 요청 간 대기 (Think Time)
  sleep(getRandomInt(1, 2));
}

// ============================================================================
// 테스트 완료 후 결과 요약
// ============================================================================
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

  return {
    [`results/stress-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
