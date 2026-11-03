<!-- components/integrations/IntegrationExecutionsDialog.vue -->
<script setup lang="ts">
import { computed, watch } from 'vue'
import { useIntegrationExecutions } from '@/composables'
import type { Integration, IntegrationExecutionStatus } from '@/types'

const props = defineProps<{
  modelValue: boolean
  integration: Integration | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// Id reativo: acompanha qual integração foi aberta no diálogo.
// Combinado com enabled no composable, o diálogo montado sem seleção
// não dispara request nenhuma.
const integrationId = computed(() => props.integration?.id ?? '')

const { executions, isLoadingExecutions, refetchExecutions } =
  useIntegrationExecutions(integrationId)

watch(
  () => props.modelValue,
  (open) => {
    if (open && integrationId.value) refetchExecutions()
  },
)

function statusType(s: IntegrationExecutionStatus) {
  return s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'danger' : 'info'
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="`Execuções — ${integration?.name ?? ''}`" width="860px" top="4vh"
    @update:model-value="emit('update:modelValue', $event)">
    <section v-if="isLoadingExecutions" class="loading">Carregando…</section>
    <el-table v-else :data="executions ?? []" style="width: 100%">
      <el-table-column label="Início" width="180" class-name="mobile-label-started">
        <template #default="{ row }">{{ new Date(row.startedAt).toLocaleString('pt-BR') }}</template>
      </el-table-column>
      <el-table-column label="Status" width="110" class-name="mobile-label-status">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Tent." width="90" prop="attempt" class-name="mobile-label-attempt" />
      <el-table-column label="HTTP" width="70" prop="httpStatus" class-name="mobile-label-http" />
      <el-table-column label="Resposta / Erro" min-width="300" class-name="mobile-label-response">
        <template #default="{ row }">
          <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
          <code v-else class="response-code">{{ (row.responseBody ?? '').slice(0, 120) }}</code>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<style scoped>
.loading {
  padding: 28px;
  text-align: center;
  color: #718096;
}

.response-code {
  font-size: 12px;
  word-break: break-all;
}

.error-text {
  color: #c0392b;
  font-size: 12px;
  word-break: break-all;
}
</style>
