-- 데이터베이스 생성 (필요시)
-- CREATE DATABASE IF NOT EXISTS virtual_commerce DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE virtual_commerce;

-- 0. 회원 테이블
CREATE TABLE IF NOT EXISTS member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    pwd VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 이메일 유니크 인덱스
CREATE UNIQUE INDEX idx_member_email ON member(email);

-- 1. 회원 잔액 테이블
CREATE TABLE IF NOT EXISTS member_balance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회원별 유니크 인덱스 (한 회원당 하나의 잔액 레코드)
CREATE UNIQUE INDEX idx_member_balance_member_id ON member_balance(member_id);

-- 2. 상품 정보 테이블
CREATE TABLE IF NOT EXISTS product_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    stock_quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품명 인덱스
CREATE INDEX idx_product_summary_name ON product_summary(name);

-- 3. 쿠폰 정보 테이블
CREATE TABLE IF NOT EXISTS coupon_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    discount_percentage BIGINT NOT NULL,
    expired_at BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 만료시간 인덱스
CREATE INDEX idx_coupon_summary_expired_at ON coupon_summary(expired_at);

-- 4. 쿠폰 소유자 테이블
CREATE TABLE IF NOT EXISTS coupon_owners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_summary_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    using_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회원별 쿠폰 조회 인덱스
CREATE INDEX idx_coupon_owners_member_id ON coupon_owners(member_id);

-- 5. 쿠폰 발급 상태 테이블
CREATE TABLE IF NOT EXISTS coupon_issuance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_summary_id BIGINT NOT NULL,
    policy_type VARCHAR(50) NOT NULL COMMENT 'GENERAL, FIRST_COME_FIRST_SERVE',
    max_count INT NULL COMMENT '최대 발급 수 (NULL이면 무제한)',
    issued_count INT NOT NULL DEFAULT 0,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰 요약별 발급 정책 조회 인덱스
CREATE INDEX idx_coupon_issuance_coupon_summary_id ON coupon_issuance(coupon_summary_id);
-- 발급 가능 시간 조회 인덱스
CREATE INDEX idx_coupon_issuance_period ON coupon_issuance(start_at, end_at);

-- 6. 쿠폰 발급 및 사용 이력 테이블
CREATE TABLE IF NOT EXISTS coupon_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_summary_id BIGINT NOT NULL,
    coupon_owner_id BIGINT NULL,
    member_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL COMMENT 'ISSUED, USED, CANCELLED, EXPIRED',
    order_summary_id BIGINT NULL COMMENT '사용/취소 시 주문 ID',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰별 이력 조회 인덱스
CREATE INDEX idx_coupon_history_coupon_summary_id ON coupon_history(coupon_summary_id);
-- 회원별 이력 조회 인덱스
CREATE INDEX idx_coupon_history_member_id ON coupon_history(member_id);
-- 이벤트 타입별 조회 인덱스
CREATE INDEX idx_coupon_history_event_type ON coupon_history(event_type, created_at);
-- 쿠폰 소유자별 이력 조회 인덱스
CREATE INDEX idx_coupon_history_coupon_owner_id ON coupon_history(coupon_owner_id);

-- 7. 주문 요약 테이블
CREATE TABLE IF NOT EXISTS order_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 회원별 주문 조회 인덱스
CREATE INDEX idx_order_summary_member_id ON order_summary(member_id);

-- 8. 주문 상세 테이블
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_summary_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    price BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주문별 상세 조회 인덱스
CREATE INDEX idx_order_item_order_summary_id ON order_item(order_summary_id);
-- 상위 상품조회 통계 인덱스
CREATE INDEX idx_order_item_product_id ON order_item(created_at, product_id);
