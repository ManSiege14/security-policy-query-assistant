import { useState, useEffect } from 'react'
import { getGroupedDocuments, deleteDocumentGroup, insertDocument, uploadPdf } from '../api'

export default function DocumentsPage() {
  const [docs, setDocs]       = useState([])
  const [expanded, setExpanded] = useState(new Set())
  const [activeTab, setActiveTab] = useState('text') // 'text' or 'pdf'
  const [title, setTitle]     = useState('')
  const [text, setText]       = useState('')
  const [file, setFile]       = useState(null)
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(false)
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    loadGroupedDocs()
  }, [])

  async function loadGroupedDocs() {
    setFetching(true)
    try {
      const data = await getGroupedDocuments()
      setDocs(Array.isArray(data) ? data : [])
    } catch {
      setError('Failed to load documents')
    } finally {
      setFetching(false)
    }
  }

  function toggleExpand(documentId) {
    setExpanded(prev => {
      const next = new Set(prev)
      if (next.has(documentId)) {
        next.delete(documentId)
      } else {
        next.add(documentId)
      }
      return next
    })
  }

  // Handle Text Submission
  async function handleInsert(e) {
    e.preventDefault()
    if (!title.trim() || !text.trim()) {
      setError('Title and text are required')
      return
    }
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      const result = await insertDocument({ title, text })
      const chunkCount = result.chunks ?? result.chunkCount ?? '?'
      setSuccess(`Inserted ${chunkCount} chunk(s) successfully.`)
      setTitle('')
      setText('')
      await loadGroupedDocs()
    } catch (e) {
      setError(e.response?.data?.message ?? 'Insert failed. Is Ollama running?')
    } finally {
      setLoading(false)
    }
  }

  // Handle PDF Upload
  async function handleUpload(e) {
    e.preventDefault()
    if (!file) {
      setError('Please select a PDF file')
      return
    }
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      await uploadPdf(file, title || file.name)
      setSuccess('PDF uploaded, processed, and embedded successfully!')
      setFile(null)
      setTitle('')
      await loadGroupedDocs()
    } catch (e) {
      setError(e.response?.data?.message ?? 'PDF upload and embedding failed.')
    } finally {
      setLoading(false)
    }
  }

  async function handleDeleteDocument(documentId) {
    try {
      await deleteDocumentGroup(documentId)
      setDocs(prev => prev.filter(d => d.documentId !== documentId))
      setExpanded(prev => {
        const next = new Set(prev)
        next.delete(documentId)
        return next
      })
    } catch {
      setError('Delete failed')
    }
  }

  return (
      <div>
        <h1>Documents</h1>

        {/* Unified Input Card */}
        <div className="card" style={{ marginBottom: 24 }}>
          {/* Tab Switchers */}
          <div style={{ display: 'flex', gap: '16px', borderBottom: '1px solid #eee', marginBottom: 16, paddingBottom: 8 }}>
            <button
                type="button"
                onClick={() => { setActiveTab('text'); setError(''); setSuccess(''); }}
                style={{
                  background: 'none', border: 'none', padding: '4px 8px', cursor: 'pointer', fontSize: 14,
                  fontWeight: activeTab === 'text' ? 'bold' : 'normal',
                  borderBottom: activeTab === 'text' ? '2px solid #333' : 'none',
                  color: activeTab === 'text' ? '#000' : '#666'
                }}
            >
              Add Raw Text
            </button>
            <button
                type="button"
                onClick={() => { setActiveTab('pdf'); setError(''); setSuccess(''); }}
                style={{
                  background: 'none', border: 'none', padding: '4px 8px', cursor: 'pointer', fontSize: 14,
                  fontWeight: activeTab === 'pdf' ? 'bold' : 'normal',
                  borderBottom: activeTab === 'pdf' ? '2px solid #333' : 'none',
                  color: activeTab === 'pdf' ? '#000' : '#666'
                }}
            >
              Upload PDF
            </button>
          </div>

          {/* Dynamic Form Content */}
          <form onSubmit={activeTab === 'text' ? handleInsert : handleUpload}>
            <label style={{ fontSize: 12, color: '#666' }}>
              {activeTab === 'text' ? 'Title' : 'Document Title (Optional)'}
            </label>
            <input
                type="text"
                placeholder={activeTab === 'text' ? "e.g. Operating Systems Notes" : "Defaults to file name"}
                value={title}
                onChange={e => setTitle(e.target.value)}
            />

            {activeTab === 'text' ? (
                <>
                  <label style={{ fontSize: 12, color: '#666' }}>Text Content</label>
                  <textarea
                      placeholder="Paste any text — lecture notes, articles, documentation..."
                      style={{ minHeight: 120 }}
                      value={text}
                      onChange={e => setText(e.target.value)}
                  />
                  <p className="info" style={{ marginBottom: 8 }}>
                    Text is automatically chunked (250 words, 30 overlap) and embedded via Ollama.
                  </p>
                </>
            ) : (
                <>
                  <label style={{ fontSize: 12, color: '#666', display: 'block', marginBottom: 6 }}>Select PDF File</label>

                  {/* Custom Elegant File Picker */}
                  <label
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        padding: '24px',
                        border: file ? '2px dashed #2e7d32' : '2px dashed #ccc',
                        borderRadius: '6px',
                        backgroundColor: file ? '#f1f8e9' : '#fafafa',
                        cursor: 'pointer',
                        textAlign: 'center',
                        marginBottom: '16px',
                        transition: 'all 0.2s ease-in-out'
                      }}
                  >
                    <span style={{ fontSize: '20px', marginBottom: '4px' }}>{file ? '📄' : '📁'}</span>
                    <span style={{ fontSize: '13px', color: file ? '#2e7d32' : '#333', fontWeight: file ? 'bold' : 'normal' }}>
                  {file ? file.name : 'Click to browse or drop your PDF here'}
                </span>
                    {file && (
                        <span style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>
                    ({(file.size / 1024 / 1024).toFixed(2)} MB)
                  </span>
                    )}
                    {/* Hidden original input */}
                    <input
                        type="file"
                        accept=".pdf"
                        onChange={(e) => setFile(e.target.files[0] || null)}
                        style={{ display: 'none' }}
                    />
                  </label>
                </>
            )}

            <button className="btn" type="submit" disabled={loading || (activeTab === 'pdf' && !file)}>
              {loading
                  ? (activeTab === 'text' ? 'Embedding Text...' : 'Processing PDF...')
                  : (activeTab === 'text' ? 'Embed & Insert' : 'Upload & Embed PDF')
              }
            </button>
          </form>

          {error   && <p className="error" style={{ marginTop: 10 }}>{error}</p>}
          {success && <p className="success" style={{ marginTop: 10 }}>{success}</p>}
        </div>

        {/* Document list (grouped) */}
        <div>
          <h3 style={{ marginBottom: 10, fontSize: 14 }}>
            Stored Documents ({docs.length})
            {' '}
            <button className="btn" style={{ fontSize: 11, padding: '2px 8px' }} onClick={loadGroupedDocs}>
              Refresh
            </button>
          </h3>

          {fetching && <p className="loading">Loading...</p>}

          {!fetching && docs.length === 0 && (
              <p className="info">No documents yet. Add one above.</p>
          )}

          {docs.map(doc => {
            const isExpanded = expanded.has(doc.documentId)

            return (
                <div className="result-item" key={doc.documentId}>
                  <div
                      style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}
                      onClick={() => toggleExpand(doc.documentId)}
                  >
                    <div className="value" style={{ fontWeight: 'bold' }}>
                      📄 {doc.title ?? `Document ${doc.documentId}`}
                    </div>
                    <span style={{ fontSize: 12, color: '#666' }}>
                  {isExpanded ? '▲ Collapse' : '▼ Expand'}
                </span>
                  </div>

                  <div className="meta">
                    <span>{doc.totalChunks} chunk{doc.totalChunks === 1 ? '' : 's'}</span>
                    <span style={{ fontSize: 11, color: '#999' }}>id: {doc.documentId}</span>
                    <button className="btn btn-danger"
                            style={{ padding: '2px 8px', fontSize: 11 }}
                            onClick={(e) => { e.stopPropagation(); handleDeleteDocument(doc.documentId); }}>
                      Delete Document
                    </button>
                  </div>

                  {isExpanded && (
                      <div style={{ marginTop: 10, paddingLeft: 12, borderLeft: '2px solid #eee' }}>
                        {doc.chunks.map(chunk => (
                            <div key={chunk.id} style={{ marginBottom: 10 }}>
                              <div style={{ fontSize: 12, fontWeight: 'bold', color: '#333' }}>
                                Chunk {chunk.chunkIndex + 1}
                              </div>
                              <div style={{ fontSize: 12, color: '#555', margin: '2px 0' }}>
                                {chunk.preview}
                              </div>
                              <div className="meta">
                                <span>id: {chunk.id}</span>
                                <span>{chunk.wordCount} words</span>
                              </div>
                            </div>
                        ))}
                      </div>
                  )}
                </div>
            )
          })}
        </div>
      </div>
  )
}