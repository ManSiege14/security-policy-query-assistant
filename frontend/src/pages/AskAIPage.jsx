import { useState } from 'react'
import { askRag } from '../api'

export default function AskAIPage() {
  const [question, setQuestion] = useState('')
  const [k, setK]         = useState(3)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')
  const [result, setResult]     = useState(null)  // Maps to backend RagResponse: { answer, context, model }
  const [history, setHistory]   = useState([])

  async function handleAsk(e) {
    e.preventDefault()
    if (!question.trim()) return
    setLoading(true)
    setError('')
    setResult(null)

    try {
      const data = await askRag({ question, k: parseInt(k) })
      setResult(data)
      setHistory(prev => [{ question, data }, ...prev.slice(0, 9)])
      setQuestion('')
    } catch (e) {
      setError(e.response?.data?.message ?? 'Ask failed. Is Ollama running with documents loaded?')
    } finally {
      setLoading(false)
    }
  }

  function loadFromHistory(entry) {
    setResult(entry.data)
    setQuestion(entry.question)
    setError('')
  }

  return (
    <div>
      <h1>Ask AI</h1>

      <div className="card">
        <form onSubmit={handleAsk}>
          <label style={{ fontSize: 12, color: '#666' }}>
            Question (answers from your uploaded documents)
          </label>
          <textarea
            placeholder="What is dynamic programming?&#10;How does HNSW work?"
            value={question}
            onChange={e => setQuestion(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter' && e.ctrlKey) handleAsk(e)
            }}
          />

          <div className="row">
            <div style={{ width: 120 }}>
              <label style={{ fontSize: 12, color: '#666' }}>Context chunks (k)</label>
              <select value={k} onChange={e => setK(e.target.value)}>
                <option value={1}>1</option>
                <option value={2}>2</option>
                <option value={3}>3</option>
                <option value={5}>5</option>
              </select>
            </div>
            <div style={{ paddingTop: 20 }}>
              <button className="btn" type="submit" disabled={loading}>
                {loading ? 'Thinking...' : 'Ask'}
              </button>
            </div>
          </div>
        </form>

        {error && <p className="error">{error}</p>}
      </div>

      {/* Current answer & Sources (Step 9B) */}
      {result && (
        <div className="card">
          {/* Improvement 3: More professional model metadata info */}
          <div style={{ fontSize: 12, color: '#666', marginBottom: 8, lineHeight: 1.4 }}>
            Retrieved {result.context?.length ?? 0} chunks<br />
            Generated using {result.model ? result.model.charAt(0).toUpperCase() + result.model.slice(1) : 'Ollama'}
          </div>

          <h3>Answer</h3>
          <div
            style={{
              whiteSpace: 'pre-wrap',
              lineHeight: 1.6
            }}
          >
            {result.answer}
          </div>

          {/* Improvement 1: Dynamic source count heading */}
          {result.context?.length > 0 && (
            <>
              <hr style={{ margin: '20px 0' }} />
              <h4>Sources ({result.context.length})</h4>

              {result.context.map((doc, index) => (
                <div
                  key={doc.id ?? index}
                  style={{
                    padding: '12px',
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px',
                    marginBottom: '10px'
                  }}
                >
                  <strong>
                    [{index + 1}] {doc.title ?? 'Untitled Document'}
                  </strong>
                  
                  {/* Improvement 2: Limited source chunk preview length */}
                  <p
                    style={{
                      marginTop: '8px',
                      color: '#666'
                    }}
                  >
                    {doc.chunkText 
                      ? (doc.chunkText.length > 300 
                          ? doc.chunkText.substring(0, 300) + "..." 
                          : doc.chunkText)
                      : '(no text)'}
                  </p>
                </div>
              ))}
            </>
          )}
        </div>
      )}

      {/* History */}
      {history.length > 0 && (
        <div>
          <h3 style={{ fontSize: 14, marginBottom: 10 }}>
            Recent ({history.length})
          </h3>
          {history.map((entry, i) => (
            <div className="result-item" key={i}
              style={{ cursor: 'pointer' }}
              onClick={() => loadFromHistory(entry)}>
              <div className="label">Q</div>
              <div className="value">{entry.question}</div>
              <div className="label" style={{ marginTop: 4 }}>A</div>
              <div style={{ fontSize: 12, color: '#555' }}>
                {entry.data.answer?.slice(0, 120)}
                {entry.data.answer?.length > 120 ? '…' : ''}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}