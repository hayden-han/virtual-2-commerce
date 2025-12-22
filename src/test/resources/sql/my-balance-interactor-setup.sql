-- MyBalanceInteractor 통합테스트용 데이터

-- 테스트용 회원 (초기 잔액 100,000원)
INSERT INTO member (id, email, pwd, member_type, created_at, updated_at)
VALUES (5001, 'balance-interactor-test@example.com', 'password123', 'GENERAL', '2025-01-01 00:00:00', '2025-01-01 00:00:00');

INSERT INTO member_balance (id, member_id, balance, created_at, updated_at)
VALUES (5001, 5001, 100000, '2025-01-01 00:00:00', '2025-01-01 00:00:00');
