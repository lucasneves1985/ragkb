// composables/useReminders.ts
import { ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { remindersService } from '@/services'
import type { ReminderCreateRequest } from '@/types'

const REMINDERS_KEY = ['reminders']

function extractMessage(error: unknown): string | undefined {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message
}

export function useReminders() {
  const query = useQuery({
    queryKey: REMINDERS_KEY,
    queryFn: remindersService.list,
  })

  return {
    reminders: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}

export function useCreateReminder() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: creating } = useMutation({
    mutationFn: (request: ReminderCreateRequest) => remindersService.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: REMINDERS_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível criar o lembrete.'
    },
  })

  return { createReminder: mutateAsync, creating, errorMessage }
}

export function useCancelReminder() {
  const queryClient = useQueryClient()
  const errorMessage = ref('')

  const { mutateAsync, isPending: cancelling } = useMutation({
    mutationFn: (id: string) => remindersService.cancel(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: REMINDERS_KEY }),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível cancelar o lembrete.'
    },
  })

  return { cancelReminder: mutateAsync, cancelling, errorMessage }
}
