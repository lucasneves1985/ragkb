// composables/useWhatsApp.ts
import { ref, computed, type MaybeRefOrGetter } from 'vue'
import { toValue } from 'vue'
import { useQuery, useMutation } from '@tanstack/vue-query'
import { appConfigurationsService, whatsappService } from '@/services'
import type { WhatsAppTestSendRequest } from '@/types'

function extractMessage(error: unknown): string | undefined {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message
}

export function useWhatsAppStatus() {
  const query = useQuery({
    queryKey: ['whatsapp-status'],
    queryFn: whatsappService.status,
    // Sessão pode cair a qualquer momento — revalida a cada 30s
    refetchInterval: 30_000,
  })

  return {
    status: query.data,
    isLoadingStatus: query.isLoading,
    isErrorStatus: query.isError,
    refetchStatus: query.refetch,
  }
}

/**
 * QR de emparelhamento. Executa apenas quando "enabled" (sessão não conectada).
 * O QR da WAHA expira (60s o primeiro, 20s os subsequentes) — refetchInterval
 * renova a imagem enquanto o parâmetro de renovação for true.
 */
export function useWhatsAppQr(enabled: MaybeRefOrGetter<boolean>) {
  const shouldPoll = computed(() => toValue(enabled))

  const query = useQuery({
    queryKey: ['whatsapp-qr'],
    queryFn: whatsappService.qr,
    enabled: shouldPoll,
    refetchInterval: 25_000,
    retry: false,
  })

  return {
    qr: query.data,
    isLoadingQr: query.isLoading,
    isErrorQr: query.isError,
    refetchQr: query.refetch,
  }
}

export function useStartWhatsAppSession() {
  const errorMessage = ref('')

  const { mutateAsync, isPending: starting } = useMutation({
    mutationFn: () => whatsappService.startSession(),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível iniciar a sessão.'
    },
  })

  return { startSession: mutateAsync, starting, errorMessage }
}

export function useTestWhatsAppSend() {
  const errorMessage = ref('')

  const { mutateAsync, isPending: sending } = useMutation({
    mutationFn: (request: WhatsAppTestSendRequest) => whatsappService.testSend(request),
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Falha no envio de teste.'
    },
  })

  return { testSend: mutateAsync, sending, errorMessage }
}

export function useAppConfigurations() {
  const query = useQuery({
    queryKey: ['app-configurations'],
    queryFn: appConfigurationsService.list,
  })

  return {
    configurations: query.data,
    isLoadingConfigurations: query.isLoading,
    isErrorConfigurations: query.isError,
    refetchConfigurations: query.refetch,
  }
}

export function useSetAppConfiguration() {
  const errorMessage = ref('')

  const { mutateAsync, isPending: saving } = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      appConfigurationsService.set(key, value),
    onSuccess: () => { },
    onError: (error) => {
      errorMessage.value = extractMessage(error) ?? 'Não foi possível salvar a configuração.'
    },
  })

  return { setConfiguration: mutateAsync, saving, errorMessage }
}
