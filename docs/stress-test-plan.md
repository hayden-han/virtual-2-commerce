# 주문 API 부하 테스트 기획서

## 1. 개요

본 문서는 주문 API(`POST /api/v1/orders`)에 대한 부하 테스트 기획을 담고 있습니다.
주문 API의 특성을 분석하고, 적합한 부하 테스트 유형을 선정하며, k6를 활용한 테스트 시나리오를 제시합니다.

---

## 2. 주문 API 아키텍처 분석

### 2.1 API 엔드포인트

```
POST /api/v1/orders
Headers:
  - X-Member-Id: Long (필수)
  - Idempotency-Key: String (필수)
```

### 2.2 주문 처리 흐름

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        단일 트랜잭션 범위                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  1. 재고 차감 (분산 락 + 조건부 UPDATE)                                   │
│  2. 회원 조회                                                            │
│  3. 주문 생성 (OrderSummary + OrderItem)                                 │
│  4. 쿠폰 사용 (선택, 조건부 UPDATE)                                       │
│  5. 결제 정보 생성                                                       │
│  6. 잔고 차감 (조건부 UPDATE)                                            │
│  7. 이벤트 발행                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      비동기 처리 (트랜잭션 커밋 후)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  @Async + @TransactionalEventListener                                   │
│  → Kafka 메시지 발행 (place-order-complete 토픽)                         │
│  → Kafka Consumer가 외부 데이터 플랫폼 API 호출                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 동시성 제어 메커니즘

| 대상 | 제어 방식 | 특징 |
|-----|---------|------|
| **재고 감소** | Redis 분산 락 (Redisson) + 조건부 UPDATE | 락 대기 시간 최대 30초, 재시도 3회 |
| **잔고 감소** | 조건부 UPDATE | `balance >= amount` 조건 |
| **쿠폰 사용** | 조건부 UPDATE | `usingAt IS NULL` 조건 |

### 2.4 주요 의존성

- **Redis**: 분산 락, 멱등성 캐시
- **PostgreSQL/MySQL**: 주문, 상품, 회원 데이터
- **Kafka**: 주문 완료 이벤트 비동기 처리
- **외부 API**: 데이터 플랫폼 연동

---

## 3. 부하 테스트 유형 분석

### 3.1 테스트 유형별 특징

| 테스트 유형 | 목적 | 부하 패턴 | 적합한 시스템 |
|------------|-----|----------|--------------|
| **Load Test** | 예상 부하에서의 성능 확인 | 일정한 부하 유지 | 일반적인 웹 서비스 |
| **Endurance Test** | 장시간 안정성 확인 | 장시간 일정 부하 | 메모리 누수 의심 시스템 |
| **Stress Test** | 한계점 및 복구력 확인 | 점진적 증가 | 경쟁 리소스가 많은 시스템 |
| **Peak Test** | 순간 최대 부하 대응력 | 급격한 스파이크 | 이벤트성 트래픽 시스템 |

### 3.2 주문 API의 특성 분석

주문 API는 다음과 같은 특성을 가집니다:

1. **경쟁 리소스 다수**
   - 동일 상품 재고에 대한 동시 접근
   - 회원 잔고에 대한 동시 차감
   - 쿠폰 사용 경쟁

2. **분산 락 사용**
   - 재고 차감 시 Redis 분산 락 사용
   - 락 대기 및 재시도 로직 존재
   - 동시 요청 증가 시 락 경합 심화

3. **복합 트랜잭션**
   - 6단계의 작업이 단일 트랜잭션으로 묶임
   - 트랜잭션 지속 시간이 상대적으로 김
   - DB 커넥션 점유 시간 증가

4. **비동기 처리 체인**
   - Kafka 메시지 발행/소비
   - 외부 API 호출

---

## 4. 테스트 유형 선정: Stress Test

### 4.1 선정 근거

