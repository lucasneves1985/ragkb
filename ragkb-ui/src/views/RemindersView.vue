<!-- views/RemindersView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCancelReminder, useCreateReminder, useReminders } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import type { ReminderStatus } from '@/types'

const { reminders, isLoading, isError } = useReminders()
const { createReminder, creating, errorMessage } = useCreateReminder()
const { cancelReminder, cancelling } = useCancelReminder()

const request = ref('')

async function handleCreate() {
  if (!request.value.trim()) return
  await createReminder({ request: request.value.trim() })
  if (!errorMessage.value) {
    ElMessage.success('Lembrete agendado.')
    request.value = ''
  } else {
    ElMessage.error(errorMessage.value)
  }
}

async function handleCancel(id: string) {
  try {
    await ElMessageBox.confirm('Cancelar este lembrete?', 'Confirmação', { type: 'warning' })
  } catch {
    return
  }
  await cancelReminder(id)
  ElMessage.success('Lembrete cancelado.')
}

function statusType(s: ReminderStatus) {
  return s === 'SENT' ? 'success' : s === 'FAILED' ? 'danger' : s === 'CANCELLED' ? 'info' : 'warning'
}
</script>

<template>
  <ErrorBoundary>
    <div class="view">
      <div class="header">
        <h2><el-icon class="title-icon">
            <Bell />
          </el-icon> Lembretes</h2>
      </div>

      <section class="surface create-box">
        <el-input v-model="request" placeholder='Ex.: "Reunião de alinhamento amanhã às 15:00"'
          @keyup.enter="handleCreate" />
        <el-button type="primary" :loading="creating" @click="handleCreate">Agendar</el-button>
      </section>

      <section v-if="isLoading || cancelling" class="surface loading">Carregando…</section>
      <section v-else-if="isError" class="surface">Erro ao carregar.</section>
      <section v-else class="surface">
        <el-table :data="reminders ?? []" style="width: 100%">
          <el-table-column label="Lembrete" min-width="260">
            <template #default="{ row }">
              <b>{{ row.summary ?? row.originalRequest }}</b>
            </template>
          </el-table-column>
          <el-table-column label="Lembrar em" width="180">
            <template #default="{ row }">{{ new Date(row.remindAt).toLocaleString('pt-BR') }}</template>
          </el-table-column>
          <el-table-column label="Status" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Nº Tent." width="100" prop="retryCount" />
          <el-table-column label="Ações" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status === 'PENDING'" text type="danger"
                @click="handleCancel(row.id)">Cancelar</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.header {
  margin-bottom: 12px;
}

.title-icon {
  margin-right: 6px;
  vertical-align: -2px;
}

.create-box {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.loading {
  padding: 28px;
  text-align: center;
  color: #718096;
  font-size: 13px;
}
</style>
