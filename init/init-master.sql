-- product_summary 테이블 생성
CREATE TABLE product_summary (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    stock_quantity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- product_summary 샘플 데이터
INSERT INTO product_summary (id, name, price, stock_quantity, created_at, updated_at) VALUES
(1, '샘플상품1', 1000, 10, NOW(), NOW()),
(2, '샘플상품2', 2000, 20, NOW(), NOW()),
(3, '샘플상품3', 3000, 30, NOW(), NOW()),
(4, '샘플상품4', 4000, 40, NOW(), NOW()),
(5, '샘플상품5', 5000, 50, NOW(), NOW()),
(6, '샘플상품6', 6000, 60, NOW(), NOW()),
(7, '샘플상품7', 7000, 70, NOW(), NOW()),
(8, '샘플상품8', 8000, 80, NOW(), NOW()),
(9, '샘플상품9', 9000, 90, NOW(), NOW()),
(10, '샘플상품10', 10000, 100, NOW(), NOW()),
(11, '샘플상품11', 11000, 110, NOW(), NOW()),
(12, '샘플상품12', 12000, 120, NOW(), NOW()),
(13, '샘플상품13', 13000, 130, NOW(), NOW()),
(14, '샘플상품14', 14000, 140, NOW(), NOW()),
(15, '샘플상품15', 15000, 150, NOW(), NOW()),
(16, '샘플상품16', 16000, 160, NOW(), NOW()),
(17, '샘플상품17', 17000, 170, NOW(), NOW()),
(18, '샘플상품18', 18000, 180, NOW(), NOW()),
(19, '샘플상품19', 19000, 190, NOW(), NOW()),
(20, '샘플상품20', 20000, 200, NOW(), NOW());
