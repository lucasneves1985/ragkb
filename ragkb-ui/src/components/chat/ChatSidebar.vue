<script setup lang="ts">
import { ChatLineRound, Close, Delete, Document, Plus } from '@element-plus/icons-vue'
import type { Conversation } from '@/types'

defineProps<{
  conversations: Conversation[]
  currentConversationId: string | undefined
  showHistory: boolean
}>()

const emit = defineEmits<{
  'update:showHistory': [value: boolean]
  select: [id: string]
  delete: [id: string, event: Event]
  new: []
}>()

function formatDate(isoStr?: string) {
  if (!isoStr) return ''
  const date = new Date(isoStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffHours = diffMs / (1000 * 60 * 60)
  if (diffHours < 24 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString([], { day: '2-digit', month: 'short' })
}
</script>

<template>
  <aside v-show="showHistory" class="chat-side surface">
    <div class="chat-side-head">
      <span>CONVERSAS</span>
      <el-button text :icon="Close" @click="emit('update:showHistory', false)" />
    </div>

    <button class="new-chat-btn" @click="emit('new')">
      <Plus /> Nova conversa
    </button>

    <div class="history-list">
      <button
        v-for="conv in conversations"
        :key="conv.id"
        class="history"
        :class="{ active: currentConversationId === conv.id }"
        @click="emit('select', conv.id)"
      >
        <ChatLineRound />
        <span class="history-title">{{ conv.title }}</span>
        <small>{{ formatDate(conv.updatedAt) }}</small>
        <span class="delete-icon" title="Excluir conversa" @click="emit('delete', conv.id, $event)">
          <Delete />
        </span>
      </button>

      <div v-if="conversations.length === 0" class="empty-history">
        Nenhuma conversa anterior
      </div>
    </div>

    <div class="side-note">
      <Document />
      <span>Apenas fontes autorizadas sao usadas nas respostas.</span>
    </div>
  </aside>
</template>
