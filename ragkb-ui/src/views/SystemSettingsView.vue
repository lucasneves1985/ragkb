<!-- views/SystemSettingsView.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAppConfigurations, useSetAppConfiguration } from '@/composables'
import PageHeading from '@/components/layout/PageHeading.vue'

const KEY_REMINDER_TIME = 'cards.reminder_time'
const KEY_REMINDER_WINDOW_DAYS = 'cards.reminder_window_days'
const DEFAULT_TIME = '08:00'
const DEFAULT_WINDOW_DAYS = 2

const { configurations, isLoadingConfigurations, refetchConfigurations } = useAppConfigurations()
const { setConfiguration, saving, errorMessage } = useSetAppConfiguration()

const reminderTime = ref(DEFAULT_TIME)
const reminderWindowDays = ref<number>(DEFAULT_WINDOW_DAYS)

watch(configurations, (value) => {
  const time = value?.[KEY_REMINDER_TIME]
  if (time) {
    reminderTime.value = time
  }
  const window = value?.[KEY_REMINDER_WINDOW_DAYS]
  if (window) {
    const parsed = Number(window)
    if (Number.isInteger(parsed) && parsed >= 1 && parsed <= 14) {
      reminderWindowDays.value = parsed
    }
  }
}, { immediate: true })

async function save() {
  errorMessage.value = ''
  try {
    await setConfiguration({ key: KEY_REMINDER_TIME, value: reminderTime.value })
    await setConfiguration({ key: KEY_REMINDER_WINDOW_DAYS, value: String(reminderWindowDays.value) })
    ElMessage.success(
      `Configurações salvas: envio diário às ${reminderTime.value}, janela de ${reminderWindowDays.value} dia(s). Vale na próxima execução.`,
    )
    refetchConfigurations()
  } catch {
    ElMessage.error(errorMessage.value || 'Não foi possível salvar a configuração.')
  }
}
</script>

<template>
  <div>
    <PageHeading eyebrow="ADMINISTRAÇÃO" title="Configurações do sistema"
      description="Parâmetros globais da aplicação. Visível apenas para administradores." />

    <section class="surface settings-card">
      <h3 class="settings-title">Lembretes de tarefas (WhatsApp)</h3>
      <p class="settings-description">
        Horário diário em que o sistema envia os lembretes de prazo das tarefas pelo WhatsApp e
        quantos dias antes do vencimento o aviso é disparado (janela de alerta). A alteração
        vale na próxima execução diária, sem reiniciar a aplicação. Se o horário escolhido já
        passou hoje, a próxima execução acontece amanhã no horário configurado. Cards cujo prazo
        já está dentro da nova janela recebem o aviso na próxima execução — cards que já
        receberam aviso pelo prazo anterior não recebem de novo, a menos que o prazo seja editado.
      </p>

      <div v-if="isLoadingConfigurations" class="settings-loading">Carregando…</div>
      <div v-else class="settings-row">
        <div class="settings-field">
          <label class="settings-label">Horário de envio</label>
          <el-time-picker v-model="reminderTime" value-format="HH:mm" format="HH:mm" placeholder="Horário de envio" />
        </div>
        <div class="settings-field">
          <label class="settings-label">Janela de alerta (dias antes)</label>
          <el-input-number v-model="reminderWindowDays" :min="1" :max="14" :step="1" step-strictly
            controls-position="right" />
        </div>
        <el-button type="primary" class="primary-button" :loading="saving" @click="save">
          Salvar
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.settings-card {
  padding: 24px;
}

.settings-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
}

.settings-description {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  max-width: 640px;
}

.settings-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.settings-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.settings-loading {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
