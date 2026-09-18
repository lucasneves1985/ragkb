<!-- components/business-rules/BusinessRuleEditorDialog.vue -->
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Download, UploadFilled } from '@element-plus/icons-vue'
import { businessRulesService } from '@/services'
import type { Article, BusinessRule, BusinessRuleAttachment, BusinessRuleSubmitPayload, Sector } from '@/types'

const props = defineProps<{
  modelValue: boolean
  sectors: Sector[]
  articles: Article[]
  rule?: BusinessRule | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: BusinessRuleSubmitPayload): void
}>()

const form = reactive({
  title: '',
  description: '',
  requester: '',
  reason: '',
  sectorNames: [] as string[],
  articleIds: [] as string[],
})

// ── Anexos ─────────────────────────────────────────────────
// Backend aceita: pdf, doc, docx, csv, xlsx, zip, rar (20 MB, magic bytes).
const attachments = ref<BusinessRuleAttachment[]>([])
const uploadingAttachment = ref(false)
const attachmentInput = ref<HTMLInputElement | null>(null)
const ACCEPTED_EXTENSIONS = '.pdf,.doc,.docx,.csv,.xlsx,.zip,.rar'

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    form.title = props.rule?.title ?? ''
    form.description = props.rule?.description ?? ''
    form.requester = props.rule?.requester ?? ''
    form.reason = props.rule?.reason ?? ''
    form.sectorNames = props.rule ? [...props.rule.sectorNames] : []
    form.articleIds = props.rule ? [...props.rule.articleIds] : []

    // Anexos só existem com regra salva (o endpoint é /rules/{id}/attachments).
    attachments.value = []
    if (props.rule) {
      loadAttachments(props.rule.id)
    }
  },
)

async function loadAttachments(ruleId: string) {
  try {
    attachments.value = await businessRulesService.listAttachments(ruleId)
  } catch {
    // Anexo é secundário — falha ao carregar não bloqueia a edição da regra.
  }
}

function openAttachmentPicker() {
  attachmentInput.value?.click()
}

async function handleAttachmentChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // permite reenviar o mesmo arquivo
  if (!file || !props.rule) return

  uploadingAttachment.value = true
  try {
    attachments.value = await businessRulesService.uploadAttachment(props.rule.id, file)
    ElMessage.success('Anexo enviado.')
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(message || 'Não foi possível enviar o anexo.')
  } finally {
    uploadingAttachment.value = false
  }
}

async function handleAttachmentDelete(attachment: BusinessRuleAttachment) {
  if (!props.rule) return
  try {
    attachments.value = await businessRulesService.deleteAttachment(props.rule.id, attachment.id)
    ElMessage.success('Anexo removido.')
  } catch {
    ElMessage.error('Não foi possível remover o anexo.')
  }
}

async function handleAttachmentDownload(attachment: BusinessRuleAttachment) {
  if (!props.rule) return
  try {
    await businessRulesService.downloadAttachment(props.rule.id, attachment)
  } catch {
    ElMessage.error('Não foi possível baixar o anexo.')
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

// ── Formulário ─────────────────────────────────────────────
function close() {
  emit('update:modelValue', false)
}

function validate(): boolean {
  if (form.title.trim().length < 3 || form.title.trim().length > 120) {
    ElMessage.error('Título deve ter entre 3 e 120 caracteres.')
    return false
  }
  if (!form.description.trim()) {
    ElMessage.error('Informe a descrição.')
    return false
  }
  if (form.sectorNames.length === 0) {
    ElMessage.error('Informe ao menos um setor com acesso.')
    return false
  }
  return true
}

function submit() {
  if (!validate()) return
  emit('submit', {
    title: form.title,
    description: form.description,
    requester: form.requester,
    reason: form.reason,
    sectorNames: [...form.sectorNames],
    articleIds: [...form.articleIds],
  })
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="rule ? 'Editar regra' : 'Nova regra'" width="900px" top="4vh"
    :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
    @update:model-value="emit('update:modelValue', $event)">
    <el-form label-position="top" @submit.prevent>
      <el-row :gutter="16">
        <el-col :span="16">
          <el-form-item label="Título *">
            <el-input v-model="form.title" :maxlength="120" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="Solicitante">
            <el-input v-model="form.requester" :maxlength="120" placeholder="Opcional" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="Descrição *">
        <el-input v-model="form.description" type="textarea" :rows="6" :maxlength="5000" show-word-limit />
      </el-form-item>

      <el-form-item label="Motivo">
        <el-input v-model="form.reason" type="textarea" :rows="2" :maxlength="500" show-word-limit
          placeholder="Motivo da criação (opcional)" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Setores com acesso *">
            <el-select v-model="form.sectorNames" multiple placeholder="Selecione os setores" style="width: 100%">
              <el-option v-for="sector in sectors" :key="sector.id" :label="sector.name" :value="sector.name" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Artigos vinculados">
            <el-select v-model="form.articleIds" multiple filterable placeholder="Vincule artigos publicados (opcional)"
              style="width: 100%">
              <el-option v-for="article in articles" :key="article.id" :label="article.title" :value="article.id"
                :disabled="article.status !== 'PUBLISHED'" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Anexos: somente com regra já salva -->
      <el-form-item label="Anexos">
        <template v-if="rule">
          <input ref="attachmentInput" type="file" :accept="ACCEPTED_EXTENSIONS" style="display: none"
            @change="handleAttachmentChange" />
          <div class="attachments">
            <ul v-if="attachments.length > 0" class="attachment-list">
              <li v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
                <span class="attachment-name">{{ attachment.fileName }}</span>
                <span class="attachment-meta">{{ formatSize(attachment.sizeBytes) }}</span>
                <el-button text type="primary" :icon="Download" @click="handleAttachmentDownload(attachment)" />
                <el-button text type="danger" :icon="Delete" @click="handleAttachmentDelete(attachment)" />
              </li>
            </ul>
            <el-button :icon="UploadFilled" :loading="uploadingAttachment" @click="openAttachmentPicker">
              Anexar documento
            </el-button>
            <span class="attachment-hint">PDF, DOC(X), CSV, XLSX, ZIP ou RAR — até 20 MB</span>
          </div>
        </template>
        <el-alert v-else type="info" :closable="false" title="Salve a regra para anexar documentos." />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="close">Cancelar</el-button>
      <el-button type="primary" :loading="saving" @click="submit">Salvar</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.attachments {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.attachment-name {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta {
  color: #9aa4b1;
  font: 10px 'DM Mono';
}

.attachment-hint {
  color: #9aa4b1;
  font-size: 11px;
}
</style>
