<script setup lang="ts">
import { Refresh, Clock, Edit, Delete } from '@element-plus/icons-vue'
import type { CertificateMonitor } from '@/types'
defineProps<{ monitors: CertificateMonitor[]; checking?: boolean }>()
defineEmits<{
  (e: 'check', monitor: CertificateMonitor): void
  (e: 'history', monitor: CertificateMonitor): void
  (e: 'edit', monitor: CertificateMonitor): void
  (e: 'delete', monitor: CertificateMonitor): void
}>()
function date(value: string | null) { return value ? new Date(value).toLocaleString('pt-BR') : ' — ' }
</script>

<template>
  <el-table :data="monitors" style="width: 100%" empty-text="Nenhuma URL monitorada ainda.">
    <el-table-column label="URL" min-width="220"><template #default="{ row }">{{ row.url }}</template></el-table-column>
    <el-table-column label="Verificação" min-width="100" class-name="col-optional"><template #default="{ row }">{{
      row.checkTime }}</template></el-table-column>
    <el-table-column label="Alerta antes" min-width="110" class-name="col-optional"><template #default="{ row }">{{
      row.alertThresholdDays }} dias</template></el-table-column>
    <el-table-column label="Canal" min-width="90" class-name="col-optional"><template #default="{ row }">{{
      row.notificationChannel === 'EMAIL' ? 'E-mail' : 'WhatsApp' }}</template></el-table-column>
    <el-table-column label="Última checagem" min-width="160"><template #default="{ row }">{{ date(row.lastCheckedAt)
        }}</template></el-table-column>
    <el-table-column label="Ações" width="180" align="center"><template #default="{ row }">
        <div class="row-actions">
          <el-tooltip content="Checar o certificado agora" placement="top">
            <el-button text circle :icon="Refresh" :loading="checking" title="Checar o certificado agora"
              aria-label="Checar o certificado agora" @click="$emit('check', row)" />
          </el-tooltip>
          <el-tooltip content="Ver histórico de checagens" placement="top">
            <el-button text circle :icon="Clock" title="Ver histórico de checagens"
              aria-label="Ver histórico de checagens" @click="$emit('history', row)" />
          </el-tooltip>
          <el-tooltip content="Editar monitor" placement="top">
            <el-button text circle :icon="Edit" title="Editar monitor" aria-label="Editar monitor"
              @click="$emit('edit', row)" />
          </el-tooltip>
          <el-tooltip content="Excluir monitor" placement="top">
            <el-button text circle type="danger" :icon="Delete" title="Excluir monitor" aria-label="Excluir monitor"
              @click="$emit('delete', row)" />
          </el-tooltip>
        </div>
      </template></el-table-column>
  </el-table>
</template>

<style scoped>
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

/* Responsividade: colunas secundárias saem em telas estreitas.
   As células do el-table vivem em subcomponentes, por isso :deep(). */
@media (max-width: 900px) {
  .el-table :deep(.col-optional) {
    display: none;
  }
}
</style>
