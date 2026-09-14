import { ref } from 'vue'
import { documentsService } from '@/services'
import type { DocumentActionRequest, IngestDocumentRequest, KnowledgeDocument } from '@/types'

export function useDocuments() {
  const documents = ref<KnowledgeDocument[]>([])
  const loading = ref(false)
  const uploading = ref(false)
  const updating = ref(false)
  const errorMessage = ref('')

  async function loadDocuments(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    try {
      documents.value = await documentsService.list()
    } catch {
      errorMessage.value = 'Não foi possível carregar os documentos.'
    } finally {
      loading.value = false
    }
  }

  async function uploadDocument(data: FormData): Promise<KnowledgeDocument | null> {
    uploading.value = true
    errorMessage.value = ''

    try {
      const document = await documentsService.upload(data)
      await loadDocuments()
      return document
    } catch {
      errorMessage.value = 'Erro ao processar o documento. Tente novamente.'
      return null
    } finally {
      uploading.value = false
    }
  }

  async function ingestDocument(
    file: File,
    request: IngestDocumentRequest,
  ): Promise<KnowledgeDocument | null> {
    uploading.value = true
    errorMessage.value = ''

    try {
      const document = await documentsService.ingest(file, request)
      await loadDocuments()
      return document
    } catch {
      errorMessage.value = 'Não foi possível importar o documento.'
      return null
    } finally {
      uploading.value = false
    }
  }

  async function changeStatus(
    id: string,
    request: DocumentActionRequest,
  ): Promise<KnowledgeDocument | null> {
    updating.value = true
    errorMessage.value = ''

    try {
      const updatedDocument = await documentsService.changeStatus(id, request)
      await loadDocuments()
      return updatedDocument
    } catch {
      errorMessage.value = 'Não foi possível alterar o status do documento.'
      return null
    } finally {
      updating.value = false
    }
  }

  async function archiveDocument(id: string): Promise<KnowledgeDocument | null> {
    return changeStatus(id, { action: 'ARCHIVE' })
  }

  async function reactivateDocument(id: string): Promise<KnowledgeDocument | null> {
    return changeStatus(id, { action: 'REACTIVATE' })
  }

  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    documents,
    loading,
    uploading,
    updating,
    errorMessage,
    loadDocuments,
    uploadDocument,
    ingestDocument,
    changeStatus,
    archiveDocument,
    reactivateDocument,
    clearError,
  }
}
