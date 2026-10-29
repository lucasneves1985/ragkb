<!-- components/cards/CardDetailDialog.vue -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, Download, Paperclip } from '@element-plus/icons-vue'
import {
  useCardDetail,
  useUpdateCard,
  useMoveCard,
  useAddComment,
  useUpdateComment,
  useDeleteComment,
  useUploadAttachment,
  useDeleteAttachment,
  extractCardsErrorMessage,
} from '@/composables'
import { cardsService } from '@/services'
import { CARD_STATUSES, CARD_STATUS_LABELS } from '@/types'
import type { CardComment, CardStatus } from '@/types'

const props = defineProps<{
  cardId: string | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const visible = computed({
  get: () => !!props.cardId,
  set: (value) => {
    if (!value) emit('close')
  },
})

const cardIdRef = computed(() => props.cardId)
const { data: detail, isLoading } = useCardDetail(cardIdRef)

const { mutateAsync: updateCard } = useUpdateCard()
const { mutateAsync: moveCard } = useMoveCard()
const { mutateAsync: addComment } = useAddComment()
const { mutateAsync: updateComment } = useUpdateComment()
const { mutateAsync: deleteComment } = useDeleteComment()
const { mutateAsync: uploadAttachment } = useUploadAttachment()
const { mutateAsync: deleteAttachment } = useDeleteAttachment()

// ── Edição do card ────────────────────────────────────────
const editing = ref(false)
const title = ref('')
const description = ref('')
const savingCard = ref(false)

watch(detail, (value) => {
  if (value) {
    title.value = value.title
    description.value = value.description ?? ''
    editing.value = false
  }
}, { immediate: true })

async function saveCard() {
  if (!detail.value) return
  const trimmed = title.value.trim()
  if (!trimmed) {
    ElMessage.warning('O título é obrigatório.')
    return
  }
  savingCard.value = true
  try {
    await updateCard({
      id: detail.value.id,
      request: { title: trimmed, description: description.value.trim() || undefined },
    })
    ElMessage.success('Card atualizado!')
    editing.value = false
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    savingCard.value = false
  }
}

// ── Status ────────────────────────────────────────────────
const moving = ref(false)

async function changeStatus(status: CardStatus) {
  if (!detail.value || detail.value.status === status) return
  moving.value = true
  try {
    await moveCard({ id: detail.value.id, request: { status, position: null } })
    ElMessage.success(`Status alterado para ${CARD_STATUS_LABELS[status]}.`)
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    moving.value = false
  }
}

// ── Comentários ───────────────────────────────────────────
const newComment = ref('')
const addingComment = ref(false)
const editingCommentId = ref<string | null>(null)
const editText = ref('')

async function submitComment() {
  if (!detail.value) return
  const content = newComment.value.trim()
  if (!content) return
  addingComment.value = true
  try {
    await addComment({ cardId: detail.value.id, request: { content } })
    newComment.value = ''
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    addingComment.value = false
  }
}

function startEditComment(comment: CardComment) {
  editingCommentId.value = comment.id
  editText.value = comment.content
}

async function submitCommentEdit() {
  if (!editingCommentId.value) return
  const content = editText.value.trim()
  if (!content) return
  try {
    await updateComment({ commentId: editingCommentId.value, request: { content } })
    editingCommentId.value = null
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

async function removeComment(comment: CardComment) {
  try {
    await ElMessageBox.confirm('Excluir este comentário?', 'Excluir comentário', {
      type: 'warning',
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
    })
  } catch {
    return
  }
  try {
    await deleteComment(comment.id)
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

// ── Anexos ────────────────────────────────────────────────
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)

function pickFile() {
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !detail.value) return
  uploading.value = true
  try {
    await uploadAttachment({ cardId: detail.value.id, file })
    ElMessage.success('Anexo enviado!')
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    uploading.value = false
  }
}

async function download(attachmentId: string, fileName: string) {
  if (!detail.value) return
  try {
    await cardsService.downloadAttachment(detail.value.id, attachmentId, fileName)
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

async function removeAttachment(attachmentId: string, fileName: string) {
  try {
    await ElMessageBox.confirm(`Excluir o anexo "${fileName}"?`, 'Excluir anexo', {
      type: 'warning',
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
    })
  } catch {
    return
  }
  if (!detail.value) return
  try {
    await deleteAttachment({ cardId: detail.value.id, attachmentId })
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(value: string): string {
  return new Date(value).toLocaleString('pt-BR')
}
</script>

<template>
  <el-dialog v-model="visible" title="Detalhe da tarefa" width="640px" top="6vh">
    <div v-if="isLoading || !detail" class="column-hint">Carregando…</div>

    <template v-else>
      <!-- Título e descrição -->
      <template v-if="!editing">
        <h3 class="detail-title">{{ detail.title }}</h3>
        <p class="detail-description">{{ detail.description || 'Sem descrição.' }}</p>
        <el-button size="small" text type="primary" @click="editing = true">Editar</el-button>
      </template>

      <template v-else>
        <el-input v-model="title" maxlength="200" placeholder="Título" class="card-detail-section" />
        <el-input v-model="description" type="textarea" :rows="4" placeholder="Descrição" class="card-detail-section" />
        <div class="card-detail-section">
          <el-button size="small" @click="editing = false">Cancelar</el-button>
          <el-button size="small" type="primary" class="primary-button" :loading="savingCard" @click="saveCard">
            Salvar
          </el-button>
        </div>
      </template>

      <!-- Status -->
      <h4>Status</h4>
      <el-select :model-value="detail.status" :loading="moving"
        @update:model-value="(value: CardStatus) => changeStatus(value)">
        <el-option v-for="status in CARD_STATUSES" :key="status" :value="status" :label="CARD_STATUS_LABELS[status]" />
      </el-select>

      <!-- Comentários -->
      <h4><el-icon>
          <ChatDotRound />
        </el-icon> Comentários ({{ detail.comments.length }})</h4>
      <div v-for="comment in detail.comments" :key="comment.id" class="comment-item">
        <template v-if="editingCommentId === comment.id">
          <el-input v-model="editText" type="textarea" :rows="3" />
          <div class="card-detail-section">
            <el-button size="small" @click="editingCommentId = null">Cancelar</el-button>
            <el-button size="small" type="primary" class="primary-button" @click="submitCommentEdit">
              Salvar
            </el-button>
          </div>
        </template>
        <template v-else>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-date">
            {{ formatDate(comment.createdAt) }}
            <el-button size="small" text type="primary" @click="startEditComment(comment)">Editar</el-button>
            <el-button size="small" text type="danger" @click="removeComment(comment)">Excluir</el-button>
          </div>
        </template>
      </div>
      <el-input v-model="newComment" type="textarea" :rows="2" placeholder="Escreva um comentário…" />
      <div class="card-detail-section">
        <el-button size="small" type="primary" class="primary-button" :loading="addingComment"
          :disabled="!newComment.trim()" @click="submitComment">
          Comentar
        </el-button>
      </div>

      <!-- Anexos -->
      <h4><el-icon>
          <Paperclip />
        </el-icon> Anexos ({{ detail.attachments.length }})</h4>
      <input ref="fileInput" type="file" hidden @change="onFileSelected" />
      <div v-for="attachment in detail.attachments" :key="attachment.id" class="attachment-item">
        <span class="attachment-name">{{ attachment.fileName }}</span>
        <span class="attachment-size">{{ formatSize(attachment.sizeBytes) }}</span>
        <el-button size="small" text type="primary" @click="download(attachment.id, attachment.fileName)">
          <el-icon>
            <Download />
          </el-icon>
        </el-button>
        <el-button size="small" text type="danger" @click="removeAttachment(attachment.id, attachment.fileName)">
          <el-icon>
            <Delete />
          </el-icon>
        </el-button>
      </div>
      <el-button size="small" :loading="uploading" @click="pickFile">Enviar anexo</el-button>

      <!-- Histórico de status -->
      <h4>Histórico de status</h4>
      <el-timeline>
        <el-timeline-item v-for="(event, index) in detail.statusHistory" :key="index"
          :timestamp="formatDate(event.changedAt)">
          <template v-if="event.fromStatus">
            {{ CARD_STATUS_LABELS[event.fromStatus as CardStatus] }} →
            <strong>{{ CARD_STATUS_LABELS[event.toStatus as CardStatus] }}</strong>
          </template>
          <template v-else>
            Criada como <strong>{{ CARD_STATUS_LABELS[event.toStatus as CardStatus] }}</strong>
          </template>
        </el-timeline-item>
      </el-timeline>
    </template>
  </el-dialog>
</template>

<style src="@/views/styles/cards.view.css"></style>
