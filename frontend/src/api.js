import axios from 'axios'

//const BASE = ''  Vite proxy handles /api and /status → localhost:8080
const BASE = import.meta.env.VITE_API_URL || ''
// ── Status ──────────────────────────────────────────────────────────────────

export const getStatus = () =>
    axios.get(`${BASE}/status`).then(r => r.data)

// ── Demo vectors ─────────────────────────────────────────────────────────────

export const getDemoItems = () =>
    axios.get(`${BASE}/api/demo/items`).then(r => r.data)

export const searchDemo = ({ query, metric = 'cosine', k = 5 }) =>
    axios.post(`${BASE}/api/documents/search`, { query, metric, k }).then(r => r.data)

export const insertDemo = ({ metadata, category, embedding }) =>
    axios.post(`${BASE}/api/demo/insert`, { metadata, category, embedding }).then(r => r.data)

export const deleteDemo = (id) =>
    axios.delete(`${BASE}/api/demo/delete/${id}`).then(r => r.data)

// ── Documents ─────────────────────────────────────────────────────────────────

export const getDocuments = () =>
    axios.get(`${BASE}/api/documents`).then(r => r.data)

export const insertDocument = ({ title, text }) =>
    axios.post(`${BASE}/api/documents`, { title, text }).then(r => r.data)

export const deleteDocument = (id) =>
    axios.delete(`${BASE}/api/documents/${id}`).then(r => r.data)

// ── Documents (grouped view — Step 13) ───────────────────────────────────────

export const getGroupedDocuments = () =>
    axios.get(`${BASE}/api/documents/grouped`).then(r => r.data)

export const deleteDocumentGroup = (documentId) =>
    axios.delete(`${BASE}/api/documents/document/${documentId}`).then(r => r.data)

// ── RAG ───────────────────────────────────────────────────────────────────────

export const askRag = ({ question, k = 3 }) =>
    axios.post(`${BASE}/api/rag/ask`, { question, k }).then(r => r.data)

// PDF UPLOAD
export const uploadPdf = (file, title) => {
    const formData = new FormData()

    formData.append("file", file)
    formData.append("title", title)

    return axios.post(
        `${BASE}/api/documents/upload`,
        formData,
        {
            headers: {
                "Content-Type": "multipart/form-data"
            }
        }
    ).then(r => r.data)
}