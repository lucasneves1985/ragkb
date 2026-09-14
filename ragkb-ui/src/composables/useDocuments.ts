// ragkb-ui/src/composables/useDocuments.ts
import { computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { documentsService } from '@/services'
import { queryKeys } from './queryKeys'
import type { KnowledgeDocument } from '@/types'

// ── QUERY: listar documentos ──────────────────────────────
export function useDocuments() {
  const query = useQuery({
    queryKey: queryKeys.documents.lists(),
    queryFn: () => documentsService.list(),
  })

  return {
    documents: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  }
}

// ── MUTATION: upload (ingest) ─────────────────────────────
export function useUploadDocument() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (data: FormData) => documentsService.upload(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.lists() })
    },
  })

  return {
    uploadDocument: mutation.mutateAsync,
    uploading: mutation.isPending,
    error: mutation.error,
  }
}

// ── MUTATION: arquivar ────────────────────────────────────
export function useArchiveDocument() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => documentsService.changeStatus(id, { action: 'ARCHIVE' }),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.documents.lists() })
      const previous = queryClient.getQueryData<KnowledgeDocument[]>(
        queryKeys.documents.lists(),
      )

      if (previous) {
        queryClient.setQueryData<KnowledgeDocument[]>(
          queryKeys.documents.lists(),
          previous.map((d) =>
            d.id === id ? { ...d, status: 'ARCHIVED' } : d,
          ),
        )
      }
      return { previous }
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.documents.lists(), context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.lists() })
    },
  })

  return {
    archiveDocument: mutation.mutateAsync,
    updating: mutation.isPending,
  }
}

// ── MUTATION: reativar ────────────────────────────────────
export function useReactivateDocument() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => documentsService.changeStatus(id, { action: 'REACTIVATE' }),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.documents.lists() })
      const previous = queryClient.getQueryData<KnowledgeDocument[]>(
        queryKeys.documents.lists(),
      )

      if (previous) {
        queryClient.setQueryData<KnowledgeDocument[]>(
          queryKeys.documents.lists(),
          previous.map((d) =>
            d.id === id ? { ...d, status: 'ACTIVE' } : d,
          ),
        )
      }
      return { previous }
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.documents.lists(), context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.documents.lists() })
    },
  })

  return {
    reactivateDocument: mutation.mutateAsync,
    updating: mutation.isPending,
  }
}
