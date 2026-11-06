// composables/useProjectSharing.ts
// Compartilhamento de projeto (V20) — mutations owner-only no backend.
// A lista de participantes NÃO precisa de query própria: o ProjectDto já
// traz participantUsernames em projects.lists — as mutations invalidam essa
// lista e a UI atualiza.
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { projectsService } from '@/services'
import { queryKeys } from './queryKeys'

export function useShareProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ projectId, username }: { projectId: string; username: string }) =>
      projectsService.share(projectId, { username }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.lists() })
    },
  })
}

export function useUnshareProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ projectId, userId }: { projectId: string; userId: number }) =>
      projectsService.unshare(projectId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.lists() })
    },
  })
}
