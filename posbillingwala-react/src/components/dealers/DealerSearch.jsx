import { Search, ChevronDown } from 'lucide-react'
import './DealerSearch.css'

export default function DealerSearch({ query, onQueryChange, areas = [], area, onAreaChange }) {
  return (
    <div className="dealer-search">
      <label className="dealer-search__field">
        <span className="sr-only">Search dealers</span>
        <Search size={18} />
        <input
          type="search"
          placeholder="Search by area, name, phone…"
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
        />
      </label>
      {areas.length > 0 && (
        <label className="dealer-search__select">
          <span className="sr-only">Filter by area</span>
          <select value={area} onChange={(e) => onAreaChange(e.target.value)}>
            <option value="">All areas</option>
            {areas.map((a) => (
              <option key={a} value={a}>
                {a}
              </option>
            ))}
          </select>
          <span className="dealer-search__select-arrow" aria-hidden>
            <ChevronDown size={18} strokeWidth={2.4} />
          </span>
        </label>
      )}
    </div>
  )
}
