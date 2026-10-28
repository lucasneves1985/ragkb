import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { certificateMonitorsService } from '@/services'
import type { CertificateMonitorRequest } from '@/types'

export const CERTIFICATE_MONITORS_KEY = ['certificate-monitors']

export function useCertificateMonitors() {
  const client = useQueryClient()
  const query = useQuery({ queryKey: CERTIFICATE_MONITORS_KEY, queryFn: certificateMonitorsService.list })
  const refresh = () => client.invalidateQueries({ queryKey: CERTIFICATE_MONITORS_KEY })
  const createMutation = useMutation({ mutationFn: certificateMonitorsService.create, onSuccess: refresh })
  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: string; request: CertificateMonitorRequest }) =>
      certificateMonitorsService.update(id, request),
    onSuccess: refresh,
  })
  const deleteMutation = useMutation({ mutationFn: certificateMonitorsService.remove, onSuccess: refresh })
  const checkMutation = useMutation({
    mutationFn: certificateMonitorsService.check,
    onSuccess: () => client.invalidateQueries({ queryKey: ['certificate-history'] }),
  })

  return {
    monitors: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
    createMonitor: createMutation.mutateAsync,
    updateMonitor: updateMutation.mutateAsync,
    deleteMonitor: deleteMutation.mutateAsync,
    checkNow: checkMutation.mutateAsync,
    isSaving: computed(() => createMutation.isPending.value || updateMutation.isPending.value),
    isChecking: checkMutation.isPending,
  }
}

export function useCertificateMonitorHistory(id: MaybeRefOrGetter<string>, enabled: MaybeRefOrGetter<boolean>) {
  const monitorId = computed(() => toValue(id))
  const isEnabled = computed(() => toValue(enabled) && monitorId.value !== '')
  const query = useQuery({
    queryKey: computed(() => ['certificate-history', monitorId.value]),
    queryFn: () => certificateMonitorsService.history(monitorId.value),
    enabled: isEnabled,
  })
  return { history: query.data, isLoading: query.isLoading, isError: query.isError, refetch: query.refetch }
}