**Stress Test**가 주문 API에 가장 적합한 부하 테스트입니다.

#### 근거 1: 경쟁 리소스 한계점 파악 필요

```
동시 주문 요청 증가
       │
       ▼
┌──────────────────┐
│  Redis 분산 락   │ ← 락 경합 증가
│  대기 시간 증가   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ DB 커넥션 풀     │ ← 커넥션 고갈 위험
│ 점유 시간 증가   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 응답 시간 급증   │ ← Breaking Point
│ 에러율 증가      │
└──────────────────┘
```

주문 API는 분산 락, DB 커넥션, Kafka 등 여러 공유 리소스를 사용합니다.
점진적으로 부하를 증가시켜 **어느 시점에서 시스템이 한계에 도달하는지** 파악해야 합니다.

#### 근거 2: 분산 락 재시도 로직의 한계 검증

```kotlin
// 현재 설정
@DistributedLock(
    retryCount = 3,
    retryDelayMillis = [200, 1000, 2000],  // 총 최대 3.2초 대기
    leaseTime = 30,
)
```

- 동시 요청이 많아지면 락 획득 실패율이 증가
- 재시도 3회 모두 실패 시 `LockAcquisitionException` 발생
- **몇 VU(Virtual Users)에서 락 실패가 급증하는지** 파악 필요

#### 근거 3: DB 커넥션 풀 포화 시점 확인

단일 트랜잭션 내에서 6개 작업이 순차 실행되므로:
- 트랜잭션 지속 시간 = Σ(각 작업 시간)
- 동시 요청 N개 → N개 커넥션 동시 점유
- **커넥션 풀 고갈 시점** 파악 필요

#### 근거 4: 복구력(Resilience) 검증

Stress Test는 한계점 도달 후 **부하 감소 시 시스템이 정상 복구되는지**도 확인합니다:
- 락 경합 해소 후 정상화 속도
- Kafka 메시지 백로그 소화 능력
- DB 커넥션 반환 후 안정화

### 4.2 다른 테스트 유형을 제외한 이유

| 테스트 유형 | 제외 이유 |
|------------|----------|
| **Load Test** | 예상 부하는 알 수 있으나, 경쟁 리소스의 한계점을 파악하기 어려움 |
| **Endurance Test** | 메모리 누수보다 동시성 문제가 더 critical한 관심사 |
| **Peak Test** | 순간 스파이크보다 점진적 증가 시 시스템 변화 관찰이 더 중요 |

### 4.3 Stress Test로 확인할 지표

| 지표 | 의미 | 임계값 (예시) |
|-----|-----|-------------|
| **응답 시간 p95** | 95% 요청의 응답 시간 | < 2000ms |
| **응답 시간 p99** | 99% 요청의 응답 시간 | < 5000ms |
| **에러율** | 실패한 요청 비율 | < 1% |
| **처리량(RPS)** | 초당 처리 요청 수 | 최대 RPS 확인 |
| **Breaking Point** | 에러율 급증 시점 | VU 수 확인 |

---

## 5. k6 적합성 검토

### 5.1 k6 개요

k6는 Grafana Labs에서 개발한 오픈소스 부하 테스트 도구입니다.

- **공식 사이트**: https://k6.io/
- **GitHub**: https://github.com/grafana/k6
- **최신 버전**: k6 1.0 (2025년 5월 릴리즈)

### 5.2 k6의 장점

| 특징 | 설명 |
|-----|-----|
| **JavaScript 기반** | 개발자 친화적 스크립트 작성 |
| **Go 엔진** | 고성능, 낮은 리소스 사용 |
| **다양한 프로토콜** | HTTP, WebSocket, gRPC 지원 |
| **유연한 시나리오** | stages, scenarios로 복잡한 부하 패턴 정의 |
| **풍부한 메트릭** | p95, p99, 에러율 등 기본 제공 |
| **CI/CD 통합** | GitHub Actions, Jenkins 등 연동 용이 |

