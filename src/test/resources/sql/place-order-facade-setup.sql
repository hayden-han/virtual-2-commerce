-- PlaceOrderFacade 통합테스트용 데이터 (5100번대 ID 사용)

-- 회원 데이터 생성
INSERT INTO member (id, email, pwd, member_type, created_at, updated_at)
VALUES (5101, 'place-order-facade-test@example.com', 'securepassword', 'GENERAL', '2025-01-02 00:00:00', '2025-01-18 13:20:12');

-- 회원의 잔액 데이터 생성
INSERT INTO member_balance (id, member_id, balance, created_at, updated_at)
VALUES (5101, 5101, 2000000, '2025-01-02 00:00:00', '2025-03-18 13:00:00');

-- 상품 데이터 생성
INSERT INTO product_summary (id, name, price, stock_quantity, created_at, updated_at)
VALUES (5101, '테스트 상품 A', 1790000, 10, '2025-03-18 13:00:00', '2025-03-18 13:00:00'),
       (5102, '테스트 상품 B', 89000, 20, '2025-03-18 13:00:00', '2025-03-18 13:00:00'),
       (5103, '테스트 상품 C', 12000, 100, '2025-03-18 13:00:00', '2025-03-18 13:00:00');
