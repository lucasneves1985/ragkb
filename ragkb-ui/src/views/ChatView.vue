<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ChatLineRound, Close, Delete, Document, Plus, Promotion, Tickets } from '@element-plus/icons-vue'
import { useChat, useConversations } from '@/composables'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const md = new MarkdownIt({ breaks: true, html: false })
function renderMarkdown(content?: string) {
  if (!content) return ''
  return DOMPurify.sanitize(md.render(content))
}

const showHistory = ref(true)
const messagesContainer = ref<HTMLElement | null>(null)

const {
  auth,
  question,
  loading,
  messages,
  ticket,
  currentConversationId,
  canSend,
  scrollToBottom,
  selectConversation,
  startNewConversation,
  ask,
  openTicket,
} = useChat({ messagesContainer })

const { conversations, loadConversations, deleteConversation } = useConversations()

async function handleAsk() {
  await ask(() => loadConversations())
}

function resetChat() {
  startNewConversation()
}

async function removeConversation(id: string, event: Event) {
  event.stopPropagation()
  const success = await deleteConversation(id)
  if (success && currentConversationId.value === id) {
    startNewConversation()
  }
}

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

onMounted(() => {
  loadConversations()
  scrollToBottom()
})
</script>

<template>
  <ErrorBoundary @retry="resetChat">
  <div class="chat-layout">
    <aside v-show="showHistory" class="chat-side surface">
      <div class="chat-side-head">
        <span>CONVERSAS</span>
        <el-button text :icon="Close" @click="showHistory = false" />
      </div>

      <button class="new-chat-btn" @click="startNewConversation">
        <Plus /> Nova conversa
      </button>

      <div class="history-list">
        <button v-for="conv in conversations" :key="conv.id" class="history"
          :class="{ active: currentConversationId === conv.id }" @click="selectConversation(conv.id)">
          <ChatLineRound />
          <span class="history-title">{{ conv.title }}</span>
          <small>{{ formatDate(conv.updatedAt) }}</small>
          <span class="delete-icon" title="Excluir conversa" @click="removeConversation(conv.id, $event)">
            <Delete />
          </span>
        </button>

        <div v-if="conversations.length === 0" class="empty-history">
          Nenhuma conversa anterior
        </div>
      </div>

      <div class="side-note">
        <Document />
        <span>Apenas fontes autorizadas são usadas nas respostas.</span>
      </div>
    </aside>

    <section class="chat-main">
      <button v-if="!showHistory" class="show-side" @click="showHistory = true">☰ Histórico</button>
      <div class="chat-welcome">
        <p class="eyebrow">ASSISTENTE VIRTUAL</p>
        <h1>Como posso ajudar?</h1>
        <p>Faça perguntas sobre normas, políticas e processos internos.</p>
      </div>

      <div ref="messagesContainer" class="messages">
        <article v-for="message in messages" :key="message.id" class="message" :class="message.from">
          <div class="avatar">{{ message.from === 'user' ? auth.initials : 'R' }}</div>
          <div class="bubble">
            <div v-if="message.text" class="message-text" v-html="renderMarkdown(message.text)"></div>

            <template v-if="message.ticket">
              <div class="fallback-head">
                <Tickets />
                <div>
                  <b>Conhecimento não localizado</b>
                  <p>Posso encaminhar esta solicitação ao suporte técnico.</p>
                </div>
              </div>
              <el-alert v-if="message.ticketSent"
                title="Chamado aberto com sucesso. Você receberá atualizações pelo canal corporativo." type="success"
                :closable="false" show-icon />
              <el-form v-else label-position="top" class="ticket-form">
                <div class="ticket-fields">
                  <el-form-item label="Assunto"><el-input v-model="ticket.subject" /></el-form-item>
                </div>
                <el-form-item label="Descrição"><el-input v-model="ticket.description" type="textarea"
                    :rows="3" /></el-form-item>
                <el-button class="primary-button" @click="openTicket(message)">Abrir chamado técnico</el-button>
              </el-form>
            </template>

            <div v-if="message.sources?.length" class="sources">
              <span>FONTES CONSULTADAS</span>
              <el-tag v-for="source in message.sources" :key="source" size="small" effect="plain">
                <Document /> {{ source }}
              </el-tag>
            </div>
          </div>
        </article>

        <div v-if="loading" class="message assistant">
          <div class="avatar">R</div>
          <div class="bubble typing"><i></i><i></i><i></i></div>
        </div>
      </div>

      <div class="input-area">
        <div class="composer">
          <el-input v-model.trim="question" type="textarea" :autosize="{ minRows: 2, maxRows: 5 }" resize="none"
            placeholder="Pergunte algo à base de conhecimento…" :disabled="loading"
            @keydown.enter.exact.prevent="handleAsk" />
          <el-button circle class="send" :disabled="!canSend" :icon="Promotion" @click="handleAsk" />
        </div>
        <p>Enter para enviar · As respostas são geradas exclusivamente com documentos autorizados.</p>
      </div>
    </section>
  </div>
  </ErrorBoundary>
</template>

<style scoped lang="css" src="@/views/styles/chat.view.css"></style>