### 5.3 Stress Test 지원 여부

k6는 `stages`를 통해 Stress Test 패턴을 쉽게 구현할 수 있습니다:

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 10 },   // 워밍업
    { duration: '5m', target: 50 },   // 점진적 증가
    { duration: '5m', target: 100 },  // 더 증가
    { duration: '5m', target: 150 },  // 한계 도전
    { duration: '2m', target: 0 },    // 복구 확인
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};
```

### 5.4 결론: k6 사용 적합

k6는 다음 이유로 주문 API Stress Test에 **적합**합니다:

1. **stages 지원**: 점진적 부하 증가 패턴 구현 가능
2. **thresholds**: 자동 성능 기준 검증
3. **낮은 학습 곡선**: JavaScript 기반으로 빠른 스크립트 작성
4. **로컬 실행**: 별도 인프라 없이 개발 환경에서 테스트 가능

---

## 6. 테스트 시나리오

### 6.1 시나리오 개요

```
Phase 1: Warm-up (2분)
├── VU: 0 → 10
└── 목적: 시스템 워밍업, 커넥션 풀 초기화

Phase 2: Ramp-up (5분)
├── VU: 10 → 50
└── 목적: 정상 부하 확인

Phase 3: Stress (5분)
├── VU: 50 → 100
└── 목적: 고부하 성능 확인

Phase 4: Breaking Point (5분)
├── VU: 100 → 150
└── 목적: 한계점 탐색

Phase 5: Recovery (2분)
├── VU: 150 → 0
└── 목적: 복구력 확인
```

### 6.2 테스트 데이터 요구사항

| 데이터 | 요구량 | 비고 |
|-------|-------|------|
| 회원 | 최소 100명 | 각 회원별 잔고 충분히 설정 |
| 상품 | 최소 10개 | 재고 충분히 설정 (10,000개 이상) |
| 쿠폰 | 선택사항 | 쿠폰 없는 시나리오 우선 |

### 6.3 성공 기준 (Thresholds)

| 지표 | 기준 | 근거 |
|-----|-----|-----|
| **http_req_duration p(95)** | < 2000ms | 사용자 체감 응답 시간 |
| **http_req_duration p(99)** | < 5000ms | 극단적 케이스 허용 범위 |
| **http_req_failed** | < 1% | 비즈니스 허용 에러율 |

### 6.4 테스트 스크립트 구조

```
k6/
├── scripts/
│   └── stress-test-order.js    # 메인 테스트 스크립트
├── data/
│   └── test-data.json          # 테스트 데이터 (회원ID, 상품ID 등)
└── results/
    └── (테스트 결과 저장)
```

---

## 7. k6 테스트 스크립트

### 7.1 메인 스크립트: stress-test-order.js

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// 커스텀 메트릭
const orderSuccess = new Counter('order_success');
const orderFailed = new Counter('order_failed');
const lockFailures = new Counter('lock_failures');
const orderDuration = new Trend('order_duration');

// Stress Test 설정
export const options = {
  stages: [
    { duration: '2m', target: 10 },    // Phase 1: Warm-up
    { duration: '5m', target: 50 },    // Phase 2: Ramp-up
    { duration: '5m', target: 100 },   // Phase 3: Stress
    { duration: '5m', target: 150 },   // Phase 4: Breaking Point
    { duration: '2m', target: 0 },     // Phase 5: Recovery
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.01'],
    order_success: ['count>0'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 데이터
const MEMBER_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]; // 실제 회원 ID로 교체
const PRODUCT_IDS = [1, 2, 3, 4, 5];                 // 실제 상품 ID로 교체

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function generateIdempotencyKey() {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export default function () {
  const memberId = getRandomElement(MEMBER_IDS);
  const productId = getRandomElement(PRODUCT_IDS);

  const payload = JSON.stringify({
    orderItems: [
      {
        productSummaryId: productId,
        quantity: 1,
        price: 10000,
      },
    ],
    paymentSummary: {
      method: 'POINT',
      totalAmount: 10000,
      discountAmount: 0,
      chargeAmount: 10000,
    },
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Member-Id': String(memberId),
      'Idempotency-Key': generateIdempotencyKey(),
    },
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/v1/orders`, payload, params);
  const duration = Date.now() - startTime;

  orderDuration.add(duration);

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
    orderSuccess.add(1);
  } else {
    orderFailed.add(1);

    // 락 실패 감지
    if (response.body && response.body.includes('잠시 후 다시 시도')) {
      lockFailures.add(1);
    }

    // 디버그 출력
    if (__ENV.DEBUG) {
      console.log(`Failed: ${response.status} - ${response.body}`);
    }
  }

  sleep(1); // 요청 간 1초 대기
}

