// composables/useCards.ts
import { computed, type Ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { cardsService, projectsService } from '@/services'
import { queryKeys } from './queryKeys'
import type {
  CreateCardRequest,
  CreateCommentRequest,
  CreateProjectRequest,
  MoveCardRequest,
  UpdateCardRequest,
  UpdateCommentRequest,
} from '@/types'

function extractMessage(error: unknown): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
  return message ?? 'Falha na operação. Tente novamente.'
}

// ── Projetos ──────────────────────────────────────────────

export function useProjects() {
  return useQuery({
    queryKey: queryKeys.projects.lists(),
    queryFn: projectsService.list,
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CreateProjectRequest) => projectsService.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.projects.all }),
  })
}

export function useUpdateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: CreateProjectRequest }) =>
      projectsService.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.projects.all }),
  })
}

export function useDeleteProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => projectsService.delete(id),
    onSuccess: () => {
      // Cards somem por cascade no backend — invalida os dois domínios.
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.all })
      queryClient.invalidateQueries({ queryKey: queryKeys.cards.all })
    },
  })
}

// ── Board / cards ─────────────────────────────────────────

export function useBoard(projectId: Ref<string | undefined>, includeClosed: Ref<boolean>) {
  return useQuery({
    queryKey: computed(() => queryKeys.cards.board(projectId.value ?? '', includeClosed.value)),
    queryFn: () => cardsService.board(projectId.value!, includeClosed.value),
    enabled: computed(() => !!projectId.value),
  })
}

export function useCardDetail(cardId: Ref<string | null>) {
  return useQuery({
    queryKey: computed(() => queryKeys.cards.detail(cardId.value ?? '')),
    queryFn: () => cardsService.detail(cardId.value!),
    enabled: computed(() => !!cardId.value),
  })
}

export function useCreateCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ projectId, request }: { projectId: string; request: CreateCardRequest }) =>
      cardsService.create(projectId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useUpdateCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateCardRequest }) =>
      cardsService.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useMoveCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: MoveCardRequest }) =>
      cardsService.move(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useDeleteCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => cardsService.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

// ── Comentários ───────────────────────────────────────────

export function useAddComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, request }: { cardId: string; request: CreateCommentRequest }) =>
      cardsService.addComment(cardId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useUpdateComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ commentId, request }: { commentId: string; request: UpdateCommentRequest }) =>
      cardsService.updateComment(commentId, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useDeleteComment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (commentId: string) => cardsService.deleteComment(commentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

// ── Anexos ────────────────────────────────────────────────

export function useUploadAttachment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, file }: { cardId: string; file: File }) =>
      cardsService.uploadAttachment(cardId, file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

export function useDeleteAttachment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, attachmentId }: { cardId: string; attachmentId: string }) =>
      cardsService.deleteAttachment(cardId, attachmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cards.all }),
  })
}

// ── Assistente de tarefas ─────────────────────────────────

export function useAskCards() {
  return useMutation({
    mutationFn: (request: { question: string; projectId?: string }) =>
      cardsService.askCards(request),
  })
}

export { extractMessage as extractCardsErrorMessage }
