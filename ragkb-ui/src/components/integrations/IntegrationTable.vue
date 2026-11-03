<!-- components/integrations/IntegrationTable.vue -->
<script setup lang="ts">
import { Edit, List, SwitchButton, Delete } from '@element-plus/icons-vue'
import type { Integration } from '@/types'

defineProps<{
  integrations: Integration[]
}>()

defineEmits<{
  (e: 'edit', integration: Integration): void
  (e: 'executions', integration: Integration): void
  (e: 'toggle', integration: Integration): void
  (e: 'delete', integration: Integration): void
}>()

function typeLabel(t: Integration['integrationType']) {
  return t === 'SCHEDULED' ? 'Agendada' : 'Consulta (LLM)'
}

function scheduleLabel(i: Integration): string {
  if (i.integrationType !== 'SCHEDULED') return '—'
  if (i.scheduleCron) return `cron: ${i.scheduleCron}\n(${i.scheduleTimezone ?? 'UTC'})`
  if (i.scheduleIntervalSeconds) return `a cada ${i.scheduleIntervalSeconds}s`
  return '—'
}

function actionLabel(i: Integration): string {
  if (i.actionType === 'EMAIL') return `E-mail: ${i.actionTarget ?? ''}`
  if (i.actionType === 'WHATSAPP') return `WhatsApp: ${i.actionTarget ?? ''}`
  return 'Nenhuma'
}
</script>

<template>
  <el-table :data="integrations" style="width: 100%">
    <el-table-column label="Nome" min-width="200" class-name="mobile-label-name">
      <template #default="{ row }">
        <b>{{ row.name }}</b>
        <div v-if="row.description" class="sub">{{ row.description }}</div>
      </template>
    </el-table-column>
    <el-table-column label="Tipo" min-width="110" class-name="mobile-label-type">
      <template #default="{ row }">
        <el-tag size="small" :type="row.integrationType === 'QUERY' ? 'warning' : 'primary'">
          {{ typeLabel(row.integrationType) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Agendamento" min-width="180" class-name="mobile-label-schedule">
      <template #default="{ row }">{{ scheduleLabel(row) }}</template>
    </el-table-column>
    <el-table-column label="Ação" min-width="180" class-name="mobile-label-action">
      <template #default="{ row }">{{ actionLabel(row) }}</template>
    </el-table-column>
    <el-table-column label="Status" min-width="90" class-name="mobile-label-status">
      <template #default="{ row }">
        <el-tag size="small" :type="row.active ? 'success' : 'info'">
          {{ row.active ? 'Ativa' : 'Inativa' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="180" align="center" class-name="mobile-label-actions">
      <template #default="{ row }">
        <div class="row-actions">
          <el-tooltip content="Editar integração" placement="top">
            <el-button text circle type="primary" class="table-action-neutral" :icon="Edit" title="Editar integração" aria-label="Editar integração"
              @click="$emit('edit', row)" />
          </el-tooltip>
          <el-tooltip content="Ver histórico de execuções" placement="top">
            <el-button text circle type="primary" :icon="List" title="Ver histórico de execuções"
              aria-label="Ver histórico de execuções" @click="$emit('executions', row)" />
          </el-tooltip>
          <el-tooltip :content="row.active ? 'Desativar integração' : 'Ativar integração'" placement="top">
            <el-button text circle :type="row.active ? 'warning' : 'success'" :icon="SwitchButton"
              :title="row.active ? 'Desativar integração' : 'Ativar integração'"
              :aria-label="row.active ? 'Desativar integração' : 'Ativar integração'" @click="$emit('toggle', row)" />
          </el-tooltip>
          <el-tooltip content="Excluir integração" placement="top">
            <el-button text circle type="danger" :icon="Delete" title="Excluir integração"
              aria-label="Excluir integração" @click="$emit('delete', row)" />
          </el-tooltip>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.sub {
  color: #7a8699;
  font-size: 12px;
}

.row-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

/* O Element Plus aplica margin-left de 12px entre botões adjacentes;
   aqui o espaçamento é controlado pelo gap do container. */
.row-actions .el-button+.el-button {
  margin-left: 0;
}

/* Ícones levemente maiores que o padrão (14px) do Element Plus. */
.row-actions .el-button .el-icon {
  font-size: 18px;
}

</style>
