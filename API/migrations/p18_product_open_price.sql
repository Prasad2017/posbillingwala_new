-- P18: Product open price flag (MySQL) — additive only, no DROP
-- When openPrice = 'on', cashier enters unit price during billing.

ALTER TABLE `products`
  ADD COLUMN `openPrice` VARCHAR(10) NOT NULL DEFAULT 'off' AFTER `productPrice`;
