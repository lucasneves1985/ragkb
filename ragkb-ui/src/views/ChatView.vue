<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ChatLineRound, Close, Delete, Document, Plus, Promotion, Tickets } from '@element-plus/icons-vue'
import { useChat, useConversations } from '@/composables'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

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
        <button
          v-for="conv in conversations"
          :key="conv.id"
          class="history"
          :class="{ active: currentConversationId === conv.id }"
          @click="selectConversation(conv.id)"
        >
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
              <el-alert
                v-if="message.ticketSent"
                title="Chamado aberto com sucesso. Você receberá atualizações pelo canal corporativo."
                type="success"
                :closable="false"
                show-icon
              />
              <el-form v-else label-position="top" class="ticket-form">
                <div class="ticket-fields">
                  <el-form-item label="Assunto"><el-input v-model="ticket.subject" /></el-form-item>
                </div>
                <el-form-item label="Descrição"><el-input v-model="ticket.description" type="textarea" :rows="3" /></el-form-item>
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
          <el-input
            v-model.trim="question"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none"
            placeholder="Pergunte algo à base de conhecimento…"
            :disabled="loading"
            @keydown.enter.exact.prevent="handleAsk"
          />
          <el-button circle class="send" :disabled="!canSend" :icon="Promotion" @click="handleAsk" />
        </div>
        <p>Enter para enviar · As respostas são geradas exclusivamente com documentos autorizados.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  height: calc(100vh - 144px);
  max-height: calc(100vh - 144px);
  gap: 26px;
  overflow: hidden;
}
.chat-side {
  width: 226px;
  flex: 0 0 226px;
  height: 100%;
  max-height: 100%;
  padding: 13px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px 10px;
  color: #8490a1;
  font: 10px 'DM Mono';
  letter-spacing: 1px;
  flex-shrink: 0;
}
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 9px;
  margin-bottom: 12px;
  border: 1px dashed #168463;
  border-radius: 8px;
  background: #f0fdf9;
  color: #168463;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.new-chat-btn:hover {
  background: #e1f7ef;
}
.new-chat-btn svg {
  width: 14px;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.history {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #566278;
  text-align: left;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.history svg {
  width: 14px;
  flex-shrink: 0;
}
.history-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.history small {
  margin-left: auto;
  color: #9aa4b3;
  font-size: 10px;
  flex-shrink: 0;
}
.delete-icon {
  display: none;
  padding: 2px;
  color: #a0aec0;
  border-radius: 4px;
}
.delete-icon:hover {
  color: #e53e3e;
}
.history:hover .delete-icon {
  display: flex;
}
.history:hover small {
  display: none;
}
.history.active {
  background: #edf8f4;
  color: #168463;
  font-weight: 700;
}
.empty-history {
  padding: 15px 8px;
  color: #a0aec0;
  font-size: 11px;
  text-align: center;
}
.side-note {
  display: flex;
  gap: 8px;
  margin: 15px 8px 4px;
  padding-top: 14px;
  border-top: 1px solid #edf0f4;
  color: #8b96a7;
  font-size: 10px;
  line-height: 1.5;
  flex-shrink: 0;
}
.side-note svg {
  width: 16px;
  flex: none;
}
.chat-main {
  width: min(790px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  flex: 1;
}
.show-side {
  align-self: flex-start;
  border: 0;
  background: none;
  color: #168463;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
}
.chat-welcome {
  text-align: center;
  padding: 12px 0 16px;
  flex-shrink: 0;
}
.chat-welcome h1 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  letter-spacing: -1px;
}
.chat-welcome p:last-child {
  margin: 4px 0 0;
  color: #778499;
  font-size: 13px;
}
.messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}
.message {
  display: flex;
  gap: 10px;
  max-width: 94%;
}
.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.avatar {
  display: grid;
  place-items: center;
  flex: 0 0 30px;
  height: 30px;
  border-radius: 9px;
  background: #dff3eb;
  color: #168463;
  font: 500 11px 'DM Mono';
}
.user .avatar {
  border-radius: 50%;
  background: #e8edf5;
  color: #536175;
}
.bubble {
  padding: 13px 15px;
  border: 1px solid #e4e9ef;
  border-radius: 4px 13px 13px 13px;
  background: #fff;
  color: #354156;
  font-size: 13px;
  line-height: 1.65;
}
.user .bubble {
  border: 0;
  border-radius: 13px 4px 13px 13px;
  background: #273b4b;
  color: #fff;
}
.message-text :deep(p) {
  margin: 0 0 8px 0;
}
.message-text :deep(p:last-child) {
  margin-bottom: 0;
}
.message-text :deep(ol),
.message-text :deep(ul) {
  margin: 8px 0;
  padding-left: 20px;
}
.message-text :deep(li) {
  margin-bottom: 4px;
  line-height: 1.6;
}
.message-text :deep(strong) {
  font-weight: 700;
  color: #172033;
}
.user .message-text :deep(strong) {
  color: #fff;
}
.message-text :deep(code) {
  background: #f0f4f8;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'DM Mono', monospace;
  font-size: 0.9em;
}
.sources {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  margin-top: 13px;
  padding-top: 10px;
  border-top: 1px solid #eef1f4;
}
.sources > span {
  font: 9px 'DM Mono';
  letter-spacing: 0.5px;
  color: #8793a4;
}
.sources .el-tag {
  font-size: 10px;
  color: #168463;
}
.sources svg {
  width: 11px;
  margin-right: 3px;
}
.fallback-head {
  display: flex;
  gap: 11px;
  margin-bottom: 14px;
}
.fallback-head svg {
  width: 25px;
  color: #d08a22;
}
.fallback-head b {
  color: #273247;
}
.fallback-head p {
  margin: 3px 0;
  color: #788498;
  font-size: 12px;
  line-height: 1.45;
}
.ticket-form {
  padding-top: 8px;
}
.ticket-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.ticket-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.ticket-form :deep(.el-form-item__label) {
  font-size: 11px;
  font-weight: 700;
  color: #566278;
}
.typing {
  display: flex;
  gap: 5px;
  padding: 16px;
}
.typing i {
  width: 6px;
  height: 6px;
  background: #91a0ae;
  border-radius: 50%;
  animation: pulse 1s infinite alternate;
}
.typing i:nth-child(2) {
  animation-delay: 0.2s;
}
.typing i:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes pulse {
  to {
    opacity: 0.25;
    transform: translateY(-2px);
  }
}
.input-area {
  flex-shrink: 0;
  margin-top: 10px;
  padding-top: 10px;
  background: #f6f7fb;
}
.composer {
  position: relative;
  padding: 8px 55px 7px 6px;
  border: 1px solid #dfe5eb;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 5px 16px rgba(32, 46, 66, 0.06);
}
.composer :deep(textarea) {
  font-size: 13px;
}
.composer :deep(.el-textarea__inner) {
  box-shadow: none !important;
}
.send {
  position: absolute;
  right: 10px;
  bottom: 13px;
  background: #168463;
  color: #fff;
  border: 0;
}
.send.is-disabled {
  background: #e2e7ec;
  color: #9da8b5;
}
.input-area p {
  text-align: center;
  color: #98a2b3;
  font-size: 10px;
}
@media (max-width: 850px) {
  .chat-side {
    display: none;
  }
  .chat-main {
    max-width: 760px;
  }
  .ticket-fields {
    grid-template-columns: 1fr;
  }
}
</style>