export function handleSummary(data) {
  return {
    'results/stress-test-summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  // k6 기본 summary 포맷 사용
  return '';
}
```

### 7.2 실행 명령어

```bash
# 기본 실행
k6 run k6/scripts/stress-test-order.js

# 환경 변수와 함께 실행
k6 run -e BASE_URL=http://localhost:8080 -e DEBUG=true k6/scripts/stress-test-order.js

# 결과를 JSON으로 출력
k6 run --out json=results/stress-test-result.json k6/scripts/stress-test-order.js
```

---

## 8. 테스트 실행 계획

### 8.1 사전 준비

1. **테스트 환경 구성**
   - Docker Compose로 로컬 환경 구성 (DB, Redis, Kafka)
   - 애플리케이션 서버 기동

2. **테스트 데이터 준비**
   - 회원 데이터 생성 (충분한 잔고)
   - 상품 데이터 생성 (충분한 재고)

3. **k6 설치**
   ```bash
   # macOS
   brew install k6

   # 또는 Docker
   docker pull grafana/k6
   ```

### 8.2 테스트 실행 순서

1. **Smoke Test** (선택)
   - VU 1~2로 기본 동작 확인

2. **Stress Test**
   - 메인 스크립트 실행
   - 약 19분 소요

3. **결과 분석**
   - Breaking Point 확인
   - 병목 구간 식별

### 8.3 모니터링 포인트

- **애플리케이션**: 응답 시간, 에러 로그
- **Redis**: 락 대기 시간, 메모리 사용량
- **PostgreSQL/MySQL**: 커넥션 수, 쿼리 지연
- **Kafka**: 메시지 백로그, Consumer lag

---

## 9. 예상 결과 및 개선 방향

### 9.1 예상되는 병목 구간

| 구간 | 예상 문제 | 개선 방향 |
|-----|---------|----------|
| **분산 락** | 락 경합으로 인한 대기 시간 증가 | 락 범위 축소, 재시도 전략 조정 |
| **DB 커넥션** | 커넥션 풀 고갈 | 풀 사이즈 조정, 트랜잭션 최적화 |
| **트랜잭션 지속 시간** | 롱 트랜잭션으로 인한 DB 부하 | 트랜잭션 분리, CQRS 패턴 검토 |

### 9.2 Stress Test 결과에 따른 액션

| Breaking Point | 권장 액션 |
|---------------|----------|
| VU < 30 | 아키텍처 재검토 필요 |
| 30 ≤ VU < 50 | 분산 락 최적화 우선 |
| 50 ≤ VU < 100 | DB 커넥션 풀 튜닝 |
| VU ≥ 100 | 현재 구조로 충분, 모니터링 강화 |

---

## 10. 참고 자료

- [Grafana k6 공식 문서](https://grafana.com/docs/k6/latest/)
- [k6 Stress Testing Guide](https://grafana.com/docs/k6/latest/testing-guides/test-types/stress-testing/)
- [k6 GitHub Repository](https://github.com/grafana/k6)
