<!-- components/integrations/IntegrationEditorDialog.vue -->
<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CreateIntegrationRequest,
  Integration,
  IntegrationActionType,
  IntegrationAuthType,
  IntegrationType,
  UpdateIntegrationRequest,
} from '@/types'

const props = defineProps<{
  modelValue: boolean
  integration?: Integration | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: UpdateIntegrationRequest): void
}>()

const AUTH_TYPES: IntegrationAuthType[] = ['NONE', 'BEARER', 'BASIC', 'HEADER_CUSTOM']
const ACTION_TYPES: IntegrationActionType[] = ['NONE', 'EMAIL', 'WHATSAPP']

const form = reactive({
  name: '',
  description: '',
  url: '',
  authType: 'NONE' as IntegrationAuthType,
  credentials: '',
  requestTemplate: '',
  outputSchema: '',
  integrationType: 'SCHEDULED' as IntegrationType,
  scheduleCron: '',
  scheduleTimezone: 'America/Sao_Paulo',
  scheduleIntervalSeconds: null as number | null,
  contextDescription: '',
  paramsDefinition: '',
  actionType: 'NONE' as IntegrationActionType,
  actionTarget: '',
  actionTemplate: '',
  active: true,
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const i = props.integration
    form.name = i?.name ?? ''
    form.description = i?.description ?? ''
    form.url = i?.url ?? ''
    form.authType = i?.authType ?? 'NONE'
    form.credentials = '' // blank = manter credencial existente (contrato do backend)
    form.requestTemplate = i?.requestTemplate ?? ''
    form.outputSchema = i?.outputSchema ?? ''
    form.integrationType = i?.integrationType ?? 'SCHEDULED'
    form.scheduleCron = i?.scheduleCron ?? ''
    form.scheduleTimezone = i?.scheduleTimezone ?? 'America/Sao_Paulo'
    form.scheduleIntervalSeconds = i?.scheduleIntervalSeconds ?? null
    form.contextDescription = i?.contextDescription ?? ''
    form.paramsDefinition = i?.paramsDefinition ?? ''
    form.actionType = i?.actionType ?? 'NONE'
    form.actionTarget = i?.actionTarget ?? ''
    form.actionTemplate = i?.actionTemplate ?? ''
    form.active = i?.active ?? true
  },
)

function close() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (!form.name.trim()) return ElMessage.error('Informe o nome.')
  if (!form.url.trim()) return ElMessage.error('Informe a URL.')
  if (form.integrationType === 'SCHEDULED' && !form.scheduleCron.trim() && !form.scheduleIntervalSeconds) {
    return ElMessage.error('Agendada exige cron ou intervalo em segundos.')
  }
  if (form.integrationType === 'QUERY' && !form.contextDescription.trim()) {
    return ElMessage.error('Consulta exige a descrição de contexto para o LLM.')
  }
  if (form.actionType === 'EMAIL' && !form.actionTarget.includes('@')) {
    return ElMessage.error('Ação e-mail exige um e-mail de destino.')
  }
  if (form.actionType === 'WHATSAPP' && !form.actionTarget.endsWith('@c.us')) {
    return ElMessage.error('Ação WhatsApp exige chatId no formato 55DDNNNNNNNNN@c.us.')
  }

  emit('submit', {
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    url: form.url.trim(),
    authType: form.authType,
    // Enviado apenas se preenchido — blank/null mantém a credencial existente
    credentials: form.credentials.trim() || undefined,
    requestTemplate: form.requestTemplate.trim() || undefined,
    outputSchema: form.outputSchema.trim() || undefined,
    integrationType: form.integrationType,
    scheduleCron: form.scheduleCron.trim() || undefined,
    scheduleTimezone: form.scheduleTimezone.trim() || undefined,
    scheduleIntervalSeconds: form.scheduleIntervalSeconds ?? undefined,
    contextDescription: form.contextDescription.trim() || undefined,
    paramsDefinition: form.paramsDefinition.trim() || undefined,
    actionType: form.actionType,
    actionTarget: form.actionTarget.trim() || undefined,
    actionTemplate: form.actionTemplate.trim() || undefined,
    active: form.active,
  } satisfies CreateIntegrationRequest)
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="integration ? 'Editar integração' : 'Nova integração'" width="760px"
    top="4vh" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
    @update:model-value="emit('update:modelValue', $event)">
    <el-form label-position="top">
      <el-form-item label="Nome" required>
        <el-input v-model="form.name" maxlength="120" />
      </el-form-item>
      <el-form-item label="Descrição">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="URL" required>
        <el-input v-model="form.url" placeholder="https://api.exemplo.com/endpoint" />
      </el-form-item>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="Autenticação">
            <el-select v-model="form.authType">
              <el-option v-for="t in AUTH_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="integration?.hasCredentials ? 'Credenciais (em branco mantém)' : 'Credenciais'">
            <el-input v-model="form.credentials" type="password" show-password
              :placeholder="integration?.hasCredentials ? '••••••••' : ''" />
          </el-form-item>
        </el-col>
      </el-row>
      <div v-if="form.authType === 'HEADER_CUSTOM'" class="hint">
        HEADER_CUSTOM: credentials em JSON {"header": "X-Api-Key", "value": "..."}
      </div>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="Tipo" required>
            <el-select v-model="form.integrationType">
              <el-option label="Agendada (pull)" value="SCHEDULED" />
              <el-option label="Consulta (LLM)" value="QUERY" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Ativa">
            <el-switch v-model="form.active" />
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="form.integrationType === 'SCHEDULED'">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Cron (opcional)">
              <el-input v-model="form.scheduleCron" placeholder="0 0 8 * * *" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Timezone">
              <el-input v-model="form.scheduleTimezone" placeholder="America/Sao_Paulo" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Intervalo em segundos (opcional, se sem cron)">
          <el-input-number v-model="form.scheduleIntervalSeconds" :min="30" :step="30" />
        </el-form-item>
      </template>

      <template v-if="form.integrationType === 'QUERY'">
        <el-form-item label="Contexto para o LLM" required>
          <el-input v-model="form.contextDescription" type="textarea" :rows="2"
            placeholder='Ex.: "Retorna quantidade de atendimentos no dia"' />
        </el-form-item>
        <el-form-item label="Definição de parâmetros (JSON Schema, opcional)">
          <el-input v-model="form.paramsDefinition" type="textarea" :rows="3" />
        </el-form-item>
      </template>

      <el-form-item label="Template do body (opcional)">
        <el-input v-model="form.requestTemplate" type="textarea" :rows="3" placeholder='{"date": "{{today}}"}' />
      </el-form-item>
      <el-form-item label="Output schema (JSON Schema da resposta, opcional)">
        <el-input v-model="form.outputSchema" type="textarea" :rows="3" />
      </el-form-item>

      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="Ação pós-execução">
            <el-select v-model="form.actionType">
              <el-option v-for="t in ACTION_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item v-if="form.actionType !== 'NONE'" label="Destino da ação" required>
            <el-input v-model="form.actionTarget"
              :placeholder="form.actionType === 'EMAIL' ? 'destino@empresa.com' : '55DDNNNNNNNNN@c.us'" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item v-if="form.actionType !== 'NONE'" label="Template da mensagem (opcional)">
        <el-input v-model="form.actionTemplate" type="textarea" :rows="3"
          placeholder="Cotação de {{response.date}}: 1 {{response.base}} = R$ {{response.rates.BRL}}" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">Cancelar</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">Salvar</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  font-size: 12px;
  color: #7a8699;
  margin: -8px 0 12px;
}
</style>
