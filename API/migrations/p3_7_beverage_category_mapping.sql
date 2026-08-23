-- P3-7: Map legacy beverage-like category names to Beverage food type (MySQL) — additive only
-- Safe: only updates categories still on default Food type

UPDATE `categories` c
INNER JOIN `food_types` ft_food ON ft_food.foodTypeCode = 'food'
INNER JOIN `food_types` ft_bev ON ft_bev.foodTypeCode = 'beverage'
SET c.foodTypeId = ft_bev.foodTypeId
WHERE (c.foodTypeId IS NULL OR c.foodTypeId = ft_food.foodTypeId)
  AND c.categoryStatus = 'active'
  AND (
    LOWER(c.categoryName) LIKE '%beverage%'
    OR LOWER(c.categoryName) LIKE '%drink%'
    OR LOWER(c.categoryName) LIKE '%juice%'
    OR LOWER(c.categoryName) LIKE '%mocktail%'
    OR LOWER(c.categoryName) LIKE '%cocktail%'
    OR LOWER(c.categoryName) LIKE '%tea%'
    OR LOWER(c.categoryName) LIKE '%coffee%'
    OR LOWER(c.categoryName) LIKE '%shake%'
    OR LOWER(c.categoryName) LIKE '%lassi%'
    OR LOWER(c.categoryName) LIKE '%soda%'
    OR LOWER(c.categoryName) LIKE '%soft%'
    OR LOWER(c.categoryName) LIKE '%cold%'
    OR LOWER(c.categoryName) LIKE '%water%'
    OR LOWER(c.categoryName) LIKE '%milk%'
  );
