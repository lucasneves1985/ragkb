<!-- components/business-rules/BusinessRuleDetailDialog.vue -->
<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { articlesService, businessRulesService } from '@/services'
import type { ArticleDetail, BusinessRule, BusinessRuleAttachment } from '@/types'

const props = defineProps<{
  modelValue: boolean
  rule: BusinessRule | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const attachments = ref<BusinessRuleAttachment[]>([])
const linkedArticles = ref<ArticleDetail[]>([])
const loadingExtras = ref(false)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    attachments.value = []
    linkedArticles.value = []
    if (props.rule) {
      loadExtras(props.rule)
    }
  },
)

async function loadExtras(rule: BusinessRule) {
  loadingExtras.value = true
  try {
    const [attachmentList, articles] = await Promise.all([
      businessRulesService.listAttachments(rule.id),
      Promise.all(rule.articleIds.map((id) => articlesService.get(id).catch(() => null))),
    ])
    attachments.value = attachmentList
    linkedArticles.value = articles.filter((a): a is ArticleDetail => a !== null)
  } catch {
    // Anexos/artigos são complementares — falha não bloqueia a visualização.
  } finally {
    loadingExtras.value = false
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

function statusLabel(s: BusinessRule['status']) {
  return s === 'PUBLISHED' ? 'Publicada' : s === 'ARCHIVED' ? 'Arquivada' : 'Rascunho'
}

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="rule?.title ?? 'Regra'" width="900px" top="4vh"
    @update:model-value="emit('update:modelValue', $event)">
    <template v-if="rule">
      <div class="detail-header">
        <el-tag size="small" effect="light">{{ statusLabel(rule.status) }}</el-tag>
        <span v-for="sector in rule.sectorNames" :key="sector" class="sector-chip">
          {{ sector }}
        </span>
      </div>

      <el-descriptions :column="2" border class="detail-block">
        <el-descriptions-item label="Solicitante">
          {{ rule.requester || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="Autor">{{ rule.authorUsername }}</el-descriptions-item>
        <el-descriptions-item label="Criada em">
          {{ new Date(rule.createdAt).toLocaleString('pt-BR') }}
        </el-descriptions-item>
        <el-descriptions-item label="Última edição">
          {{ new Date(rule.updatedAt).toLocaleString('pt-BR') }}
        </el-descriptions-item>
        <el-descriptions-item label="Editada por" :span="2">
          {{ rule.updatedUsername || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <h4>Descrição</h4>
        <p class="description">{{ rule.description }}</p>
      </div>

      <div v-if="rule.reason" class="detail-section">
        <h4>Motivo</h4>
        <p class="description">{{ rule.reason }}</p>
      </div>

      <div class="detail-section">
        <h4>Artigos vinculados</h4>
        <p v-if="linkedArticles.length === 0 && rule.articleIds.length === 0 && !loadingExtras" class="empty">
          Nenhum artigo vinculado.
        </p>
        <p v-else-if="linkedArticles.length === 0 && rule.articleIds.length > 0 && !loadingExtras" class="empty">
          Artigos vinculados indisponíveis.
        </p>
        <ul v-else-if="linkedArticles.length > 0" class="article-list">
          <li v-for="article in linkedArticles" :key="article.id">
            <a :href="article.url" target="_blank" rel="noopener" class="article-link">
              {{ article.title }}
            </a>
          </li>
        </ul>
      </div>

      <div class="detail-section">
        <h4>Anexos</h4>
        <p v-if="attachments.length === 0 && !loadingExtras" class="empty">Nenhum anexo.</p>
        <ul v-else class="attachment-list">
          <li v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
            <span class="attachment-name">{{ attachment.fileName }}</span>
            <span class="attachment-meta">{{ formatSize(attachment.sizeBytes) }}</span>
            <el-button text type="primary" :icon="Download" @click="handleAttachmentDownload(attachment)">
              Baixar
            </el-button>
          </li>
        </ul>
      </div>
    </template>

    <template #footer>
      <el-button @click="close">Fechar</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
}

.sector-chip {
  padding: 2px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  font-size: 12px;
  color: #7a8699;
}

.detail-block {
  margin-bottom: 16px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h4 {
  margin: 0 0 6px;
  font-size: 13px;
  color: #4a5568;
}

.description {
  margin: 0;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.6;
}

.empty {
  margin: 0;
  color: #9aa4b1;
  font-size: 12px;
}

.article-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.article-link {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 13px;
}

.article-link:hover {
  text-decoration: underline;
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
</style>
