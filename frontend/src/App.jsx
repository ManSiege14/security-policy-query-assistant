import { useState, useEffect } from 'react'
import { getStatus } from './api'
import SearchPage from './pages/SearchPage'
import DocumentsPage from './pages/DocumentsPage'
import AskAIPage from './pages/AskAIPage'

const PAGES = [
  { id: 'search', label: 'Search' },
  { id: 'documents', label: 'Documents' },
  { id: 'ask', label: 'Ask AI' },
]

export default function App() {
  const [page, setPage] = useState('search')
  const [status, setStatus] = useState(null)

  useEffect(() => {
    async function fetchStatus() {
      try {
        const s = await getStatus()
        setStatus(s)
      } catch {
        setStatus(null)
      }
    }

    fetchStatus()

    const id = setInterval(fetchStatus, 15000)
    return () => clearInterval(id)
  }, [])

  const ollamaOnline = status?.ollamaAvailable ?? false

  return (
    <div className="app">
      <nav className="sidebar">
        <div className="logo">
          <h2>VECTORDB</h2>

          <div className="tagline">
            Semantic Search & RAG
          </div>
        </div>

        {PAGES.map((p) => (
          <button
            key={p.id}
            className={page === p.id ? 'active' : ''}
            onClick={() => setPage(p.id)}
          >
            {p.id === 'documents'
              ? `Documents (${status?.docCount ?? 0})`
              : p.label}
          </button>
        ))}

        <div className="status-bar">
          <div className="status-title">
            System Status
          </div>

          <div>
            <span className={ollamaOnline ? 'online' : 'offline'}>
              ●
            </span>{' '}
            Ollama {ollamaOnline ? 'Online' : 'Offline'}
          </div>

          <div className="status-stat">
            📄 Documents: {status?.docCount ?? 0}
          </div>

          <div className="status-stat">
            🔍 Demo Vectors: {status?.demoCount ?? 0}
          </div>
        </div>
      </nav>

      <main className="main">
        <div className="page-header">
          <h1>VectorDB</h1>

          <p>
            Local Semantic Search & Retrieval-Augmented Generation
          </p>
        </div>

        {page === 'search' && <SearchPage />}
        {page === 'documents' && <DocumentsPage />}
        {page === 'ask' && <AskAIPage />}
      </main>
    </div>
  )
}