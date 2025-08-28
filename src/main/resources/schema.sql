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

-- 5. 주문 요약 테이블
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

-- 6. 주문 상세 테이블
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
