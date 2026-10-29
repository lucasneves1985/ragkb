<script setup lang="ts">
import type { CertificateMonitor } from '@/types'
defineProps<{ monitors: CertificateMonitor[]; checking?: boolean }>()
defineEmits<{
  (e: 'check', monitor: CertificateMonitor): void
  (e: 'history', monitor: CertificateMonitor): void
  (e: 'edit', monitor: CertificateMonitor): void
  (e: 'delete', monitor: CertificateMonitor): void
}>()
function date(value: string | null) { return value ? new Date(value).toLocaleString('pt-BR') : '—' }
</script>
<template>
  <el-table :data="monitors" style="width: 100%" empty-text="Nenhuma URL monitorada ainda.">
    <el-table-column label="URL" min-width="220"><template #default="{ row }">{{ row.url }}</template></el-table-column>
    <el-table-column label="Verificação" width="115"><template #default="{ row }">{{ row.checkTime }}</template></el-table-column>
    <el-table-column label="Alerta antes" width="130"><template #default="{ row }">{{ row.alertThresholdDays }} dias</template></el-table-column>
    <el-table-column label="Canal" width="110"><template #default="{ row }">{{ row.notificationChannel === 'EMAIL' ? 'E-mail' : 'WhatsApp' }}</template></el-table-column>
    <el-table-column label="Última checagem" min-width="160"><template #default="{ row }">{{ date(row.lastCheckedAt) }}</template></el-table-column>
    <el-table-column label="Ações" width="250"><template #default="{ row }">
      <el-button link :loading="checking" @click="$emit('check', row)">Checar agora</el-button>
      <el-button link @click="$emit('history', row)">Histórico</el-button>
      <el-button link @click="$emit('edit', row)">Editar</el-button>
      <el-button link type="danger" @click="$emit('delete', row)">Excluir</el-button>
    </template></el-table-column>
  </el-table>
</template>
