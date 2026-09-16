import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { articlesService } from '@/services'
import { queryKeys } from './queryKeys'
import type { Article, CreateArticleRequest, UpdateArticleRequest } from '@/types'

// ── QUERY: listar artigos ─────────────────────────────────
export function useArticles() {
  const query = useQuery({
    queryKey: queryKeys.articles.lists(),
    queryFn: () => articlesService.list(),
  })

  return {
    articles: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
    refetch: query.refetch,
  }
}

// ── MUTATION: criar artigo ────────────────────────────────
export function useCreateArticle() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (request: CreateArticleRequest) => articlesService.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.articles.lists() })
    },
  })

  return {
    createArticle: mutation.mutateAsync,
    creating: mutation.isPending,
    error: mutation.error,
  }
}

// ── MUTATION: atualizar artigo ────────────────────────────
export function useUpdateArticle() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateArticleRequest }) =>
      articlesService.update(id, request),
    onSuccess: () => {
      // Re-ingestão no backend muda chunkCount/version — sempre invalidar
      queryClient.invalidateQueries({ queryKey: queryKeys.articles.lists() })
    },
  })

  return {
    updateArticle: mutation.mutateAsync,
    updating: mutation.isPending,
    error: mutation.error,
  }
}

// ── MUTATION: publicar ────────────────────────────────────
export function usePublishArticle() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => articlesService.changeStatus(id, { action: 'PUBLISH' }),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.articles.lists() })
      const previous = queryClient.getQueryData<Article[]>(
        queryKeys.articles.lists(),
      )

      if (previous) {
        queryClient.setQueryData<Article[]>(
          queryKeys.articles.lists(),
          previous.map((a) =>
            a.id === id
              ? { ...a, status: 'PUBLISHED' as const, publishedAt: new Date().toISOString() }
              : a,
          ),
        )
      }
      return { previous }
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.articles.lists(), context.previous)
      }
    },
    onSettled: () => {
      // Invalidate obrigatório: publish dispara ingestão e muda chunkCount
      queryClient.invalidateQueries({ queryKey: queryKeys.articles.lists() })
    },
  })

  return {
    publishArticle: mutation.mutateAsync,
    updating: mutation.isPending,
  }
}

// ── MUTATION: arquivar ────────────────────────────────────
export function useArchiveArticle() {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (id: string) => articlesService.changeStatus(id, { action: 'ARCHIVE' }),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.articles.lists() })
      const previous = queryClient.getQueryData<Article[]>(
        queryKeys.articles.lists(),
      )

      if (previous) {
        queryClient.setQueryData<Article[]>(
          queryKeys.articles.lists(),
          previous.map((a) =>
            a.id === id ? { ...a, status: 'ARCHIVED' as const } : a,
          ),
        )
      }
      return { previous }
    },
    onError: (_err, _id, context) => {
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.articles.lists(), context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.articles.lists() })
    },
  })

  return {
    archiveArticle: mutation.mutateAsync,
    updating: mutation.isPending,
  }
}

// ── MUTATION: upload de imagem ────────────────────────────
export function useUploadArticleImage() {
  const mutation = useMutation({
    mutationFn: (file: File) => articlesService.uploadImage(file),
  })

  return {
    uploadArticleImage: mutation.mutateAsync,
    uploading: mutation.isPending,
    error: mutation.error,
  }
}
