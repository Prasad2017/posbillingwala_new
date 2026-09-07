# 13 Product Service Engine

Catalog hierarchy for all business templates.

```text
Food Type → Category → Subcategory → Product → Portion (+ Combo)
```

## Facade
`Extra/ProductServiceEngine.java`

## Live entry
Settings → Master Data (`ProductMaster`, categories, portions, combos).

## Safe rules
Additive catalog only; bill line snapshots already freeze name/price.
