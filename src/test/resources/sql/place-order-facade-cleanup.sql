-- PlaceOrderFacade 통합테스트 데이터 정리 (5100번대 ID 사용)

-- 주문 관련 데이터 삭제
DELETE FROM payment_summary WHERE order_summary_id IN (
    SELECT id FROM order_summary WHERE member_id = 5101
);
DELETE FROM order_item WHERE order_summary_id IN (
    SELECT id FROM order_summary WHERE member_id = 5101
);
DELETE FROM order_summary WHERE member_id = 5101;

-- 상품 데이터 삭제
DELETE FROM product_summary WHERE id IN (5101, 5102, 5103);

-- 잔액 및 회원 데이터 삭제
DELETE FROM member_balance WHERE id = 5101;
DELETE FROM member WHERE id = 5101;
