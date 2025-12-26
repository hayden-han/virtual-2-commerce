-- ============================================================================
-- Stress Test용 데이터
-- - 회원 100명 (충분한 잔고: 1억원)
-- - 상품 10개 (저렴한 가격: 100~1000원, 충분한 재고: 100만개)
-- ============================================================================

-- 기존 테스트 데이터가 있으면 추가 회원/상품만 삽입

-- 회원 추가 (ID 4 ~ 100)
INSERT INTO member (id, email, pwd, member_type, created_at, updated_at)
SELECT n, CONCAT('stress-test-user', n, '@test.com'), '1234', 'GENERAL', NOW(), NOW()
FROM (
    SELECT @row := @row + 1 as n
    FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t1,
         (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2,
         (SELECT @row := 3) init
) numbers
WHERE n <= 100
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 회원 잔고 추가 (각 회원당 100,000,000원 = 1억원)
INSERT INTO member_balance (member_id, balance, created_at, updated_at)
SELECT n, 100000000, NOW(), NOW()
FROM (
    SELECT @row2 := @row2 + 1 as n
    FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t1,
         (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t2,
         (SELECT @row2 := 0) init
) numbers
WHERE n <= 100
ON DUPLICATE KEY UPDATE balance = 100000000, updated_at = NOW();

-- 상품 추가 (10개 상품, 저렴한 가격 100~1000원, 각각 1,000,000개 재고)
INSERT INTO product_summary (id, name, price, stock_quantity, created_at, updated_at)
VALUES
    (1, 'Stress Test Product 1', 100, 1000000, NOW(), NOW()),
    (2, 'Stress Test Product 2', 200, 1000000, NOW(), NOW()),
    (3, 'Stress Test Product 3', 300, 1000000, NOW(), NOW()),
    (4, 'Stress Test Product 4', 400, 1000000, NOW(), NOW()),
    (5, 'Stress Test Product 5', 500, 1000000, NOW(), NOW()),
    (6, 'Stress Test Product 6', 600, 1000000, NOW(), NOW()),
    (7, 'Stress Test Product 7', 700, 1000000, NOW(), NOW()),
    (8, 'Stress Test Product 8', 800, 1000000, NOW(), NOW()),
    (9, 'Stress Test Product 9', 900, 1000000, NOW(), NOW()),
    (10, 'Stress Test Product 10', 1000, 1000000, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    stock_quantity = 1000000,
    updated_at = NOW();
