import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { JsonMap } from '@/api/client'
import { isApiSuccess, apiMessage } from '@/api/client'
import { catalogApi, tablesApi } from '@/api/services'
import { useUserId } from '@/stores/authStore'
import { useI18n } from '@/i18n'
import { PageHeader, LoadingBlock, EmptyState, Money } from '@/shared/ui'

function str(row: JsonMap, ...keys: string[]): string {
  for (const k of keys) {
    const v = row[k]
    if (v != null && String(v).trim()) return String(v)
  }
  return ''
}

function num(row: JsonMap, ...keys: string[]): number {
  for (const k of keys) {
    const v = row[k]
    if (v != null && v !== '') {
      const n = Number(v)
      if (!Number.isNaN(n)) return n
    }
  }
  return 0
}

type Section =
  | 'categories'
  | 'subcategories'
  | 'portions'
  | 'products'
  | 'combos'
  | 'tables'

export function MastersPage() {
  const t = useI18n((s) => s.t)
  const userId = useUserId()
  const qc = useQueryClient()
  const [section, setSection] = useState<Section>('categories')
  const [msg, setMsg] = useState<string | null>(null)

  // form fields
  const [categoryName, setCategoryName] = useState('')
  const [subName, setSubName] = useState('')
  const [subCatId, setSubCatId] = useState('')
  const [portionName, setPortionName] = useState('')
  const [productName, setProductName] = useState('')
  const [productPrice, setProductPrice] = useState('')
  const [productCatId, setProductCatId] = useState('')
  const [comboName, setComboName] = useState('')
  const [comboPrice, setComboPrice] = useState('')
  const [tableNumber, setTableNumber] = useState('')
  const [tableName, setTableName] = useState('')
  const [capacity, setCapacity] = useState('4')

  const listQ = useQuery({
    queryKey: ['masters', section, userId],
    enabled: Boolean(userId),
    queryFn: async () => {
      const id = userId!
      switch (section) {
        case 'categories':
          return catalogApi.getCategories(id)
        case 'subcategories':
          return catalogApi.getSubcategories(id)
        case 'portions':
          return catalogApi.getPortionMasters(id)
        case 'products':
          return catalogApi.getProducts(id)
        case 'combos':
          return catalogApi.getCombos(id)
        case 'tables':
          return tablesApi.getTables(id)
      }
    },
  })

  const catsQ = useQuery({
    queryKey: ['masters-cats', userId],
    enabled: Boolean(userId) && (section === 'subcategories' || section === 'products'),
    queryFn: () => catalogApi.getCategories(userId!),
  })

  const insert = useMutation({
    mutationFn: async () => {
      if (!userId) throw new Error('Not signed in')
      let res: JsonMap
      switch (section) {
        case 'categories':
          res = await catalogApi.insertCategory({
            userId,
            categoryName: categoryName.trim(),
          })
          break
        case 'subcategories':
          res = await catalogApi.insertSubcategory({
            userId,
            subcategoryName: subName.trim(),
            categoryId: subCatId,
          })
          break
        case 'portions':
          res = await catalogApi.insertPortionMaster({
            userId,
            portionMasterName: portionName.trim(),
          })
          break
        case 'products':
          res = await catalogApi.insertProduct({
            userId,
            productName: productName.trim(),
            productPrice: productPrice,
            categoryId: productCatId,
          })
          break
        case 'combos':
          res = await catalogApi.insertCombo({
            userId,
            comboName: comboName.trim(),
            comboPrice: comboPrice,
          })
          break
        case 'tables':
          res = await tablesApi.insertPosTable({
            userId,
            tableNumber: tableNumber.trim(),
            tableName: tableName.trim(),
            capacity,
          })
          break
      }
      if (!isApiSuccess(res)) throw new Error(apiMessage(res))
      return res
    },
    onSuccess() {
      setMsg('Saved')
      setCategoryName('')
      setSubName('')
      setPortionName('')
      setProductName('')
      setProductPrice('')
      setComboName('')
      setComboPrice('')
      setTableNumber('')
      setTableName('')
      void qc.invalidateQueries({ queryKey: ['masters', section, userId] })
    },
    onError(err) {
      setMsg(err instanceof Error ? err.message : 'Save failed')
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    insert.mutate()
  }

  const sections: { id: Section; label: string }[] = [
    { id: 'categories', label: 'Categories' },
    { id: 'subcategories', label: 'Subcategories' },
    { id: 'portions', label: 'Portion masters' },
    { id: 'products', label: 'Products' },
    { id: 'combos', label: 'Combos' },
    { id: 'tables', label: 'Tables' },
  ]

  return (
    <div>
      <PageHeader title={t('masters')} subtitle="Catalog setup" />

      <div className="tabs">
        {sections.map((s) => (
          <button
            key={s.id}
            type="button"
            className={`tab${section === s.id ? ' active' : ''}`}
            onClick={() => {
              setSection(s.id)
              setMsg(null)
            }}
          >
            {s.label}
          </button>
        ))}
      </div>

      {msg ? <p className="muted">{msg}</p> : null}

      <form className="card" style={{ maxWidth: 440, marginBottom: '1rem' }} onSubmit={onSubmit}>
        {section === 'categories' ? (
          <div className="field">
            <label htmlFor="cat">Category name</label>
            <input
              id="cat"
              value={categoryName}
              onChange={(e) => setCategoryName(e.target.value)}
              required
            />
          </div>
        ) : null}

        {section === 'subcategories' ? (
          <>
            <div className="field">
              <label htmlFor="subcat-parent">Category</label>
              <select
                id="subcat-parent"
                value={subCatId}
                onChange={(e) => setSubCatId(e.target.value)}
                required
              >
                <option value="">Select…</option>
                {(catsQ.data ?? []).map((c) => (
                  <option key={str(c, 'categoryId')} value={str(c, 'categoryId')}>
                    {str(c, 'categoryName')}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="subname">Subcategory name</label>
              <input
                id="subname"
                value={subName}
                onChange={(e) => setSubName(e.target.value)}
                required
              />
            </div>
          </>
        ) : null}

        {section === 'portions' ? (
          <div className="field">
            <label htmlFor="pm">Portion master name</label>
            <input
              id="pm"
              value={portionName}
              onChange={(e) => setPortionName(e.target.value)}
              required
            />
          </div>
        ) : null}

        {section === 'products' ? (
          <>
            <div className="field">
              <label htmlFor="pcat">Category</label>
              <select
                id="pcat"
                value={productCatId}
                onChange={(e) => setProductCatId(e.target.value)}
              >
                <option value="">Optional…</option>
                {(catsQ.data ?? []).map((c) => (
                  <option key={str(c, 'categoryId')} value={str(c, 'categoryId')}>
                    {str(c, 'categoryName')}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="pname">Product name</label>
              <input
                id="pname"
                value={productName}
                onChange={(e) => setProductName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="pprice">Price</label>
              <input
                id="pprice"
                type="number"
                min={0}
                step="0.01"
                value={productPrice}
                onChange={(e) => setProductPrice(e.target.value)}
                required
              />
            </div>
          </>
        ) : null}

        {section === 'combos' ? (
          <>
            <div className="field">
              <label htmlFor="cname">Combo name</label>
              <input
                id="cname"
                value={comboName}
                onChange={(e) => setComboName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="cprice">Price</label>
              <input
                id="cprice"
                type="number"
                min={0}
                step="0.01"
                value={comboPrice}
                onChange={(e) => setComboPrice(e.target.value)}
                required
              />
            </div>
          </>
        ) : null}

        {section === 'tables' ? (
          <>
            <div className="field">
              <label htmlFor="tnum">Table number</label>
              <input
                id="tnum"
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="tname">Table name</label>
              <input
                id="tname"
                value={tableName}
                onChange={(e) => setTableName(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="cap">Capacity</label>
              <input
                id="cap"
                type="number"
                min={1}
                value={capacity}
                onChange={(e) => setCapacity(e.target.value)}
              />
            </div>
          </>
        ) : null}

        <button type="submit" className="btn" disabled={insert.isPending}>
          {t('add')}
        </button>
      </form>

      {listQ.isLoading ? <LoadingBlock /> : null}
      {(listQ.data?.length ?? 0) === 0 && !listQ.isLoading ? (
        <EmptyState message={t('noData')} />
      ) : (
        <div className="card" style={{ overflowX: 'auto' }}>
          <table className="list-table">
            <thead>
              <tr>
                {section === 'categories' ? (
                  <>
                    <th>ID</th>
                    <th>Name</th>
                  </>
                ) : null}
                {section === 'subcategories' ? (
                  <>
                    <th>Name</th>
                    <th>Category</th>
                  </>
                ) : null}
                {section === 'portions' ? (
                  <>
                    <th>ID</th>
                    <th>Name</th>
                  </>
                ) : null}
                {section === 'products' ? (
                  <>
                    <th>Name</th>
                    <th>Price</th>
                    <th>Category</th>
                  </>
                ) : null}
                {section === 'combos' ? (
                  <>
                    <th>Name</th>
                    <th>Price</th>
                  </>
                ) : null}
                {section === 'tables' ? (
                  <>
                    <th>No.</th>
                    <th>Name</th>
                    <th>Seats</th>
                  </>
                ) : null}
              </tr>
            </thead>
            <tbody>
              {(listQ.data ?? []).map((row, i) => {
                const key =
                  str(
                    row,
                    'categoryId',
                    'subcategoryId',
                    'portionMasterId',
                    'productId',
                    'comboId',
                    'tableId',
                    'id',
                  ) || String(i)
                return (
                  <tr key={key}>
                    {section === 'categories' ? (
                      <>
                        <td>{str(row, 'categoryId')}</td>
                        <td>{str(row, 'categoryName')}</td>
                      </>
                    ) : null}
                    {section === 'subcategories' ? (
                      <>
                        <td>{str(row, 'subcategoryName')}</td>
                        <td>{str(row, 'categoryId', 'categoryName')}</td>
                      </>
                    ) : null}
                    {section === 'portions' ? (
                      <>
                        <td>{str(row, 'portionMasterId')}</td>
                        <td>{str(row, 'portionMasterName', 'portionName')}</td>
                      </>
                    ) : null}
                    {section === 'products' ? (
                      <>
                        <td>{str(row, 'productName')}</td>
                        <td>
                          <Money value={num(row, 'productPrice')} />
                        </td>
                        <td>{str(row, 'categoryName', 'categoryId')}</td>
                      </>
                    ) : null}
                    {section === 'combos' ? (
                      <>
                        <td>{str(row, 'comboName')}</td>
                        <td>
                          <Money value={num(row, 'comboPrice', 'price')} />
                        </td>
                      </>
                    ) : null}
                    {section === 'tables' ? (
                      <>
                        <td>{str(row, 'tableNumber')}</td>
                        <td>{str(row, 'tableName')}</td>
                        <td>{str(row, 'capacity')}</td>
                      </>
                    ) : null}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
