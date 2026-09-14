import { ref } from 'vue'

import { ticketsService } from '@/services'
import type { TicketRequest } from '@/types'

export function useTickets() {
  const submitting = ref(false)
  const errorMessage = ref('')

  async function createTicket(request: TicketRequest): Promise<boolean> {
    submitting.value = true
    errorMessage.value = ''

    try {
      await ticketsService.create(request)
      return true
    } catch {
      errorMessage.value = 'Não foi possível abrir o chamado.'
      return false
    } finally {
      submitting.value = false
    }
  }

  function clearError(): void {
    errorMessage.value = ''
  }

  return {
    submitting,
    errorMessage,
    createTicket,
    clearError,
  }
}
