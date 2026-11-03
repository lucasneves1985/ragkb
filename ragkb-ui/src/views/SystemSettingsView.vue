<!-- views/SystemSettingsView.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { useAppConfigurations, useSetAppConfiguration, useNotificationTest } from '@/composables'
import PageHeading from '@/components/layout/PageHeading.vue'

const KEY_REMINDER_TIME = 'cards.reminder_time'
const KEY_REMINDER_WINDOW_DAYS = 'cards.reminder_window_days'
const KEY_DIGEST_ENABLED = 'cards.digest_enabled'
const KEY_DIGEST_FREQUENCY = 'cards.digest_frequency'
const KEY_DIGEST_TIME = 'cards.digest_time'
const KEY_DIGEST_WEEKDAY = 'cards.digest_weekday'

const DEFAULT_TIME = '08:00'
const DEFAULT_WINDOW_DAYS = 2
const DEFAULT_DIGEST_TIME = '07:30'

const { configurations, isLoadingConfigurations, refetchConfigurations } = useAppConfigurations()

// Uma instância do composable POR SEÇÃO: cada useSetAppConfiguration cria
// refs e useMutation próprios, então o loading de um botão não contamina o
// outro e o errorMessage de uma seção não vaza para a outra.
const {
  setConfiguration: setReminderConfiguration,
  saving: savingReminders,
  errorMessage: reminderErrorMessage,
} = useSetAppConfiguration()

const {
  setConfiguration: setDigestConfiguration,
  saving: savingDigest,
  errorMessage: digestErrorMessage,
} = useSetAppConfiguration()

// Testes "Checar Agora": também uma instância por seção. O teste envia uma
// mensagem APENAS para o WhatsApp do admin logado e não altera estado.
const {
  testing: testingReminder,
  result: reminderTestResult,
  errorMessage: reminderTestError,
  runTest: runReminderTest,
} = useNotificationTest('reminder')

const {
  testing: testingDigest,
  result: digestTestResult,
  errorMessage: digestTestError,
  runTest: runDigestTest,
} = useNotificationTest('digest')

// ── Lembretes de prazo ────────────────────────────────────
const reminderTime = ref(DEFAULT_TIME)
const reminderWindowDays = ref<number>(DEFAULT_WINDOW_DAYS)

// ── Digest de tarefas ─────────────────────────────────────
const digestEnabled = ref(false)
const digestFrequency = ref<'DAILY' | 'WEEKLY'>('DAILY')
const digestTime = ref(DEFAULT_DIGEST_TIME)
const digestWeekday = ref('MON')

const WEEKDAY_OPTIONS = [
  { value: 'MON', label: 'Segunda-feira' },
  { value: 'TUE', label: 'Terça-feira' },
  { value: 'WED', label: 'Quarta-feira' },
  { value: 'THU', label: 'Quinta-feira' },
  { value: 'FRI', label: 'Sexta-feira' },
  { value: 'SAT', label: 'Sábado' },
  { value: 'SUN', label: 'Domingo' },
] as const

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
  digestEnabled.value = value?.[KEY_DIGEST_ENABLED]?.toLowerCase() === 'true'
  const frequency = value?.[KEY_DIGEST_FREQUENCY]?.toUpperCase()
  if (frequency === 'DAILY' || frequency === 'WEEKLY') {
    digestFrequency.value = frequency
  }
  const digestTimeValue = value?.[KEY_DIGEST_TIME]
  if (digestTimeValue) {
    digestTime.value = digestTimeValue
  }
  // find() estreita o tipo: match só existe se weekday for um dos valores
  // válidos, e match.value é um literal do array — sem undefined possível.
  const weekday = value?.[KEY_DIGEST_WEEKDAY]?.toUpperCase()
  const match = WEEKDAY_OPTIONS.find((o) => o.value === weekday)
  if (match) {
    digestWeekday.value = match.value
  }
}, { immediate: true })

async function saveReminders() {
  reminderErrorMessage.value = ''
  try {
    await setReminderConfiguration({ key: KEY_REMINDER_TIME, value: reminderTime.value })
    await setReminderConfiguration({ key: KEY_REMINDER_WINDOW_DAYS, value: String(reminderWindowDays.value) })
    ElMessage.success(
      `Configurações salvas: envio diário às ${reminderTime.value}, janela de ${reminderWindowDays.value} dia(s). Vale na próxima execução.`,
    )
    refetchConfigurations()
  } catch {
    ElMessage.error(reminderErrorMessage.value || 'Não foi possível salvar a configuração.')
  }
}

