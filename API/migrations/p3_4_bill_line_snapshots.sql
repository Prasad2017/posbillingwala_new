-- P3-4: Bill line snapshots (MySQL) — additive only, no DROP
-- Frozen product + portion name/price on invoice lines

ALTER TABLE `invoice_final_product`
  ADD COLUMN `portionId` int(10) UNSIGNED DEFAULT NULL AFTER `productName`,
  ADD COLUMN `portionName` varchar(64) DEFAULT NULL AFTER `portionId`,
  ADD COLUMN `snapshotProductName` text DEFAULT NULL AFTER `portionName`,
  ADD COLUMN `snapshotLinePrice` decimal(16,2) DEFAULT NULL AFTER `snapshotProductName`;
