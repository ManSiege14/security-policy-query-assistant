import { useState, useEffect } from 'react'
import { searchDemo, getDemoItems, deleteDemo } from '../api'

export default function SearchPage() {
  const [query, setQuery]     = useState('')
  const [metric, setMetric]   = useState('cosine')
  const [k, setK]             = useState(5)
  const [results, setResults] = useState([])
  const [items, setItems]     = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [tab, setTab]         = useState('search') // 'search' | 'browse'

  useEffect(() => {
    if (tab === 'browse') loadItems()
  }, [tab])

  async function loadItems() {
    try {
      const data = await getDemoItems()
      setItems(data)
    } catch (e) {
      setError('Failed to load items')
    }
  }

  async function handleSearch(e) {
    e.preventDefault()
    if (!query.trim()) return
    setLoading(true)
    setError('')
    try {
      const data = await searchDemo({ query, metric, k: parseInt(k) })
      // Spring Boot returns { results: [...] } or an array directly
      setResults(Array.isArray(data) ? data : (data.results ?? []))
    } catch (e) {
      setError(e.response?.data?.message ?? 'Search failed')
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(id) {
    try {
      await deleteDemo(id)
      setResults(prev => prev.filter(r => r.id !== id))
      setItems(prev => prev.filter(r => r.id !== id))
    } catch {
      setError('Delete failed')
    }
  }

  return (
    <div>
      <h1>Search</h1>

      {/* Tab toggle */}
      <div style={{ marginBottom: 16 }}>
        <button className={`btn ${tab === 'search' ? '' : 'btn-secondary'}`}
          style={{ marginRight: 8 }} onClick={() => setTab('search')}>
          Search
        </button>
        <button className="btn" onClick={() => setTab('browse')}>
          Browse All
        </button>
      </div>

      {tab === 'search' && (
        <div className="card">
          <form onSubmit={handleSearch}>
            <label style={{ fontSize: 12, color: '#666' }}>Query text</label>
            <input
              type="text"
              placeholder="e.g. binary tree, pizza, basketball..."
              value={query}
              onChange={e => setQuery(e.target.value)}
            />

            <div className="row">
              <div style={{ flex: 1 }}>
                <label style={{ fontSize: 12, color: '#666' }}>Metric</label>
                <select value={metric} onChange={e => setMetric(e.target.value)}>
                  <option value="cosine">Cosine</option>
                  <option value="euclidean">Euclidean</option>
                  <option value="manhattan">Manhattan</option>
                </select>
              </div>
              <div style={{ width: 80 }}>
                <label style={{ fontSize: 12, color: '#666' }}>Top-K</label>
                <input
                  type="number"
                  min={1} max={20}
                  value={k}
                  onChange={e => setK(e.target.value)}
                />
              </div>
            </div>

            <button className="btn" type="submit" disabled={loading}>
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>

          {error && <p className="error">{error}</p>}
        </div>
      )}

      {tab === 'search' && results.length > 0 && (
  <div>
    <h3 style={{ marginBottom: 12 }}>
      Results ({results.length})
    </h3>

    {results.map((r, i) => (
      <div
        key={r.id ?? i}
        className="card"
        style={{ marginBottom: 12 }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginBottom: 8
          }}
        >
          <strong>{r.title}</strong>

          <span
            style={{
              fontSize: 12,
              color: '#888'
            }}
          >
            #{i + 1}
          </span>
        </div>

        <p
          style={{
            margin: '8px 0',
            lineHeight: 1.6
          }}
        >
          {r.chunkText}
        </p>

        <div
          style={{
            display: 'flex',
            gap: 12,
            fontSize: 12,
            color: '#888'
          }}
        >
          <span>ID: {r.id}</span>
          <span>Chunk: {r.chunkIndex}</span>
        </div>
      </div>
    ))}
  </div>
)}

      {tab === 'browse' && (
        <div>
          <h3 style={{ marginBottom: 10, fontSize: 14 }}>
            All items ({items.length})
          </h3>
          {items.length === 0 && <p className="info">No items found.</p>}
          {items.map(item => (
            <div className="result-item" key={item.id}>
              <>
  <strong>{item.title}</strong>

  <p style={{ marginTop: 8 }}>
    {item.preview ?? item.chunkText}
  </p>
</>
              <div className="meta">
                <span>id: {item.id}</span>
                {item.category && <span>category: {item.category}</span>}
                <button className="btn btn-danger"
                  style={{ padding: '2px 8px', fontSize: 11 }}
                  onClick={() => handleDelete(item.id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}