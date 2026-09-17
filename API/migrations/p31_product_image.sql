-- P31: Optional product image (MySQL) — additive only, no DROP
-- Stores relative media path or data:image/...;base64,... when uploaded from the app.

ALTER TABLE `products`
  ADD COLUMN `productImage` MEDIUMTEXT DEFAULT NULL AFTER `productName`;
