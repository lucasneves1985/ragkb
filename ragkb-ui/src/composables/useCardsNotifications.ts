// composables/useCardsNotifications.ts
import { ref } from 'vue'
import { cardsNotificationsService } from '@/services'
import type { NotificationTestResult } from '@/services/cardsNotifications.service'

/**
 * Teste "Checar Agora" das notificações de cards. Uma instância por seção da
 * tela de configurações (mesmo padrão dos dois useSetAppConfiguration): o
 * loading e o resultado de um teste não contaminam o outro.
 */
export function useNotificationTest(kind: 'reminder' | 'digest') {
  const testing = ref(false)
  const result = ref<NotificationTestResult | null>(null)
  const errorMessage = ref('')

  async function runTest() {
    testing.value = true
    errorMessage.value = ''
    result.value = null
    try {
      result.value =
        kind === 'reminder'
          ? await cardsNotificationsService.testReminder()
          : await cardsNotificationsService.testDigest()
    } catch (error) {
      const responseMessage = (error as { response?: { data?: { message?: string } } }).response
        ?.data?.message
      errorMessage.value =
        responseMessage || 'Não foi possível executar o teste. Verifique a sessão do WhatsApp.'
    } finally {
      testing.value = false
    }
  }

  return { testing, result, errorMessage, runTest }
}
