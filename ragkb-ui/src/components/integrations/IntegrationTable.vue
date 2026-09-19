<!-- components/integrations/IntegrationTable.vue -->
<script setup lang="ts">
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
  if (i.scheduleCron) return `cron: ${i.scheduleCron} (${i.scheduleTimezone ?? 'UTC'})`
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
    <el-table-column label="Nome" min-width="200">
      <template #default="{ row }">
        <b>{{ row.name }}</b>
        <div v-if="row.description" class="sub">{{ row.description }}</div>
      </template>
    </el-table-column>
    <el-table-column label="Tipo" width="130">
      <template #default="{ row }">
        <el-tag size="small" :type="row.integrationType === 'QUERY' ? 'warning' : 'primary'">
          {{ typeLabel(row.integrationType) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Agendamento" min-width="180">
      <template #default="{ row }">{{ scheduleLabel(row) }}</template>
    </el-table-column>
    <el-table-column label="Ação" min-width="180">
      <template #default="{ row }">{{ actionLabel(row) }}</template>
    </el-table-column>
    <el-table-column label="Status" width="110">
      <template #default="{ row }">
        <el-tag size="small" :type="row.active ? 'success' : 'info'">
          {{ row.active ? 'Ativa' : 'Inativa' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="270">
      <template #default="{ row }">
        <el-button text type="primary" @click="$emit('edit', row)">Editar</el-button>
        <el-button text type="primary" @click="$emit('executions', row)">Execuções</el-button>
        <el-button text :type="row.active ? 'warning' : 'success'" @click="$emit('toggle', row)">
          {{ row.active ? 'Desativar' : 'Ativar' }}
        </el-button>
        <el-button text type="danger" @click="$emit('delete', row)">Excluir</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.sub {
  color: #7a8699;
  font-size: 12px;
}
</style>
