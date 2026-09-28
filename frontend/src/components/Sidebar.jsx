const NAV_ITEMS = [
  { id: 'search', label: 'Search' },
  { id: 'documents', label: 'Documents' },
  { id: 'ask', label: 'Ask AI' },
]

const styles = {
  sidebar: {
    width: '220px',
    minHeight: '100vh',
    borderRight: '1px solid #ddd',
    padding: '24px 0',
    backgroundColor: '#fafafa',
    display: 'flex',
    flexDirection: 'column',
  },

  title: {
    padding: '0 20px 20px',
    margin: 0,
    fontSize: '18px',
    fontWeight: '700',
    borderBottom: '1px solid #ddd',
    marginBottom: '12px',
  },

  subtitle: {
    padding: '0 20px',
    marginTop: '-8px',
    marginBottom: '16px',
    fontSize: '11px',
    color: '#888',
    letterSpacing: '1px',
    textTransform: 'uppercase',
  },

  navButton: (isActive) => ({
    display: 'block',
    width: '100%',
    padding: '10px 20px',
    textAlign: 'left',
    border: 'none',
    background: isActive ? '#e8e8e8' : 'transparent',
    fontWeight: isActive ? '600' : '400',
    fontSize: '14px',
    cursor: 'pointer',
    color: '#111',
  }),

  spacer: {
    flex: 1,
  },

  statusCard: {
    margin: '16px',
    padding: '12px',
    border: '1px solid #ddd',
    borderRadius: '8px',
    background: '#fff',
  },

  statusTitle: {
    fontSize: '11px',
    color: '#666',
    textTransform: 'uppercase',
    marginBottom: '10px',
    fontWeight: '600',
  },

  statusRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '8px',
    fontSize: '13px',
  },

  dot: (online) => ({
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background: online ? '#22c55e' : '#ef4444',
  }),
}

export default function Sidebar({
  activePage,
  onNavigate,
  documentCount = 0,
  ollamaOnline = true,
}) {
  return (
    <nav style={styles.sidebar}>
      <p style={styles.title}>VectorDB</p>

      <div style={styles.subtitle}>
        Semantic Search & RAG
      </div>

      {NAV_ITEMS.map((item) => (
        <button
          key={item.id}
          style={styles.navButton(activePage === item.id)}
          onClick={() => onNavigate(item.id)}
        >
          {item.id === 'documents'
            ? `Documents (${documentCount})`
            : item.label}
        </button>
      ))}

      <div style={styles.spacer} />

      <div style={styles.statusCard}>
        <div style={styles.statusTitle}>
          System Status
        </div>

        <div style={styles.statusRow}>
          <span style={styles.dot(ollamaOnline)} />
          <span>
            Ollama {ollamaOnline ? 'Online' : 'Offline'}
          </span>
        </div>

        <div style={styles.statusRow}>
          <span>📄</span>
          <span>{documentCount} Documents</span>
        </div>
      </div>
    </nav>
  )
}