async function saveDigest() {
  digestErrorMessage.value = ''
  try {
    await setDigestConfiguration({ key: KEY_DIGEST_ENABLED, value: String(digestEnabled.value) })
    await setDigestConfiguration({ key: KEY_DIGEST_FREQUENCY, value: digestFrequency.value })
    await setDigestConfiguration({ key: KEY_DIGEST_TIME, value: digestTime.value })
    if (digestFrequency.value === 'WEEKLY') {
      await setDigestConfiguration({ key: KEY_DIGEST_WEEKDAY, value: digestWeekday.value })
    }
    const when = digestFrequency.value === 'WEEKLY'
      ? `toda ${WEEKDAY_OPTIONS.find((o) => o.value === digestWeekday.value)?.label.toLowerCase()} às ${digestTime.value}`
      : `todos os dias às ${digestTime.value}`
    ElMessage.success(
      digestEnabled.value
        ? `Resumo ativado: ${when}. Vale em até 30 segundos.`
        : 'Resumo desativado.',
    )
    refetchConfigurations()
  } catch {
    ElMessage.error(digestErrorMessage.value || 'Não foi possível salvar a configuração.')
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
        <el-button type="primary" class="primary-button" :loading="savingReminders" @click="saveReminders">
          Salvar
        </el-button>
        <el-button :icon="Promotion" :loading="testingReminder" @click="runReminderTest">
          Checar Agora
        </el-button>
      </div>

      <el-alert v-if="reminderTestResult" class="test-alert" :type="reminderTestResult.sent ? 'success' : 'warning'"
        :title="reminderTestResult.detail" :closable="false">
        <div v-if="reminderTestResult.preview" class="test-preview">{{ reminderTestResult.preview }}</div>
      </el-alert>
      <el-alert v-else-if="reminderTestError" class="test-alert" type="error" :title="reminderTestError"
        :closable="false" />
    </section>

    <section class="surface settings-card">
      <h3 class="settings-title">Resumo de tarefas (WhatsApp)</h3>
      <p class="settings-description">
        Panorama periódico enviado a cada usuário com as próprias tarefas: atrasadas, vencendo
        hoje e vencendo nos próximos 7 dias. Usuários sem nada a vencer não recebem mensagem.
        A alteração vale em até 30 segundos, sem reiniciar a aplicação.
      </p>

      <div v-if="isLoadingConfigurations" class="settings-loading">Carregando…</div>
      <div v-else class="settings-row">
        <div class="settings-field">
          <label class="settings-label">Resumo</label>
          <el-switch v-model="digestEnabled" active-text="Ativado" inactive-text="Desativado" />
        </div>
        <div class="settings-field">
          <label class="settings-label">Frequência</label>
          <el-select v-model="digestFrequency" style="width: 140px">
            <el-option value="DAILY" label="Diário" />
            <el-option value="WEEKLY" label="Semanal" />
          </el-select>
        </div>
        <div class="settings-field">
          <label class="settings-label">Horário</label>
          <el-time-picker v-model="digestTime" value-format="HH:mm" format="HH:mm" placeholder="Horário do resumo" />
        </div>
        <div v-if="digestFrequency === 'WEEKLY'" class="settings-field">
          <label class="settings-label">Dia da semana</label>
          <el-select v-model="digestWeekday" style="width: 160px">
            <el-option v-for="option in WEEKDAY_OPTIONS" :key="option.value" :value="option.value"
              :label="option.label" />
          </el-select>
        </div>
        <el-button type="primary" class="primary-button" :loading="savingDigest" @click="saveDigest">
          Salvar
        </el-button>
        <el-button :icon="Promotion" :loading="testingDigest" @click="runDigestTest">
          Checar Agora
        </el-button>
      </div>

      <el-alert v-if="digestTestResult" class="test-alert" :type="digestTestResult.sent ? 'success' : 'warning'"
        :title="digestTestResult.detail" :closable="false">
        <div v-if="digestTestResult.preview" class="test-preview">{{ digestTestResult.preview }}</div>
      </el-alert>
      <el-alert v-else-if="digestTestError" class="test-alert" type="error" :title="digestTestError"
        :closable="false" />
    </section>
  </div>
</template>

<style scoped>
.settings-card {
  padding: 24px;
  margin-bottom: 20px;
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

.test-alert {
  margin-top: 14px;
}

.test-preview {
  margin-top: 8px;
  white-space: pre-wrap;
  font-family: 'DM Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
}
</style>
